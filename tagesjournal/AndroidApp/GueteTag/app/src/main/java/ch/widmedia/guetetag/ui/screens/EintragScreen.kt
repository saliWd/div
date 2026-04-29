package ch.widmedia.guetetag.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.widmedia.guetetag.R
import ch.widmedia.guetetag.data.model.TagEintrag
import ch.widmedia.guetetag.ui.MainViewModel
import ch.widmedia.guetetag.ui.components.BewertungsSlider
import ch.widmedia.guetetag.ui.theme.*
import ch.widmedia.guetetag.utils.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EintragScreen(
    datum: String,
    viewModel: MainViewModel,
    onZurueck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var eintrag by remember { mutableStateOf<TagEintrag?>(value = null) }
    var bewertung by remember { mutableIntStateOf(value = 5) }
    var notizen by remember { mutableStateOf(value = "") }
    var isLoaded by remember { mutableStateOf(value = false) }
    var showDeleteDialog by remember { mutableStateOf(value = false) }

    // Load existing entry for this date
    LaunchedEffect(datum) {
        val existing = viewModel.eintragFuerDatum(datum)
        eintrag = existing
        bewertung = existing?.bewertung ?: 5
        notizen = existing?.notizen ?: ""
        isLoaded = true
    }

    val isNew = eintrag == null

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.delete_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepForest,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_confirm_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        eintrag?.let { viewModel.loeschen(it) { onZurueck() } }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = Chamois,
        topBar = {
            // Custom top bar with gradient
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isNew) stringResource(R.string.new_entry) else stringResource(R.string.edit_entry),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = DateUtil.lokalDatum(datum),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    if (!isNew) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ErrorRed.copy(alpha = 0.25f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = isLoaded,
            enter = fadeIn() + slideInVertically { it / 3 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 120.dp), // 10% bottom margin
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Rating Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        BewertungsSlider(
                            bewertung = bewertung,
                            onBewertungChange = { bewertung = it }
                        )
                    }
                }

                // Notes Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.entry_text),
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepForest
                        )
                        OutlinedTextField(
                            value = notizen,
                            onValueChange = { notizen = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.entry_text_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateGray.copy(alpha = 0.5f)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SageGreen,
                                unfocusedBorderColor = DividerColor,
                                focusedContainerColor = Surface,
                                unfocusedContainerColor = Surface,
                                cursorColor = SageGreen
                            ),
                            maxLines = 8
                        )
                    }
                }

                // Save Button
                Button(
                    onClick = {
                        val toSave = eintrag?.copy(
                            bewertung = bewertung,
                            notizen = notizen,
                            datum = datum
                        ) ?: TagEintrag(
                            datum = datum,
                            bewertung = bewertung,
                            notizen = notizen
                        )
                        viewModel.speichern(toSave) { onZurueck() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.save),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
