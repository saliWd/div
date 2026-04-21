package ch.widmedia.Eintrag

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.app.Activity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val DB_KEY_ALIAS = "eintrag_db_key"
    private const val PREFS_NAME = "eintrag_secure_prefs"
    private const val PREF_ENCRYPTED_DB_PASS = "enc_db_pass"
    private const val PREF_EXPORT_PASS_IV = "export_pass_iv"
    private const val PREF_ENCRYPTED_EXPORT_PASS = "enc_export_pass"
    private const val GCM_TAG_LEN = 128
    private const val PBKDF2_ITERATIONS = 310_000
    private const val AES_KEY_LEN = 256
    private const val SALT_LEN = 32

    // ── Keystore key for biometric-protected DB password ──────────────────────

    private fun getOrCreateBiometricKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
        ks.getKey(DB_KEY_ALIAS, null)?.let { return it as SecretKey }
        val spec = KeyGenParameterSpec.Builder(
            DB_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            .also { it.init(spec) }.generateKey()
    }

    fun encryptDbPasswordCipher(): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateBiometricKey())
        return cipher
    }

    fun decryptDbPasswordCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateBiometricKey(), GCMParameterSpec(GCM_TAG_LEN, iv))
        return cipher
    }

    fun saveEncryptedDbPassword(context: Context, cipher: Cipher, password: ByteArray) {
        val encrypted = cipher.doFinal(password)
        val iv = cipher.iv
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_ENCRYPTED_DB_PASS, iv.toHex() + ":" + encrypted.toHex())
            .apply()
    }

    fun loadEncryptedDbPassword(context: Context): Pair<ByteArray, ByteArray>? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_ENCRYPTED_DB_PASS, null) ?: return null
        val parts = raw.split(":")
        if (parts.size != 2) return null
        return parts[0].fromHex() to parts[1].fromHex()
    }

    fun hasDbPassword(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(PREF_ENCRYPTED_DB_PASS)

    // ── Export password stored encrypted with same biometric key ──────────────

    fun saveExportPassword(context: Context, cipher: Cipher, password: ByteArray) {
        val encrypted = cipher.doFinal(password)
        val iv = cipher.iv
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_ENCRYPTED_EXPORT_PASS, encrypted.toHex())
            .putString(PREF_EXPORT_PASS_IV, iv.toHex())
            .apply()
    }

    fun loadEncryptedExportPassword(context: Context): Pair<ByteArray, ByteArray>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enc = prefs.getString(PREF_ENCRYPTED_EXPORT_PASS, null) ?: return null
        val iv = prefs.getString(PREF_EXPORT_PASS_IV, null) ?: return null
        return iv.fromHex() to enc.fromHex()
    }

    fun hasExportPassword(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(PREF_ENCRYPTED_EXPORT_PASS)

    // ── File encryption / decryption (AES-256-GCM + PBKDF2) ──────────────────

    fun encryptFile(sourceFile: File, destFile: File, password: CharArray) {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        FileOutputStream(destFile).use { out ->
            out.write(salt)
            out.write(iv)
            FileInputStream(sourceFile).use { input ->
                val plaintext = input.readBytes()
                val ciphertext = cipher.doFinal(plaintext)
                out.write(ciphertext)
            }
        }
    }

    fun decryptFile(sourceFile: File, destFile: File, password: CharArray) {
        FileInputStream(sourceFile).use { input ->
            val salt = input.readNBytes(SALT_LEN)
            val iv = input.readNBytes(12)
            val ciphertext = input.readBytes()
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LEN, iv))
            val plaintext = cipher.doFinal(ciphertext)
            FileOutputStream(destFile).use { it.write(plaintext) }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_LEN)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // ── Biometric prompt helper ────────────────────────────────────────────────

    fun showBiometricPrompt(
        activity: Activity,
        title: String,
        cipher: Cipher,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit
    ) {
        val cancellationSignal = CancellationSignal()
        val executor = activity.mainExecutor

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle("Use fingerprint to continue")
            .setNegativeButton("Cancel", executor) { _, _ -> onError("Cancelled") }
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(
            BiometricPrompt.CryptoObject(cipher),
            cancellationSignal,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    result.cryptoObject?.cipher?.let(onSuccess)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
                override fun onAuthenticationFailed() {
                    onError("Fingerprint not recognized")
                }
            }
        )
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.fromHex(): ByteArray {
        check(length % 2 == 0)
        return ByteArray(length / 2) { i ->
            ((this[2 * i].digitToInt(16) shl 4) + this[2 * i + 1].digitToInt(16)).toByte()
        }
    }
}
