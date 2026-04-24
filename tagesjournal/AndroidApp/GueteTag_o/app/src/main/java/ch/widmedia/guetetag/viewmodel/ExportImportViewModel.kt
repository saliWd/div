package ch.widmedia.guetetag.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.widmedia.guetetag.data.AppDatabase
import ch.widmedia.guetetag.security.ExportImportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ExportImportStatus {
    object Idle    : ExportImportStatus()
    object Loading : ExportImportStatus()
    data class Success(val message: String) : ExportImportStatus()
    data class Error(val message: String)   : ExportImportStatus()
}

class ExportImportViewModel(private val db: AppDatabase) : ViewModel() {

    private val _status = MutableStateFlow<ExportImportStatus>(ExportImportStatus.Idle)
    val status: StateFlow<ExportImportStatus> = _status.asStateFlow()

    fun exportDatabase(context: Context, uri: Uri, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _status.value = ExportImportStatus.Loading
            runCatching {
                val entries = db.journalDao().getAllEntries().let { flow ->
                    // Collect a one-shot snapshot
                    var snapshot: List<ch.widmedia.guetetag.data.JournalEntry> = emptyList()
                    val job = launch { flow.collect { snapshot = it } }
                    kotlinx.coroutines.delay(200)
                    job.cancel()
                    snapshot
                }
                ExportImportHelper.exportToUri(context, uri, entries, password)
                ExportImportHelper.storeExportPassword(context, password)
            }.onSuccess {
                _status.value = ExportImportStatus.Success("Export erfolgreich.")
            }.onFailure { e ->
                _status.value = ExportImportStatus.Error(
                    "Export fehlgeschlagen: ${e.localizedMessage ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    fun importDatabase(context: Context, uri: Uri, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _status.value = ExportImportStatus.Loading
            runCatching {
                val entries = ExportImportHelper.importFromUri(context, uri, password)
                entries.forEach { db.journalDao().upsertEntry(it) }
                ExportImportHelper.storeExportPassword(context, password)
                entries.size
            }.onSuccess { count ->
                _status.value = ExportImportStatus.Success("$count Eintrag/Einträge importiert.")
            }.onFailure { e ->
                _status.value = ExportImportStatus.Error(
                    "Import fehlgeschlagen. Passwort korrekt?"
                )
            }
        }
    }

    fun resetStatus() { _status.value = ExportImportStatus.Idle }

    companion object {
        fun factory(db: AppDatabase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ExportImportViewModel(db) as T
            }
    }
}
