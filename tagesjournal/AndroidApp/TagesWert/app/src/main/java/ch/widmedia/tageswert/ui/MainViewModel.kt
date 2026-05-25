package ch.widmedia.tageswert.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ch.widmedia.tageswert.R
import ch.widmedia.tageswert.data.model.TagEintrag
import ch.widmedia.tageswert.data.repository.EintragRepository
import ch.widmedia.tageswert.security.SecurityManager
import ch.widmedia.tageswert.utils.DateUtil
import ch.widmedia.tageswert.utils.ExportImportUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val monatBewertungen: Map<String, Int> = emptyMap(),
    val lastExportTime: Long = 0L,
    val firstStartTime: Long = 0L,
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

    // Inactivity and locking
    private var inactivityJob: Job? = null
    private val _shouldLock = MutableStateFlow(false)
    val shouldLock: StateFlow<Boolean> = _shouldLock.asStateFlow()

    // Current editing state for autosave
    var editingEintrag by mutableStateOf<TagEintrag?>(null)
        private set

    init {
        // Load current month by default
        ladeMonatBewertungen(LocalDate.now())
        resetInactivityTimer()
    }

    fun loadLastExportTime(context: Context) {
        val time = SecurityManager.getLastExportTime(context)
        val firstStart = SecurityManager.getOrCreateFirstStartTime(context)
        _uiState.value = _uiState.value.copy(
            lastExportTime = time,
            firstStartTime = firstStart
        )
    }

    fun updateLastExportTime(context: Context) {
        val now = System.currentTimeMillis()
        SecurityManager.saveLastExportTime(context, now)
        _uiState.value = _uiState.value.copy(lastExportTime = now)
    }

    fun resetInactivityTimer() {
        _shouldLock.value = false
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            delay(10 * 60 * 1000) // 10 minutes
            autosave()
            _shouldLock.value = true
        }
    }

    private suspend fun autosave() {
        editingEintrag?.let {
            repository.speichern(it)
        }
    }

    fun startEditing(datum: String) {
        viewModelScope.launch {
            editingEintrag = repository.eintraegFuerDatum(datum) ?: TagEintrag(datum = datum, bewertung = 5)
        }
    }

    fun updateEditing(bewertung: Int? = null, notizen: String? = null) {
        editingEintrag = editingEintrag?.copy(
            bewertung = bewertung ?: editingEintrag?.bewertung ?: 5,
            notizen = notizen ?: editingEintrag?.notizen ?: ""
        )
    }

    fun stopEditing() {
        editingEintrag = null
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
            // Refresh evaluations (using the date of the entry to ensure correct month is updated if visible)
            ladeMonatBewertungen(LocalDate.parse(eintrag.datum, DateUtil.ISO_FORMAT))
            _uiState.value = _uiState.value.copy(successResId = R.string.entry_saved)
            onDone()
        }
    }

    fun loeschen(eintrag: TagEintrag, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.loeschen(eintrag)
            ladeMonatBewertungen(LocalDate.parse(eintrag.datum, DateUtil.ISO_FORMAT))
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
            ladeMonatBewertungen(LocalDate.now())
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
