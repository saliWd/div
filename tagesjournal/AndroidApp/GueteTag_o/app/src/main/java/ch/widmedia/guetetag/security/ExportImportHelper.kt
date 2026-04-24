package ch.widmedia.guetetag.security

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import ch.widmedia.guetetag.data.JournalEntry
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles encrypted export/import of journal entries and secure storage
 * of the export password using a non-biometric AndroidKeyStore key.
 *
 * Export file format (binary):
 *   [1 byte  version]
 *   [16 bytes PBKDF2 salt]
 *   [12 bytes AES-GCM IV]
 *   [N bytes  AES-256-GCM ciphertext of UTF-8 JSON]
 */
object ExportImportHelper {

    private const val VERSION: Byte       = 1
    private const val SALT_SIZE           = 16
    private const val IV_SIZE             = 12
    private const val PBKDF2_ITERATIONS   = 310_000
    private const val KEY_SIZE_BITS       = 256
    private const val GCM_TAG_LEN         = 128
    private const val CIPHER_ALGO         = "AES/GCM/NoPadding"

    // ── Export-password secure storage ──────────────────────────────────────

    private const val EXPORT_KEY_ALIAS    = "guetetag_export_key"
    private const val EXPORT_PREFS        = "guetetag_export_prefs"
    private const val PREF_ENC_PASSWORD   = "enc_export_password"
    private const val PREF_IV             = "export_password_iv"

    fun storeExportPassword(context: Context, password: String) {
        val key    = getOrCreateExportKey()
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        val iv        = cipher.parameters.getParameterSpec(GCMParameterSpec::class.java).iv

        context.getSharedPreferences(EXPORT_PREFS, Context.MODE_PRIVATE).edit()
            .putString(PREF_ENC_PASSWORD, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(PREF_IV,           Base64.encodeToString(iv,        Base64.NO_WRAP))
            .apply()
    }

    fun retrieveExportPassword(context: Context): String {
        val prefs    = context.getSharedPreferences(EXPORT_PREFS, Context.MODE_PRIVATE)
        val encB64   = prefs.getString(PREF_ENC_PASSWORD, null) ?: return ""
        val ivB64    = prefs.getString(PREF_IV,           null) ?: return ""
        val key      = getOrCreateExportKey()
        val iv       = Base64.decode(ivB64,  Base64.NO_WRAP)
        val cipher   = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LEN, iv))
        return String(cipher.doFinal(Base64.decode(encB64, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun getOrCreateExportKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(EXPORT_KEY_ALIAS)) {
            val spec = KeyGenParameterSpec.Builder(
                EXPORT_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                .apply { init(spec) }
                .generateKey()
        }
        return ks.getKey(EXPORT_KEY_ALIAS, null) as SecretKey
    }

    // ── Export ───────────────────────────────────────────────────────────────

    fun exportToUri(
        context: Context,
        uri: Uri,
        entries: List<JournalEntry>,
        password: String
    ) {
        val salt       = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv         = ByteArray(IV_SIZE).also   { SecureRandom().nextBytes(it) }
        val secretKey  = deriveKey(password, salt)
        val cipher     = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LEN, iv))
        val plaintext  = toJson(entries).toByteArray(Charsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintext)

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(byteArrayOf(VERSION))
            out.write(salt)
            out.write(iv)
            out.write(ciphertext)
        } ?: error("Ausgabedatei konnte nicht geöffnet werden.")
    }

    // ── Import ───────────────────────────────────────────────────────────────

    fun importFromUri(
        context: Context,
        uri: Uri,
        password: String
    ): List<JournalEntry> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Eingabedatei konnte nicht geöffnet werden.")

        var pos = 0
        val version = bytes[pos++]
        require(version == VERSION) { "Unbekanntes Dateiformat (Version $version)." }

        val salt       = bytes.copyOfRange(pos, pos + SALT_SIZE).also { pos += SALT_SIZE }
        val iv         = bytes.copyOfRange(pos, pos + IV_SIZE).also   { pos += IV_SIZE }
        val ciphertext = bytes.copyOfRange(pos, bytes.size)

        val secretKey = deriveKey(password, salt)
        val cipher    = Cipher.getInstance(CIPHER_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LEN, iv))
        val json = String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        return fromJson(json)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec    = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val bytes   = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    private fun toJson(entries: List<JournalEntry>): String {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("date",   e.date)
                    put("rating", e.rating)
                    put("text",   e.text)
                }
            )
        }
        return arr.toString()
    }

    private fun fromJson(json: String): List<JournalEntry> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            arr.getJSONObject(i).let { obj ->
                JournalEntry(
                    date   = obj.getString("date"),
                    rating = obj.getInt("rating"),
                    text   = obj.getString("text")
                )
            }
        }
    }
}
