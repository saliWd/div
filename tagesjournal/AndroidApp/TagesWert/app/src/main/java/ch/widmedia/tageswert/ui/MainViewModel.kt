package ch.widmedia.tageswert.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.widmedia.tageswert.R
import ch.widmedia.tageswert.data.model.TagEintrag
import ch.widmedia.tageswert.data.repository.EintragRepository
import ch.widmedia.tageswert.utils.DateUtil
import ch.widmedia.tageswert.utils.ExportImportUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiState(
    val isLoading: Boolean = false,
    val errorResId: Int? = null,
    val successResId: Int? = null,
    val tageWithEintrag: Map<String, Int> = emptyMap(),
    val monatBewertungen: Map<String, Int> = emptyMap(),
)

data class ImportSummary(
    val existingCount: Int,
    val newCount: Int,
    val startDate: String?,
    val endDate: String?,
    val neueEintraege: List<TagEintrag>
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
            val von = heute.minusDays(20)
            val bewertungen = repository.bewertungenFuerZeitraum(
                DateUtil.toIso(von),
                DateUtil.toIso(heute)
            )
            val map = bewertungen.associate { it.datum to it.bewertung }
            _uiState.value = _uiState.value.copy(tageWithEintrag = map)
            
            // Also load current month by default
            ladeMonatBewertungen(heute)
        }
    }

    fun ladeMonatBewertungen(datum: LocalDate) {
        viewModelScope.launch {
            val von = datum.withDayOfMonth(1)
            val bis = datum.withDayOfMonth(datum.lengthOfMonth())
            val bewertungen = repository.bewertungenFuerZeitraum(
                DateUtil.toIso(von),
                DateUtil.toIso(bis)
            )
            val map = bewertungen.associate { it.datum to it.bewertung }
            _uiState.value = _uiState.value.copy(monatBewertungen = map)
        }
    }

    suspend fun eintragFuerDatum(datum: String): TagEintrag? =
        repository.eintraegFuerDatum(datum)

    fun speichern(eintrag: TagEintrag, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.speichern(eintrag)
            ladeTageWithEintrag()
            _uiState.value = _uiState.value.copy(successResId = R.string.entry_saved)
            onDone()
        }
    }

    fun loeschen(eintrag: TagEintrag, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.loeschen(eintrag)
            ladeTageWithEintrag()
            _uiState.value = _uiState.value.copy(successResId = R.string.entry_deleted)
            onDone()
        }
    }

    fun getEncryptedExportData(context: Context, password: String, onResult: (ByteArray?) -> Unit) {
        viewModelScope.launch {
            try {
                val eintraege = repository.alleEintraegeListe()
                val data = ExportImportUtil.getEncryptedExportData(context, eintraege, password)
                onResult(data)
            } catch (_: Exception) {
                onResult(null)
            }
        }
    }

    fun prepareImport(
        context: Context,
        uri: Uri,
        password: String,
        onSuccess: (ImportSummary) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val neueEintraege = ExportImportUtil.importieren(context, uri, password)
                val existingEintraege = repository.alleEintraegeListe()

                val sorted = neueEintraege.sortedBy { it.datum }
                val startDate = sorted.firstOrNull()?.datum
                val endDate = sorted.lastOrNull()?.datum

                onSuccess(
                    ImportSummary(
                        existingCount = existingEintraege.size,
                        newCount = neueEintraege.size,
                        startDate = startDate,
                        endDate = endDate,
                        neueEintraege = neueEintraege
                    )
                )
            } catch (e: Exception) {
                onError(e.message ?: context.getString(R.string.error_unknown))
            }
        }
    }

    fun executeImport(eintraege: List<TagEintrag>, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.alleLoeschen()
            eintraege.forEach { repository.speichern(it.copy(id = 0)) }
            ladeTageWithEintrag()
            onSuccess()
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorResId = null, successResId = null)
    }

    class Factory(private val repository: EintragRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
