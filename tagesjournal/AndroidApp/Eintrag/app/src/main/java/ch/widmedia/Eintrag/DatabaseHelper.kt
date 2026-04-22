package ch.widmedia.Eintrag

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import java.io.File

data class Entry(
    val id: Long = 0,
    val date: String,
    val title: String,
    val text: String
)

class DatabaseHelper private constructor(context: Context, password: CharArray) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val db: SQLiteDatabase

    init {
        SQLiteDatabase.loadLibs(context)
        val dbFile = context.getDatabasePath(DB_NAME)
        dbFile.parentFile?.mkdirs()
        db = SQLiteDatabase.openOrCreateDatabase(dbFile, String(password), null)
        onCreate(db)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $TABLE_ENTRIES (
                $COL_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DATE  TEXT NOT NULL,
                $COL_TITLE TEXT NOT NULL,
                $COL_TEXT  TEXT NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun addEntry(entry: Entry): Long {
        val cv = ContentValues().apply {
            put(COL_DATE, entry.date)
            put(COL_TITLE, entry.title)
            put(COL_TEXT, entry.text)
        }
        return db.insert(TABLE_ENTRIES, null, cv)
    }

    fun deleteEntry(id: Long) {
        db.delete(TABLE_ENTRIES, "$COL_ID=?", arrayOf(id.toString()))
    }

    fun getAllEntries(): List<Entry> {
        val list = mutableListOf<Entry>()
        val cursor: Cursor = db.query(TABLE_ENTRIES, null, null, null, null, null, "$COL_DATE DESC, $COL_ID DESC")
        cursor.use {
            while (it.moveToNext()) {
                list += Entry(
                    id    = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                    date  = it.getString(it.getColumnIndexOrThrow(COL_DATE)),
                    title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                    text  = it.getString(it.getColumnIndexOrThrow(COL_TEXT))
                )
            }
        }
        return list
    }

    fun getEntry(id: Long): Entry? {
        val cursor: Cursor = db.query(
            TABLE_ENTRIES, null,
            "$COL_ID=?", arrayOf(id.toString()),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) Entry(
                id    = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                date  = it.getString(it.getColumnIndexOrThrow(COL_DATE)),
                title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                text  = it.getString(it.getColumnIndexOrThrow(COL_TEXT))
            ) else null
        }
    }

    fun getDatabaseFile(context: Context): File = context.getDatabasePath(DB_NAME)

    override fun close() {
        if (db.isOpen) db.close()
        super.close()
    }

    companion object {
        const val DB_NAME        = "eintrag.db"
        const val DB_VERSION     = 1
        const val TABLE_ENTRIES  = "entries"
        const val COL_ID         = "id"
        const val COL_DATE       = "date"
        const val COL_TITLE      = "title"
        const val COL_TEXT       = "text"

        @Volatile private var instance: DatabaseHelper? = null

        fun getInstance(context: Context, password: CharArray): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext, password).also { instance = it }
            }

        fun clearInstance() {
            instance?.close()
            instance = null
        }
    }
}
