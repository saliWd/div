package ch.widmedia.tageswert.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import ch.widmedia.tageswert.R
import ch.widmedia.tageswert.ui.MainViewModel
import ch.widmedia.tageswert.ui.MonatsStatistik
import ch.widmedia.tageswert.ui.TutorialStep
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
    onAlleEintraege: () -> Unit,
    onEinstellungen: () -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alleEintraege by viewModel.alleEintraege.collectAsState()
    val monatsStatistiken by viewModel.monatsStatistiken.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    var aktuellerMonat by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }

    // Scroll to tutorial items
    LaunchedEffect(uiState.tutorialStep) {
        if (uiState.tutorialStep == TutorialStep.RESTART_INFO) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

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
                // ... (no changes here)
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        containerColor = SageGreen,
                        contentColor = Color.White,
                        shape = AppCardDefaults.smallShape,
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
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .verticalScroll(scrollState)
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
                        
                        Spacer(Modifier.height(16.dp))
                        
                        StatistikSektion(
                            statistiken = monatsStatistiken,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Separator
                Spacer(Modifier.height(16.dp))

                // Lower Part: Summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    if (alleEintraege.isEmpty()) {
                        LeererZustand(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp)
                        )
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = AppCardDefaults.largeShape,
                            colors = AppCardDefaults.colors(),
                            elevation = AppCardDefaults.defaultElevation()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // ... same export reminder code ...
                                // Export Reminder
                                val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
                                val isOlderThan30Days = (System.currentTimeMillis() - uiState.lastExportTime) > thirtyDaysMillis
                                val firstStartOlderThan30Days = (System.currentTimeMillis() - uiState.firstStartTime) > thirtyDaysMillis
                                
                                if (isOlderThan30Days && firstStartOlderThan30Days) {
                                    Card(
                                        modifier = Modifier
                                            .padding(bottom = 16.dp)
                                            .fillMaxWidth(),
                                        shape = AppCardDefaults.shape,
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

                                Text(
                                    text = stringResource(R.string.total_entries_count, alleEintraege.size),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SlateGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick = onAlleEintraege,
                                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                                    shape = AppCardDefaults.smallShape,
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.show_all_entries),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }

                // Help & Tutorial Section at the bottom
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    shape = AppCardDefaults.largeShape,
                    colors = AppCardDefaults.colors(),
                    elevation = AppCardDefaults.defaultElevation()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tutorial_welcome_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepForest,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.restartTutorial(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .onGloballyPositioned { coords ->
                                    if (uiState.tutorialStep == TutorialStep.RESTART_INFO) {
                                        viewModel.setTargetRect(coords.boundsInWindow())
                                    }
                                },
                            shape = AppCardDefaults.smallShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.restart_tutorial),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        // Tutorial Overlay on top of Scaffold
        when (uiState.tutorialStep) {
            TutorialStep.WELCOME -> {
                TutorialOverlay(
                    text = stringResource(R.string.tutorial_past_dates),
                    onNext = { viewModel.advanceTutorial(context, onEintragKlick, {}) },
                    onSkip = { viewModel.skipTutorial(context) },
                    targetRect = uiState.targetRect
                )
            }
            TutorialStep.COLOR_EXPLANATION -> {
                TutorialOverlay(
                    text = stringResource(R.string.tutorial_color_change),
                    onNext = { viewModel.advanceTutorial(context, { _ -> onEinstellungen() }, {}) },
                    onSkip = { viewModel.skipTutorial(context) },
                    targetRect = uiState.targetRect
                )
            }
            TutorialStep.RESTART_INFO -> {
                TutorialOverlay(
                    text = stringResource(R.string.tutorial_settings_restart),
                    onNext = { viewModel.advanceTutorial(context, {}, {}) },
                    onSkip = { viewModel.skipTutorial(context) },
                    targetRect = uiState.targetRect,
                    isLastStep = true
                )
            }
            else -> {}
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
fun StatistikSektion(statistiken: List<MonatsStatistik>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp),
        shape = AppCardDefaults.largeShape,
        colors = AppCardDefaults.colors(),
        elevation = AppCardDefaults.defaultElevation()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.statistics_title),
                style = MaterialTheme.typography.titleSmall,
                color = DeepForest,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                statistiken.forEach { stat ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (stat.anzahl > 0) {
                            Text(
                                text = "%.1f".format(stat.durchschnitt),
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateGray,
                                fontSize = 10.sp
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        val heightFactor = (stat.durchschnitt / 10.0).toFloat()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .height((heightFactor * 70).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (stat.anzahl > 0) ratingColor(stat.durchschnitt.toInt()) 
                                    else DividerColor.copy(alpha = 0.3f)
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGray,
                            fontSize = 9.sp,
                            maxLines = 1
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
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.no_entries_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
