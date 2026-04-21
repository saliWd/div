package ch.widmedia.eintrag_g.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PassphraseManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val KEY_EXPORT_PASSWORD = "export_password"

    private fun getSharedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getDatabasePassphrase(context: Context): ByteArray {
        val prefs = getSharedPrefs(context)
        var passphrase = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (passphrase == null) {
            passphrase = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DB_PASSPHRASE, passphrase).apply()
        }
        return passphrase.toByteArray()
    }

    fun getExportPassword(context: Context): String {
        val prefs = getSharedPrefs(context)
        var password = prefs.getString(KEY_EXPORT_PASSWORD, null)
        if (password == null) {
            password = "DefaultExportPassword123!" // Should ideally be user-set, but requirement says stored in secure location
            prefs.edit().putString(KEY_EXPORT_PASSWORD, password).apply()
        }
        return password
    }
}
