package com.securenotes.app.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val dao: NoteDao
) {
    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes(query)

    suspend fun getNoteById(id: Long): Note? = dao.getNoteById(id)

    suspend fun saveNote(note: Note): Long {
        return if (note.id == 0L) {
            dao.insertNote(note)
        } else {
            dao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            note.id
        }
    }

    suspend fun deleteNote(note: Note) = dao.deleteNote(note)
}
