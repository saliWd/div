package ch.widmedia.guetetag.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.widmedia.guetetag.data.AppDatabase
import ch.widmedia.guetetag.data.DataPortabilityManager
import ch.widmedia.guetetag.data.Entry
import ch.widmedia.guetetag.security.SecurityManager
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onNavigateToEntry: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val entryDao = database.entryDao()
    val portabilityManager = remember { DataPortabilityManager(context) }
    val securityManager = remember { SecurityManager(context) }

    val today = LocalDate.now()
    val last14Days = (0..13).map { today.minusDays(it.toLong()) }.reversed()

    val entries by entryDao.getAllEntries().collectAsState(initial = emptyList())
    val entryDates = entries.map {
        Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate()
    }.toSet()

    var showSettings by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordAction by remember { mutableStateOf<PasswordAction?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            val savedPassword = securityManager.getExportPassword()
            if (savedPassword.isNullOrBlank()) {
                tempUri = uri
                passwordAction = PasswordAction.EXPORT
                showPasswordDialog = true
            } else {
                scope.launch {
                    val result = portabilityManager.exportDatabase(uri, savedPassword)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Export erfolgreich!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Export fehlgeschlagen: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            tempUri = uri
            passwordAction = PasswordAction.IMPORT
            showPasswordDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GueteTag", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(bottom = 80.dp) // 10% bottom padding
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Wie geht's dir heute?",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(last14Days) { date ->
                    val hasEntry = entryDates.contains(date)
                    val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    CalendarDay(
                        date = date,
                        hasEntry = hasEntry,
                        onClick = { onNavigateToEntry(dateMillis) }
                    )
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Daten-Portabilität", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        showSettings = false
                        exportLauncher.launch("guetetag_backup.db")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Daten exportieren")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        showSettings = false
                        importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Daten importieren")
                }
            }
        }
    }

    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(if (passwordAction == PasswordAction.EXPORT) "Export-Passwort festlegen" else "Import-Passwort eingeben") },
            text = {
                Column {
                    Text("Dieses Passwort wird zur Verschlüsselung Ihrer Exportdatei verwendet.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Passwort") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    val uri = tempUri ?: return@TextButton
                    val action = passwordAction ?: return@TextButton
                    
                    if (action == PasswordAction.EXPORT) {
                        securityManager.saveExportPassword(passwordInput)
                    }
                    
                    scope.launch {
                        val result = if (action == PasswordAction.EXPORT) {
                            portabilityManager.exportDatabase(uri, passwordInput)
                        } else {
                            portabilityManager.importDatabase(uri, passwordInput)
                        }
                        
                        if (result.isSuccess) {
                            Toast.makeText(context, "Erfolgreich!", Toast.LENGTH_SHORT).show()
                            if (action == PasswordAction.IMPORT) {
                                // Refresh? Actually replacing DB might need a restart.
                                // For now just toast.
                            }
                        } else {
                            Toast.makeText(context, "Fehlgeschlagen: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

enum class PasswordAction { EXPORT, IMPORT }

@Composable
fun CalendarDay(date: LocalDate, hasEntry: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (hasEntry) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (hasEntry) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val formatter = DateTimeFormatter.ofPattern("d.", Locale.GERMAN)
    val dayFormatter = DateTimeFormatter.ofPattern("E", Locale.GERMAN)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(
            text = date.format(dayFormatter),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
        Text(
            text = date.format(formatter),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}
