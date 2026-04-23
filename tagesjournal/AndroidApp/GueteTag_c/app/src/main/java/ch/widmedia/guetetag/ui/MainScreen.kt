
package ch.widmedia.guetetag.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.widmedia.guetetag.db.EntryDao
import ch.widmedia.guetetag.export.DatabaseExportManager
import ch.widmedia.guetetag.security.ExportPasswordStore
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

@Composable
fun MainScreen(dao: EntryDao, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var showExport by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("GueteTag", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = { showExport = true }) { Text("Export") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { showImport = true }) { Text("Import") }
        }
        Spacer(Modifier.height(16.dp))
        Text("Kalender & Einträge")
        Spacer(Modifier.weight(0.1f))
    }

    if (showExport) PasswordDialog(
        title = "Export-Passwort",
        onConfirm = { pwd ->
            ExportPasswordStore.save(LocalContext.current, pwd)
            val file = File(LocalContext.current.filesDir, "guetetag.export")
            DatabaseExportManager.export(LocalContext.current, pwd.encodeToByteArray(), file)
            scope.launch { snackbar.showSnackbar("Export erfolgreich") }
            showExport = false
        },
        onDismiss = { showExport = false }
    )

    if (showImport) PasswordDialog(
        title = "Import-Passwort",
        onConfirm = { pwd ->
            val file = File(LocalContext.current.filesDir, "guetetag.export")
            DatabaseExportManager.import(LocalContext.current, pwd.encodeToByteArray(), file)
            scope.launch { snackbar.showSnackbar("Import abgeschlossen – App neu starten") }
            showImport = false
        },
        onDismiss = { showImport = false }
    )
}
