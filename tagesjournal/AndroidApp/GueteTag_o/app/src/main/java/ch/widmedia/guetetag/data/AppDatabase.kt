package ch.widmedia.guetetag.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [JournalEntry::class],
    version  = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context, passphrase).also { instance = it }
            }

        private fun build(context: Context, passphrase: ByteArray): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "guetetag.db"
            )
                .openHelperFactory(SupportFactory(passphrase))
                .build()

        /** Call when the user triggers a full reset (invalidated key, etc.). */
        fun closeAndReset() {
            instance?.close()
            instance = null
        }
    }
}
