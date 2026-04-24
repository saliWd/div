package ch.widmedia.guetetag.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.widmedia.guetetag.data.model.TagEintrag
import ch.widmedia.guetetag.data.repository.EintragRepository
import ch.widmedia.guetetag.utils.DateUtil
import ch.widmedia.guetetag.utils.ExportImportUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val tageWithEintrag: Set<String> = emptySet()
)

class MainViewModel(private val repository: EintragRepository) : ViewModel() {

    val alleEintraege: StateFlow<List<TagEintrag>> = repository.alleEintraege()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        ladeTageWithEintrag()
    }

    private fun ladeTageWithEintrag() {
        viewModelScope.launch {
            val heute = LocalDate.now()
            val vor14 = heute.minusDays(13)
            val tage = repository.datumMitEintrag(
                DateUtil.toIso(vor14),
                DateUtil.toIso(heute)
            )
            _uiState.value = _uiState.value.copy(tageWithEintrag = tage.toSet())
        }
    }

    fun refreshKalender() = ladeTageWithEintrag()

    suspend fun eintragFuerDatum(datum: String): TagEintrag? =
        repository.eintraegFuerDatum(datum)

    fun speichern(eintrag: TagEintrag, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.speichern(eintrag)
            ladeTageWithEintrag()
            _uiState.value = _uiState.value.copy(successMessage = "Eintrag gespeichert")
            onDone()
        }
    }

    fun loeschen(eintrag: TagEintrag, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.loeschen(eintrag)
            ladeTageWithEintrag()
            _uiState.value = _uiState.value.copy(successMessage = "Eintrag gelöscht")
            onDone()
        }
    }

    fun exportieren(context: Context, password: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val eintraege = repository.alleEintraegeListe()
                val file = ExportImportUtil.exportieren(context, eintraege, password)
                onSuccess(file.absolutePath)
            } catch (e: Exception) {
                onError(e.message ?: "Unbekannter Fehler")
            }
        }
    }

    fun importieren(context: Context, uri: Uri, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val eintraege = ExportImportUtil.importieren(context, uri, password)
                repository.alleLoeschen()
                eintraege.forEach { repository.speichern(it.copy(id = 0)) }
                ladeTageWithEintrag()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Unbekannter Fehler")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    class Factory(private val repository: EintragRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
