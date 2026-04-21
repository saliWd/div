package ch.widmedia.eintrag_g.data.repository

import ch.widmedia.eintrag_g.data.dao.EntryDao
import ch.widmedia.eintrag_g.data.entity.JournalEntry
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val entryDao: EntryDao) {
    val allEntries: Flow<List<JournalEntry>> = entryDao.getAllEntries()

    suspend fun insert(entry: JournalEntry) {
        entryDao.insertEntry(entry)
    }

    suspend fun delete(entry: JournalEntry) {
        entryDao.deleteEntry(entry)
    }

    suspend fun getEntryById(id: Long): JournalEntry? {
        return entryDao.getEntryById(id)
    }

    suspend fun getAllEntriesList(): List<JournalEntry> {
        return entryDao.getAllEntriesList()
    }
}
