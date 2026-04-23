package ch.widmedia.guetetag.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import java.util.Base64

/**
 * Verwaltet verschlüsselte Einstellungen und Sicherheitsschlüssel.
 */
class SecurityManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val DB_PASSPHRASE_KEY = "db_passphrase"
        private const val EXPORT_PASSWORD_KEY = "export_password"
    }

    /**
     * Holt oder generiert eine Passphrase für die Datenbankverschlüsselung.
     */
    fun getDatabasePassphrase(): String {
        var passphrase = sharedPreferences.getString(DB_PASSPHRASE_KEY, null)
        if (passphrase == null) {
            passphrase = generateRandomString(32)
            sharedPreferences.edit().putString(DB_PASSPHRASE_KEY, passphrase).apply()
        }
        return passphrase
    }

    /**
     * Speichert das Passwort für den Export.
     */
    fun saveExportPassword(password: String) {
        sharedPreferences.edit().putString(EXPORT_PASSWORD_KEY, password).apply()
    }

    /**
     * Holt das Passwort für den Export.
     */
    fun getExportPassword(): String? {
        return sharedPreferences.getString(EXPORT_PASSWORD_KEY, null)
    }

    private fun generateRandomString(length: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}
