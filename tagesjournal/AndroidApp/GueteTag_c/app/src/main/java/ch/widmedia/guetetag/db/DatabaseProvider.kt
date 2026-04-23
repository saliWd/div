
package ch.widmedia.guetetag.db
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportFactory
import ch.widmedia.guetetag.security.DatabaseKeyManager
object DatabaseProvider {
    @Volatile private var INSTANCE: AppDatabase? = null
    fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "guetetag.db")
            .openHelperFactory(SupportFactory(DatabaseKeyManager.getOrCreate(context)))
            .build().also { INSTANCE = it }
    }
}
