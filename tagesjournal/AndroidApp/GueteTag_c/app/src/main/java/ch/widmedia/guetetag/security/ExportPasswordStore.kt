
package ch.widmedia.guetetag.security
import android.content.Context
import androidx.security.crypto.*
object ExportPasswordStore {
    fun save(context: Context, password: String) {
        val mk = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "export", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).edit().putString("pwd", password).apply()
    }
}
