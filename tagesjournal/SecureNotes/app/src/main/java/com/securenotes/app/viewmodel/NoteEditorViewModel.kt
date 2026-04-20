package com.securenotes.app.viewmodel

import androidx.lifecycle.*
import com.securenotes.app.data.Note
import com.securenotes.app.data.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _note = MutableLiveData<Note?>()
    val note: LiveData<Note?> = _note

    private val _saved = MutableLiveData(false)
    val saved: LiveData<Boolean> = _saved

    fun loadNote(id: Long) {
        if (id == 0L) { _note.value = null; return }
        viewModelScope.launch {
            _note.value = repository.getNoteById(id)
        }
    }

    fun saveNote(title: String, content: String) {
        viewModelScope.launch {
            val existing = _note.value
            val note = if (existing == null) {
                Note(title = title, content = content)
            } else {
                existing.copy(title = title, content = content, updatedAt = System.currentTimeMillis())
            }
            repository.saveNote(note)
            _saved.value = true
        }
    }
}
