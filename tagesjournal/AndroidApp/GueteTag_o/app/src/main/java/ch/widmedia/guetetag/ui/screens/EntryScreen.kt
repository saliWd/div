package ch.widmedia.guetetag.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.widmedia.guetetag.data.AppDatabase
import ch.widmedia.guetetag.ui.theme.Gold
import ch.widmedia.guetetag.ui.theme.Plum
import ch.widmedia.guetetag.viewmodel.EntryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TITLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    db: AppDatabase,
    dateString: String,
    onBack: () -> Unit
) {
    val vm: EntryViewModel = viewModel(factory = EntryViewModel.factory(db))
    val existingEntry by vm.entry.collectAsState()
    val saved         by vm.saved.collectAsState()
    val deleted       by vm.deleted.collectAsState()

    // Navigate back when save / delete completed
    LaunchedEffect(saved)   { if (saved)   { vm.resetFlags(); onBack() } }
    LaunchedEffect(deleted) { if (deleted) { vm.resetFlags(); onBack() } }

    // Load the entry once
    LaunchedEffect(dateString) { vm.loadEntry(dateString) }

    // Editable state – initialise from loaded entry
    var rating by rememberSaveable { mutableFloatStateOf(5f) }
    var text   by rememberSaveable { mutableStateOf("") }
    var deleteDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(existingEntry) {
        existingEntry?.let {
            rating = it.rating.toFloat()
            text   = it.text
        }
    }

    val dateTitle = remember(dateString) {
        runCatching { LocalDate.parse(dateString).format(TITLE_DATE_FORMATTER) }
            .getOrDefault(dateString)
            .replaceFirstChar { it.uppercase() }
    }

    if (deleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { deleteDialogVisible = false },
            title = { Text("Eintrag löschen") },
            text  = { Text("Möchten Sie diesen Eintrag wirklich löschen?") },
            confirmButton = {
                TextButton(onClick = {
                    deleteDialogVisible = false
                    vm.deleteEntry(dateString)
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogVisible = false }) { Text("Abbrechen") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = dateTitle,
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint               = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (existingEntry != null) {
                        IconButton(onClick = { deleteDialogVisible = true }) {
                            Icon(
                                imageVector        = Icons.Default.Delete,
                                contentDescription = "Eintrag löschen",
                                tint               = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable content (90 %)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.90f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // ── Bewertung ─────────────────────────────────────────────
                Text(
                    text  = "Bewertung",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text  = rating.toInt().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Gold
                )

                Slider(
                    value         = rating,
                    onValueChange = { rating = it },
                    valueRange    = 1f..10f,
                    steps         = 8,
                    colors        = SliderDefaults.colors(
                        thumbColor              = Gold,
                        activeTrackColor        = Gold,
                        inactiveTrackColor      = Gold.copy(alpha = 0.28f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(
                    modifier  = Modifier.padding(vertical = 20.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant
                )

                // ── Eintrag ───────────────────────────────────────────────
                Text(
                    text  = "Eintrag",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder   = { Text("Wie war dein Tag?") },
                    textStyle     = MaterialTheme.typography.bodyLarge,
                    maxLines      = 15
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Speichern button ──────────────────────────────────────
                Button(
                    onClick  = { vm.saveEntry(dateString, rating.toInt(), text) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Plum
                    )
                ) {
                    Text(
                        text  = "Speichern",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── 10 % bottom spacer ─────────────────────────────────────────
            Spacer(modifier = Modifier.weight(0.10f))
        }
    }
}
