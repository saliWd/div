package ch.widmedia.tageswert.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import ch.widmedia.tageswert.R
import ch.widmedia.tageswert.data.model.TagEintrag
import ch.widmedia.tageswert.ui.MainViewModel
import ch.widmedia.tageswert.ui.TutorialStep
import ch.widmedia.tageswert.ui.components.BewertungsSlider
import ch.widmedia.tageswert.ui.components.TutorialOverlay
import ch.widmedia.tageswert.ui.theme.*
import ch.widmedia.tageswert.utils.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EintragScreen(
    datum: String,
    viewModel: MainViewModel,
    onZurueck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val eintrag = viewModel.editingEintrag
    var showDeleteDialog by remember { mutableStateOf(value = false) }
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Load existing entry for this date
    LaunchedEffect(datum) {
        viewModel.startEditing(datum)
    }

    // Clear editing state when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopEditing()
        }
    }

    if (eintrag == null) {
        // Loading state or error
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SageGreen)
        }
        return
    }

    val isNew = eintrag.id == 0L

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
                        viewModel.loeschen(eintrag) { onZurueck() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = Surface,
            shape = AppCardDefaults.largeShape
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                // Custom top bar with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(bottom = 20.dp, start = 8.dp, end = 16.dp)
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
                                text = DateUtil.lokalDatumMitWochentagLang(datum),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        if (!isNew) {
                            IconButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(ErrorRed.copy(alpha = 0.4f))
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
                visible = true,
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
                        shape = AppCardDefaults.largeShape,
                        colors = AppCardDefaults.colors(),
                        elevation = AppCardDefaults.defaultElevation()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            BewertungsSlider(
                                bewertung = eintrag.bewertung,
                                onBewertungChange = { viewModel.updateEditing(bewertung = it) },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    if (uiState.tutorialStep == TutorialStep.RATING) {
                                        viewModel.setTargetRect(coords.boundsInWindow())
                                    }
                                }
                            )
                        }
                    }

                    // Notes Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppCardDefaults.largeShape,
                        colors = AppCardDefaults.colors(),
                        elevation = AppCardDefaults.defaultElevation()
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
                                value = eintrag.notizen,
                                onValueChange = { viewModel.updateEditing(notizen = it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp)
                                    .onGloballyPositioned { coords ->
                                        if (uiState.tutorialStep == TutorialStep.NOTES) {
                                            viewModel.setTargetRect(coords.boundsInWindow())
                                        }
                                    },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.entry_text_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SlateGray.copy(alpha = 0.5f)
                                    )
                                },
                                shape = AppCardDefaults.smallShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SageGreen,
                                    unfocusedBorderColor = DividerColor,
                                    focusedContainerColor = Surface,
                                    unfocusedContainerColor = Surface,
                                    cursorColor = SageGreen
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    keyboardType = KeyboardType.Text
                                ),
                                maxLines = 12
                            )
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            viewModel.speichern(eintrag) { onZurueck() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .onGloballyPositioned { coords ->
                                if (uiState.tutorialStep == TutorialStep.SAVE) {
                                    viewModel.setTargetRect(coords.boundsInWindow())
                                }
                            },
                        shape = AppCardDefaults.shape,
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

        // Tutorial Overlay on top of everything
        when (uiState.tutorialStep) {
            TutorialStep.RATING -> {
                TutorialOverlay(
                    text = stringResource(R.string.tutorial_rating),
                    onNext = { viewModel.advanceTutorial(context, {}, onZurueck) },
                    onSkip = { viewModel.skipTutorial(context) },
                    targetRect = uiState.targetRect
                )
            }
            TutorialStep.NOTES -> {
                TutorialOverlay(
                    text = stringResource(R.string.tutorial_notes),
                    onNext = { viewModel.advanceTutorial(context, {}, onZurueck) },
                    onSkip = { viewModel.skipTutorial(context) },
                    targetRect = uiState.targetRect
                )
            }
            TutorialStep.SAVE -> {
                TutorialOverlay(
                    text = stringResource(R.string.tutorial_save),
                    onNext = { viewModel.advanceTutorial(context, {}, onZurueck) },
                    onSkip = { viewModel.skipTutorial(context) },
                    targetRect = uiState.targetRect
                )
            }
            else -> {}
        }
    }
}
