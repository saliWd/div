
package ch.widmedia.guetetag.security
import android.content.Context
import androidx.security.crypto.*
import java.security.SecureRandom
object DatabaseKeyManager {
    fun getOrCreate(context: Context): ByteArray {
        val mk = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(
            context, "db", mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs.getString("k", null)?.let { return it.encodeToByteArray() }
        val k = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString("k", k.decodeToString()).apply()
        return k
    }
}
