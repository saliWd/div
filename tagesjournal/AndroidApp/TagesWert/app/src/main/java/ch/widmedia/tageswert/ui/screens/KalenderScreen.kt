package ch.widmedia.tageswert.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.widmedia.tageswert.ui.MainViewModel
import ch.widmedia.tageswert.ui.theme.*
import ch.widmedia.tageswert.utils.DateUtil
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalenderScreen(
    viewModel: MainViewModel,
    onEintragKlick: (String) -> Unit,
    onZurueck: () -> Unit,
) {
    var aktuellerMonat by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    val uiState by viewModel.uiState.collectAsState()
    val tageImMonat = remember(aktuellerMonat) { DateUtil.daysInMonth(aktuellerMonat) }

    LaunchedEffect(aktuellerMonat) {
        viewModel.ladeMonatBewertungen(aktuellerMonat)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        DateUtil.monthTitle(aktuellerMonat), 
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onZurueck) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Zurück", 
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { aktuellerMonat = aktuellerMonat.minusMonths(1) }) {
                        Icon(
                            Icons.Default.ChevronLeft, 
                            contentDescription = "Vorheriger Monat", 
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { aktuellerMonat = aktuellerMonat.plusMonths(1) }) {
                        Icon(
                            Icons.Default.ChevronRight, 
                            contentDescription = "Nächster Monat", 
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepForest)
            )
        },
        containerColor = Chamois
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Wochentage Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                val wochentage = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                wochentage.forEach { tag ->
                    Text(
                        text = tag,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepForest.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tageImMonat) { datum ->
                    if (datum != null) {
                        val isoDate = DateUtil.toIso(datum)
                        val bewertung = uiState.monatBewertungen[isoDate]
                        KalenderTagZelle(
                            datum = datum,
                            bewertung = bewertung,
                            onClick = { onEintragKlick(isoDate) }
                        )
                    } else {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = ratingColor(2), label = "1-3")
                    LegendItem(color = ratingColor(5), label = "4-5")
                    LegendItem(color = ratingColor(7), label = "6-7")
                    LegendItem(color = ratingColor(9), label = "8-10")
                }
            }
        }
    }
}

@Composable
fun KalenderTagZelle(
    datum: LocalDate,
    bewertung: Int?,
    onClick: () -> Unit
) {
    val heute = LocalDate.now()
    val istHeute = datum == heute
    val hatBewertung = bewertung != null
    
    val bgColor = when {
        hatBewertung -> ratingColor(bewertung!!)
        istHeute -> SageGreen.copy(alpha = 0.15f)
        else -> Color.White.copy(alpha = 0.6f)
    }
    
    val textColor = when {
        hatBewertung -> Color.White
        else -> DeepForest
    }

    val borderModifier = if (istHeute) {
        Modifier.background(SageGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .border(1.dp, SageGreen, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(borderModifier)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = datum.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = if (istHeute) FontWeight.Bold else FontWeight.Normal
            )
            if (hatBewertung) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label, 
            style = MaterialTheme.typography.labelMedium, 
            color = SlateGray
        )
    }
}
