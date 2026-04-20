package com.securenotes.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Entity ──────────────────────────────────────────────────────────────────

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title")   val title: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "created_at")  val createdAt: Long  = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")  val updatedAt: Long  = System.currentTimeMillis()
)

// ── DAO ──────────────────────────────────────────────────────────────────────

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}
