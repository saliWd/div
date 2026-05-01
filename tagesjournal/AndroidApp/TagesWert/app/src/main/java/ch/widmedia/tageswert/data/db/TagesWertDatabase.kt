package ch.widmedia.tageswert.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ch.widmedia.tageswert.data.model.TagEintrag
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [TagEintrag::class],
    version = 1,
    exportSchema = false,
)
abstract class TagesWertDatabase : RoomDatabase() {

    abstract fun tagEintragDao(): TagEintragDao

    companion object {
        private const val DB_NAME = "tageswert_encrypted.db"

        @Volatile
        private var INSTANCE: TagesWertDatabase? = null

        fun getInstance(context: Context, passphrase: CharArray): TagesWertDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        private fun buildDatabase(context: Context, passphrase: CharArray): TagesWertDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(String(passphrase).toByteArray())

            return Room.databaseBuilder(
                context.applicationContext,
                TagesWertDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
