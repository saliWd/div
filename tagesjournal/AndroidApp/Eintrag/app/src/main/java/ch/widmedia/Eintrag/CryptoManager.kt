package ch.widmedia.Eintrag

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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

    private const val KEYSTORE_PROVIDER    = "AndroidKeyStore"
    private const val DB_KEY_ALIAS         = "eintrag_db_key"
    private const val PREFS_NAME           = "eintrag_secure_prefs"
    private const val PREF_ENCRYPTED_DB_PASS    = "enc_db_pass"
    private const val PREF_EXPORT_PASS_IV       = "export_pass_iv"
    private const val PREF_ENCRYPTED_EXPORT_PASS = "enc_export_pass"
    private const val GCM_TAG_LEN   = 128
    private const val PBKDF2_ITER   = 310_000
    private const val AES_KEY_LEN   = 256
    private const val SALT_LEN      = 32

    // ── Keystore-backed AES key (requires biometric auth) ────────────────────

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

    fun encryptDbPasswordCipher(): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").also {
            it.init(Cipher.ENCRYPT_MODE, getOrCreateBiometricKey())
        }

    fun decryptDbPasswordCipher(iv: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").also {
            it.init(Cipher.DECRYPT_MODE, getOrCreateBiometricKey(), GCMParameterSpec(GCM_TAG_LEN, iv))
        }

    // ── DB password storage ───────────────────────────────────────────────────

    fun saveEncryptedDbPassword(context: Context, cipher: Cipher, password: ByteArray) {
        val encrypted = cipher.doFinal(password)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_ENCRYPTED_DB_PASS, cipher.iv.toHex() + ":" + encrypted.toHex())
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

    // ── Export password storage ───────────────────────────────────────────────

    fun saveExportPassword(context: Context, cipher: Cipher, password: ByteArray) {
        val encrypted = cipher.doFinal(password)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREF_ENCRYPTED_EXPORT_PASS, encrypted.toHex())
            .putString(PREF_EXPORT_PASS_IV, cipher.iv.toHex())
            .apply()
    }

    fun loadEncryptedExportPassword(context: Context): Pair<ByteArray, ByteArray>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enc = prefs.getString(PREF_ENCRYPTED_EXPORT_PASS, null) ?: return null
        val iv  = prefs.getString(PREF_EXPORT_PASS_IV, null) ?: return null
        return iv.fromHex() to enc.fromHex()
    }

    fun hasExportPassword(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(PREF_ENCRYPTED_EXPORT_PASS)

    // ── File encryption (AES-256-GCM + PBKDF2-SHA256) ────────────────────────

    fun encryptFile(sourceFile: File, destFile: File, password: CharArray) {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key  = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        FileOutputStream(destFile).use { out ->
            out.write(salt)
            out.write(cipher.iv)
            out.write(cipher.doFinal(FileInputStream(sourceFile).use { it.readBytes() }))
        }
    }

    fun decryptFile(sourceFile: File, destFile: File, password: CharArray) {
        FileInputStream(sourceFile).use { input ->
            val salt       = input.readNBytes(SALT_LEN)
            val iv         = input.readNBytes(12)
            val ciphertext = input.readBytes()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(GCM_TAG_LEN, iv))
            FileOutputStream(destFile).use { it.write(cipher.doFinal(ciphertext)) }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec    = PBEKeySpec(password, salt, PBKDF2_ITER, AES_KEY_LEN)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    // ── Biometric prompt ──────────────────────────────────────────────────────

    fun showBiometricPrompt(
        activity: Activity,
        title: String,
        subtitle: String,
        negativeText: String,
        cipher: Cipher,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit
    ) {
        val signal   = CancellationSignal()
        val executor = activity.mainExecutor

        BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(negativeText, executor) { _, _ -> onError(negativeText) }
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
            .authenticate(
                BiometricPrompt.CryptoObject(cipher),
                signal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        result.cryptoObject?.cipher?.let(onSuccess)
                    }
                    override fun onAuthenticationError(code: Int, msg: CharSequence) = onError(msg.toString())
                    override fun onAuthenticationFailed() = onError("")
                }
            )
    }

    // ── Hex helpers ───────────────────────────────────────────────────────────

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.fromHex(): ByteArray {
        check(length % 2 == 0)
        return ByteArray(length / 2) { i ->
            ((this[2 * i].digitToInt(16) shl 4) + this[2 * i + 1].digitToInt(16)).toByte()
        }
    }
}
