package ch.widmedia.guetetag.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.sp
import ch.widmedia.guetetag.R
import ch.widmedia.guetetag.ui.MainViewModel
import ch.widmedia.guetetag.ui.components.EintragKarte
import ch.widmedia.guetetag.ui.components.KalenderStreifen
import ch.widmedia.guetetag.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HauptScreen(
    viewModel: MainViewModel,
    onEintragKlick: (String) -> Unit,
    onEinstellungen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alleEintraege by viewModel.alleEintraege.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar for success/error messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier,
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
        },
        containerColor = Chamois
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 120.dp)  // 10% bottom
        ) {
            // App Header
            item {
                AppHeader(onEinstellungen = onEinstellungen)
            }

            // Kalender
            item {
                Spacer(Modifier.height(8.dp))
                KalenderStreifen(
                    tageWithEintrag = uiState.tageWithEintrag,
                    onDatumKlick = onEintragKlick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            // Trennlinie + Einträge-Titel
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = DividerColor,
                    thickness = 1.dp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.entries_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepForest
                    )
                    if (alleEintraege.isNotEmpty()) {
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
                Spacer(Modifier.height(12.dp))
            }

            // Leer-Zustand
            if (alleEintraege.isEmpty()) {
                item {
                    LeererZustand(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp)
                    )
                }
            }

            // Eintrags-Liste
            itemsIndexed(
                items = alleEintraege,
                key = { _, eintrag -> eintrag.id }
            ) { _, eintrag ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
                ) {
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

@Composable
fun AppHeader(onEinstellungen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepForest, SageGreen.copy(alpha = 0.85f))
                )
            )
            .padding(top = 48.dp, start = 24.dp, end = 16.dp, bottom = 24.dp)
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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
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
