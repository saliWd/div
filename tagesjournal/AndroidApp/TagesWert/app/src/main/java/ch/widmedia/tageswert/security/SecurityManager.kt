@file:Suppress("DEPRECATION")

package ch.widmedia.tageswert.security

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityManager {

    private const val PREFS_NAME         = "tageswert_secure_prefs"
    private const val PREF_DB_PASSPHRASE = "db_passphrase"
    private const val PREF_EXPORT_PASS   = "export_password"
    private const val PREF_LAST_EXPORT   = "last_export_time"
    private const val PREF_FIRST_START   = "first_start_time"
    private const val PREF_INTRO_SHOWN   = "intro_shown"

    private const val AES_GCM           = "AES/GCM/NoPadding"
    private const val GCM_IV_LEN        = 12
    private const val GCM_TAG_BITS      = 128
    private const val PBKDF2_ALGO       = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val KEY_LEN_BITS      = 256

    // Fixed salt embedded in the app
    private val EXPORT_SALT = byteArrayOf(
        0x47, 0x54, 0x61, 0x67, 0x53, 0x61, 0x6C, 0x74,
        0x32, 0x30, 0x32, 0x35, 0x57, 0x49, 0x44, 0x4D,
    )

    // ── DB passphrase ─────────────────────────────────────────────────────────

    fun getOrCreateDbPassphrase(context: Context): CharArray {
        val prefs = getSecurePrefs(context)
        val existing = prefs.getString(PREF_DB_PASSPHRASE, null)
        return existing?.toCharArray() ?: generatePassphrase().also {
            prefs.edit { putString(PREF_DB_PASSPHRASE, String(it)) }
        }
    }

    // ── Export password ───────────────────────────────────────────────────────

    fun saveExportPassword(context: Context, password: String) {
        getSecurePrefs(context).edit { putString(PREF_EXPORT_PASS, password) }
    }

    fun getExportPassword(context: Context): String? =
        getSecurePrefs(context).getString(PREF_EXPORT_PASS, null)

    fun saveLastExportTime(context: Context, time: Long) {
        getSecurePrefs(context).edit { putLong(PREF_LAST_EXPORT, time) }
    }

    fun getLastExportTime(context: Context): Long =
        getSecurePrefs(context).getLong(PREF_LAST_EXPORT, 0L)

    fun getOrCreateFirstStartTime(context: Context): Long {
        val prefs = getSecurePrefs(context)
        val existing = prefs.getLong(PREF_FIRST_START, 0L)
        return if (existing == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit { putLong(PREF_FIRST_START, now) }
            now
        } else {
            existing
        }
    }

    fun isIntroShown(context: Context): Boolean =
        getSecurePrefs(context).getBoolean(PREF_INTRO_SHOWN, false)

    fun setIntroShown(context: Context, shown: Boolean) {
        getSecurePrefs(context).edit { putBoolean(PREF_INTRO_SHOWN, shown) }
    }

    // ── Export encryption (AES-256-GCM via PBKDF2) ───────────────────────────

    fun encryptExportData(data: ByteArray, password: String): ByteArray {
        val key  = deriveKey(password)
        val iv   = generateIv()
        val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
        val cipher = Cipher.getInstance(AES_GCM).also {
            it.init(Cipher.ENCRYPT_MODE, key, spec)
        }
        val cipherText = cipher.doFinal(data)
        return iv + cipherText
    }

    fun decryptExportData(encryptedData: ByteArray, password: String): ByteArray {
        require(encryptedData.size > GCM_IV_LEN) { "Ungültige Exportdatei" }
        val iv         = encryptedData.copyOfRange(0, GCM_IV_LEN)
        val cipherText = encryptedData.copyOfRange(GCM_IV_LEN, encryptedData.size)
        val key        = deriveKey(password)
        val spec       = GCMParameterSpec(GCM_TAG_BITS, iv)
        val cipher     = Cipher.getInstance(AES_GCM).also {
            it.init(Cipher.DECRYPT_MODE, key, spec)
        }
        return cipher.doFinal(cipherText)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun deriveKey(password: String): SecretKeySpec {
        val spec = PBEKeySpec(
            password.toCharArray(),
            EXPORT_SALT,
            PBKDF2_ITERATIONS,
            KEY_LEN_BITS,
        )
        val raw = SecretKeyFactory.getInstance(PBKDF2_ALGO)
            .generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }

    private fun generateIv(): ByteArray =
        ByteArray(GCM_IV_LEN).also { java.security.SecureRandom().nextBytes(it) }

    private fun generatePassphrase(): CharArray {
        val kg = KeyGenerator.getInstance("AES").also { it.init(256) }
        return Base64.encodeToString(kg.generateKey().encoded, Base64.NO_WRAP).toCharArray()
    }

    private fun getSecurePrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
