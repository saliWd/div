package ch.widmedia.guetetag.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import ch.widmedia.guetetag.MainActivity
import ch.widmedia.guetetag.R
import ch.widmedia.guetetag.security.SecurityManager
import ch.widmedia.guetetag.ui.MainViewModel
import ch.widmedia.guetetag.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinstellungenScreen(
    viewModel: MainViewModel,
    onZurueck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Export state
    var exportPasswort by remember { mutableStateOf(SecurityManager.getExportPassword(context) ?: "") }
    var exportPasswortSichtbar by remember { mutableStateOf(value = false) }
    var exportLaeuft by remember { mutableStateOf(value = false) }

    // Import state
    var importPasswort by remember { mutableStateOf("") }
    var importPasswortSichtbar by remember { mutableStateOf(value = false) }
    var importUri by remember { mutableStateOf<Uri?>(value = null) }
    var importLaeuft by remember { mutableStateOf(value = false) }
    var importDateiName by remember { mutableStateOf("") }

    // File picker for import (GetContent is usually safer with FragmentActivity than CreateDocument)
    val dateiPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            importUri = it
            importDateiName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) cursor.getString(nameIndex) else null
            } ?: context.getString(R.string.import_file_select)
        }
    }

    val onExportResult: (Uri?) -> Unit = { uri ->
        uri?.let { targetUri ->
            exportLaeuft = true
            viewModel.getEncryptedExportData(context, exportPasswort) { data ->
                if (data != null) {
                    try {
                        context.contentResolver.openOutputStream(targetUri)?.use { output ->
                            output.write(data)
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.export_success))
                        }
                    } catch (_: Exception) {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.export_error))
                        }
                    }
                }
                exportLaeuft = false
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Chamois,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SageGreen,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DeepForest, SageGreen.copy(alpha = 0.8f))
                        )
                    )
                    .padding(top = 48.dp, bottom = 20.dp, start = 8.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onZurueck,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Section: Datenverwaltung
                SektionsKopf(text = stringResource(R.string.data_management), icon = Icons.Filled.Storage)

                // Export Card
                EinstellungsKarte(
                    titel = stringResource(R.string.export_confirm),
                    beschreibung = stringResource(R.string.export_description),
                    icon = Icons.Filled.Upload,
                    iconFarbe = SageGreen
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PasswortFeld(
                            wert = exportPasswort,
                            onWertChange = { exportPasswort = it },
                            label = stringResource(R.string.export_password),
                            sichtbar = exportPasswortSichtbar,
                        ) {
                            exportPasswortSichtbar = !exportPasswortSichtbar
                        }
                        Button(
                            onClick = {
                                if (exportPasswort.isBlank()) return@Button
                                val fileName = "guetetag_export_${System.currentTimeMillis()}.gtb"
                                activity?.launchFilePicker(fileName, onExportResult)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            enabled = exportPasswort.isNotBlank() && !exportLaeuft
                        ) {
                            if (exportLaeuft) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.export_confirm),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                // Import Card
                EinstellungsKarte(
                    titel = stringResource(R.string.import_confirm),
                    beschreibung = stringResource(R.string.import_description),
                    icon = Icons.Filled.Download,
                    iconFarbe = Terracotta
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // File picker
                        OutlinedButton(
                            onClick = { dateiPickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepForest)
                        ) {
                            Icon(Icons.Filled.FolderOpen, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = importDateiName.ifBlank { stringResource(R.string.import_file_select) }
                            )
                        }

                        PasswortFeld(
                            wert = importPasswort,
                            onWertChange = { importPasswort = it },
                            label = stringResource(R.string.import_password),
                            sichtbar = importPasswortSichtbar,
                        ) {
                            importPasswortSichtbar = !importPasswortSichtbar
                        }

                        // Warning
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = GoldAmber.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    null,
                                    tint = GoldAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.import_overwrite_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateGray
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val uri = importUri ?: return@Button
                                if (importPasswort.isBlank()) return@Button
                                importLaeuft = true
                                viewModel.importieren(
                                    context = context,
                                    uri = uri,
                                    password = importPasswort,
                                    onSuccess = {
                                        importLaeuft = false
                                        importUri = null
                                        importDateiName = ""
                                        importPasswort = ""
                                    },
                                ) { _ ->
                                    importLaeuft = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            enabled = (importUri != null) && importPasswort.isNotBlank() && (!importLaeuft)
                        ) {
                            if (importLaeuft) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSecondary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.import_confirm),
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SektionsKopf(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, tint = SageGreen, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = SageGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EinstellungsKarte(
    titel: String,
    beschreibung: String,
    icon: ImageVector,
    iconFarbe: Color,
    inhalt: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconFarbe.copy(alpha = 0.12f))
                ) {
                    Icon(icon, null, tint = iconFarbe, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = titel,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepForest
                    )
                    Text(
                        text = beschreibung,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )
                }
            }
            HorizontalDivider(color = DividerColor, thickness = 1.dp)
            inhalt()
        }
    }
}

@Composable
fun PasswortFeld(
    wert: String,
    onWertChange: (String) -> Unit,
    label: String,
    sichtbar: Boolean,
    onSichtbarToggle: () -> Unit
) {
    OutlinedTextField(
        value = wert,
        onValueChange = onWertChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (sichtbar) VisualTransformation.None
                               else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onSichtbarToggle) {
                Icon(
                    imageVector = if (sichtbar) Icons.Filled.VisibilityOff
                                  else Icons.Filled.Visibility,
                    contentDescription = if (sichtbar) stringResource(R.string.password_hide)
                                          else stringResource(R.string.password_show),
                    tint = SlateGray
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SageGreen,
            unfocusedBorderColor = DividerColor,
            cursorColor = SageGreen
        )
    )
}
