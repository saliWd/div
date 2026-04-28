package ch.widmedia.guetetag.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ch.widmedia.guetetag.data.model.TagEintrag
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [TagEintrag::class],
    version = 1,
    exportSchema = false,
)
abstract class GueteTagDatabase : RoomDatabase() {

    abstract fun tagEintragDao(): TagEintragDao

    companion object {
        private const val DB_NAME = "guetetag_encrypted.db"

        @Volatile
        private var INSTANCE: GueteTagDatabase? = null

        fun getInstance(context: Context, passphrase: CharArray): GueteTagDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also { INSTANCE = it }
            }
        }

        fun closeAndReset() {
            INSTANCE?.close()
            INSTANCE = null
        }

        private fun buildDatabase(context: Context, passphrase: CharArray): GueteTagDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(String(passphrase).toByteArray())

            return Room.databaseBuilder(
                context.applicationContext,
                GueteTagDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        fun getDatabasePath(context: Context): String {
            return context.getDatabasePath(DB_NAME).absolutePath
        }
    }
}
