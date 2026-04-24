package ch.widmedia.guetetag.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.widmedia.guetetag.data.AppDatabase
import ch.widmedia.guetetag.data.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EntryViewModel(private val db: AppDatabase) : ViewModel() {

    private val _entry = MutableStateFlow<JournalEntry?>(null)
    val entry: StateFlow<JournalEntry?> = _entry.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun loadEntry(date: String) {
        viewModelScope.launch {
            _entry.value = db.journalDao().getEntryByDate(date)
        }
    }

    fun saveEntry(date: String, rating: Int, text: String) {
        viewModelScope.launch {
            db.journalDao().upsertEntry(JournalEntry(date = date, rating = rating, text = text))
            _saved.value = true
        }
    }

    fun deleteEntry(date: String) {
        viewModelScope.launch {
            db.journalDao().getEntryByDate(date)?.let { db.journalDao().deleteEntry(it) }
            _deleted.value = true
        }
    }

    fun resetFlags() {
        _saved.value   = false
        _deleted.value = false
    }

    companion object {
        fun factory(db: AppDatabase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    EntryViewModel(db) as T
            }
    }
}
