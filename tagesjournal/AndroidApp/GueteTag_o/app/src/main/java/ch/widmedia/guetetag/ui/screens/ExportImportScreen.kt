package ch.widmedia.guetetag.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.widmedia.guetetag.data.AppDatabase
import ch.widmedia.guetetag.security.ExportImportHelper
import ch.widmedia.guetetag.ui.theme.Plum
import ch.widmedia.guetetag.viewmodel.ExportImportStatus
import ch.widmedia.guetetag.viewmodel.ExportImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(
    db: AppDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vm: ExportImportViewModel = viewModel(factory = ExportImportViewModel.factory(db))
    val status by vm.status.collectAsState()

    // Pre-fill the stored password from AndroidKeyStore
    var password by rememberSaveable {
        mutableStateOf(ExportImportHelper.retrieveExportPassword(context))
    }
    var passwordVisible by remember { mutableStateOf(false) }

    // SAF launchers
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) vm.exportDatabase(context, uri, password)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importDatabase(context, uri, password)
    }

    // Show Toast on status change, then reset
    LaunchedEffect(status) {
        when (status) {
            is ExportImportStatus.Success -> {
                Toast.makeText(context, (status as ExportImportStatus.Success).message, Toast.LENGTH_SHORT).show()
                vm.resetStatus()
            }
            is ExportImportStatus.Error -> {
                Toast.makeText(context, (status as ExportImportStatus.Error).message, Toast.LENGTH_LONG).show()
                vm.resetStatus()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "Export / Import",
                        style = MaterialTheme.typography.titleLarge
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.90f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // ── Password field (shared for export and import) ──────────
                Text(
                    text  = "Passwort für Export / Import",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = "Dieses Passwort wird sicher gespeichert und für beide Vorgänge verwendet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value             = password,
                    onValueChange     = { password = it },
                    modifier          = Modifier.fillMaxWidth(),
                    label             = { Text("Passwort") },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions   = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine        = true,
                    trailingIcon      = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible)
                                    "Passwort verbergen" else "Passwort anzeigen"
                            )
                        }
                    }
                )

                HorizontalDivider(
                    modifier  = Modifier.padding(vertical = 24.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant
                )

                // ── Export section ─────────────────────────────────────────
                Text(
                    text  = "Exportieren",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text  = "Alle Einträge werden verschlüsselt in eine Datei exportiert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick  = {
                        if (password.isNotBlank()) {
                            exportLauncher.launch("guetetag_export.guete")
                        } else {
                            Toast.makeText(context, "Bitte ein Passwort eingeben.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled  = status !is ExportImportStatus.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Plum)
                ) {
                    if (status is ExportImportStatus.Loading)
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    else
                        Text("Exportieren", style = MaterialTheme.typography.labelLarge)
                }

                HorizontalDivider(
                    modifier  = Modifier.padding(vertical = 24.dp),
                    color     = MaterialTheme.colorScheme.outlineVariant
                )

                // ── Import section ─────────────────────────────────────────
                Text(
                    text  = "Importieren",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text  = "Eine bestehende Export-Datei wird importiert. Bestehende Einträge bleiben erhalten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick  = {
                        if (password.isNotBlank()) {
                            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        } else {
                            Toast.makeText(context, "Bitte ein Passwort eingeben.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled  = status !is ExportImportStatus.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Plum)
                ) {
                    Text("Importieren", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── 10 % bottom spacer ─────────────────────────────────────────
            Spacer(modifier = Modifier.weight(0.10f))
        }
    }
}
