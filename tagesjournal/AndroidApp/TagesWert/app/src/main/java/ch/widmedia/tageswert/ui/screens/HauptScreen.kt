package ch.widmedia.tageswert.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import ch.widmedia.tageswert.R
import ch.widmedia.tageswert.ui.MainViewModel
import ch.widmedia.tageswert.ui.TutorialStep
import ch.widmedia.tageswert.ui.components.EintragKarte
import ch.widmedia.tageswert.ui.components.MonatsKalender
import ch.widmedia.tageswert.ui.components.TutorialOverlay
import ch.widmedia.tageswert.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HauptScreen(
    viewModel: MainViewModel,
    onEintragKlick: (String) -> Unit,
    onEinstellungen: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alleEintraege by viewModel.alleEintraege.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    var aktuellerMonat by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }

    LaunchedEffect(uiState.isIntroShown) {
        if (!uiState.isIntroShown && uiState.tutorialStep == TutorialStep.NONE) {
            viewModel.startTutorial()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadLastExportTime(context)
    }

    LaunchedEffect(aktuellerMonat) {
        viewModel.ladeMonatBewertungen(aktuellerMonat)
    }

    // Show snackbar for success/error messages
    LaunchedEffect(uiState.successResId) {
        uiState.successResId?.let { resId ->
            snackbarHostState.showSnackbar(context.getString(resId))
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorResId) {
        uiState.errorResId?.let { resId ->
            snackbarHostState.showSnackbar(context.getString(resId))
            viewModel.clearMessages()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        containerColor = SageGreen,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            },
            containerColor = Chamois
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // Upper Part: Header and Calendar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column {
                        AppHeader(
                            onEinstellungen = onEinstellungen,
                            onLock = onLock
                        )
                        Spacer(Modifier.height(8.dp))
                        MonatsKalender(
                            aktuellerMonat = aktuellerMonat,
                            monatBewertungen = uiState.monatBewertungen,
                            onMonatWechsel = { aktuellerMonat = it },
                            onDatumKlick = onEintragKlick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coords ->
                                    if (uiState.tutorialStep == TutorialStep.WELCOME || 
                                        uiState.tutorialStep == TutorialStep.COLOR_EXPLANATION) {
                                        viewModel.setTargetRect(coords.boundsInWindow())
                                    }
                                }
                        )
                    }
                }

                // Separator
                HorizontalDivider(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    color = DividerColor,
                    thickness = 1.dp
                )

                // Lower Part: Entries List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (alleEintraege.isEmpty()) {
                        LeererZustand(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            item {
                                // Export Reminder
                                val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
                                val isOlderThan30Days = (System.currentTimeMillis() - uiState.lastExportTime) > thirtyDaysMillis
                                val firstStartOlderThan30Days = (System.currentTimeMillis() - uiState.firstStartTime) > thirtyDaysMillis
                                
                                if (isOlderThan30Days && firstStartOlderThan30Days && alleEintraege.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = GoldAmber.copy(alpha = 0.1f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAmber.copy(alpha = 0.2f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Warning,
                                                    contentDescription = null,
                                                    tint = GoldAmber,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.export_reminder_title),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = DeepForest,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = stringResource(R.string.export_reminder_text),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SlateGray,
                                                lineHeight = 18.sp
                                            )
                                            TextButton(
                                                onClick = onEinstellungen,
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.export_confirm),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = SageGreen
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp).padding(start = 4.dp),
                                                    tint = SageGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.entries_title),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = DeepForest
                                    )
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SageGreen.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = alleEintraege.size.toString(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SageGreen,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            itemsIndexed(
                                items = alleEintraege,
                                key = { _, eintrag -> eintrag.id }
                            ) { _, eintrag ->
                                EintragKarte(
                                    eintrag = eintrag,
                                    onClick = { onEintragKlick(eintrag.datum) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tutorial Overlay on top of Scaffold
        if (uiState.tutorialStep == TutorialStep.WELCOME) {
            TutorialOverlay(
                text = stringResource(R.string.tutorial_past_dates),
                onNext = { viewModel.advanceTutorial(context, onEintragKlick, {}) },
                onSkip = { viewModel.skipTutorial(context) },
                targetRect = uiState.targetRect
            )
        } else if (uiState.tutorialStep == TutorialStep.COLOR_EXPLANATION) {
            TutorialOverlay(
                text = stringResource(R.string.tutorial_color_change),
                onNext = { viewModel.advanceTutorial(context, { _ -> onEinstellungen() }, {}) },
                onSkip = { viewModel.skipTutorial(context) },
                targetRect = uiState.targetRect
            )
        }
    }
}

@Composable
fun AppHeader(onEinstellungen: () -> Unit, onLock: () -> Unit) {
    var isLocking by remember { mutableStateOf(false) }

    LaunchedEffect(isLocking) {
        if (isLocking) {
            delay(400) // Brief delay to show the lock animation
            onLock()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepForest, SageGreen.copy(alpha = 0.85f))
                )
            )
            .statusBarsPadding()
            .padding(start = 24.dp, end = 16.dp, bottom = 16.dp, top = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (!isLocking) isLocking = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Crossfade(targetState = isLocking, label = "lockAnimation") { locking ->
                            Icon(
                                imageVector = if (locking) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = stringResource(R.string.lock_title),
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(
                        onClick = onEinstellungen,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeererZustand(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌱",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.no_entries),
            style = MaterialTheme.typography.titleMedium,
            color = SlateGray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.no_entries_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = SlateGray.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
