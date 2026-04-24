package ch.widmedia.guetetag.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.widmedia.guetetag.data.AppDatabase
import ch.widmedia.guetetag.ui.components.DiaryCalendar
import ch.widmedia.guetetag.ui.components.EntryListItem
import ch.widmedia.guetetag.viewmodel.MainViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    db: AppDatabase,
    onDayClick: (LocalDate) -> Unit,
    onExportImport: () -> Unit
) {
    val vm: MainViewModel = viewModel(factory = MainViewModel.factory(db))
    val entries    by vm.entries.collectAsState()
    val entryDates by vm.entryDates.collectAsState()
    val today      = remember { LocalDate.now() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "GueteTag",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    TextButton(onClick = onExportImport) {
                        Text(
                            text  = "Export / Import",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
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
            // ── Calendar – 40 % of available content height ──────────────
            Box(modifier = Modifier.fillMaxWidth().weight(0.40f)) {
                DiaryCalendar(
                    entryDates = entryDates,
                    today      = today,
                    onDayClick = onDayClick,
                    modifier   = Modifier.fillMaxSize()
                )
            }

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Entry list – next 50 % ───────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().weight(0.50f)) {
                if (entries.isEmpty()) {
                    Text(
                        text      = "Noch keine Einträge vorhanden.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp)
                    )
                } else {
                    LazyColumn(
                        modifier         = Modifier.fillMaxSize(),
                        contentPadding   = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(entries, key = { it.date }) { entry ->
                            EntryListItem(
                                entry   = entry,
                                onClick = { onDayClick(LocalDate.parse(entry.date)) }
                            )
                        }
                    }
                }
            }

            // ── 10 % empty space at bottom ───────────────────────────────
            Spacer(modifier = Modifier.weight(0.10f))
        }
    }
}
