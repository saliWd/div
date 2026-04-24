package ch.widmedia.guetetag.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ch.widmedia.guetetag.data.AppDatabase
import ch.widmedia.guetetag.ui.screens.EntryScreen
import ch.widmedia.guetetag.ui.screens.ExportImportScreen
import ch.widmedia.guetetag.ui.screens.MainScreen

sealed class Screen {
    object Main : Screen()
    data class Entry(val dateString: String) : Screen()
    object ExportImport : Screen()
}

@Composable
fun GueteTagApp(db: AppDatabase) {
    var current by remember { mutableStateOf<Screen>(Screen.Main) }

    when (val screen = current) {
        is Screen.Main -> MainScreen(
            db               = db,
            onDayClick       = { date -> current = Screen.Entry(date.toString()) },
            onExportImport   = { current = Screen.ExportImport }
        )
        is Screen.Entry -> EntryScreen(
            db         = db,
            dateString = screen.dateString,
            onBack     = { current = Screen.Main }
        )
        is Screen.ExportImport -> ExportImportScreen(
            db     = db,
            onBack = { current = Screen.Main }
        )
    }
}
