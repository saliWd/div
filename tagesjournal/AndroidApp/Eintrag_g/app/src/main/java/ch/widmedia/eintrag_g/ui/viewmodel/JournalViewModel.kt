package ch.widmedia.eintrag_g.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.widmedia.eintrag_g.data.AppDatabase
import ch.widmedia.eintrag_g.data.entity.JournalEntry
import ch.widmedia.eintrag_g.data.repository.JournalRepository
import ch.widmedia.eintrag_g.security.PassphraseManager
import ch.widmedia.eintrag_g.util.BackupUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: JournalRepository
    val allEntries: Flow<List<JournalEntry>>

    init {
        val passphrase = PassphraseManager.getDatabasePassphrase(application)
        val database = AppDatabase.getDatabase(application, passphrase)
        repository = JournalRepository(database.entryDao())
        allEntries = repository.allEntries
    }

    fun insert(entry: JournalEntry) = viewModelScope.launch {
        repository.insert(entry)
    }

    fun delete(entry: JournalEntry) = viewModelScope.launch {
        repository.delete(entry)
    }

    suspend fun getEntryById(id: Long): JournalEntry? {
        return repository.getEntryById(id)
    }

    fun exportEntries(onSuccess: (String) -> Unit, onError: (Exception) -> Unit) = viewModelScope.launch {
        try {
            val entries = repository.getAllEntriesList()
            val password = PassphraseManager.getExportPassword(getApplication())
            val encrypted = BackupUtils.encryptEntries(entries, password.toCharArray())
            onSuccess(encrypted)
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun importEntries(encryptedData: String, onSuccess: () -> Unit, onError: (String) -> Unit) = viewModelScope.launch {
        try {
            val password = PassphraseManager.getExportPassword(getApplication())
            val entries = BackupUtils.decryptEntries(encryptedData, password.toCharArray())
            if (entries != null) {
                entries.forEach { repository.insert(it) }
                onSuccess()
            } else {
                onError("Entschlüsselung fehlgeschlagen. Falsches Passwort oder beschädigte Datei.")
            }
        } catch (e: Exception) {
            onError(e.message ?: "Unbekannter Fehler")
        }
    }
}
