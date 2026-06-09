package ch.widmedia.tageswert.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import ch.widmedia.tageswert.ui.theme.*
import ch.widmedia.tageswert.utils.DateUtil
import java.time.LocalDate

@Composable
fun MonatsKalender(
    aktuellerMonat: LocalDate,
    monatBewertungen: Map<String, Int>,
    onMonatWechsel: (LocalDate) -> Unit,
    onDatumKlick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tageImMonat = remember(aktuellerMonat) { DateUtil.daysInMonth(aktuellerMonat) }
    val heute = remember { LocalDate.now() }

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
            // Monat-Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonatWechsel(aktuellerMonat.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Vorheriger Monat", tint = DeepForest)
                }
                Text(
                    text = DateUtil.monthTitle(aktuellerMonat),
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepForest,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onMonatWechsel(aktuellerMonat.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Nächster Monat", tint = DeepForest)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Wochentage Header
            Row(modifier = Modifier.fillMaxWidth()) {
                val wochentage = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                wochentage.forEach { tag ->
                    Text(
                        text = tag,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Kalender-Gitter
            val rows = tageImMonat.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { woche ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        woche.forEach { datum ->
                            if (datum != null) {
                                val isoDate = DateUtil.toIso(datum)
                                val bewertung = monatBewertungen[isoDate]
                                val istHeute = datum == heute
                                val istZukunft = datum.isAfter(heute)

                                MonatsTagZelle(
                                    datum = datum,
                                    bewertung = bewertung,
                                    istHeute = istHeute,
                                    istZukunft = istZukunft,
                                    onClick = { if (!istZukunft) onDatumKlick(isoDate) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
fun MonatsTagZelle(
    datum: LocalDate,
    bewertung: Int?,
    istHeute: Boolean,
    istZukunft: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        bewertung != null && !istZukunft -> ratingColor(bewertung).copy(alpha = 0.15f)
        istHeute -> SageGreen.copy(alpha = 0.15f)
        istZukunft -> DividerColor.copy(alpha = 0.1f)
        else -> Color.White.copy(alpha = 0.6f)
    }
    
    val textColor = when {
        bewertung != null && !istZukunft -> ratingColor(bewertung)
        istZukunft -> SlateGray.copy(alpha = 0.3f)
        else -> DeepForest
    }

    val borderModifier = if (istHeute) {
        Modifier.border(1.2.dp, SageGreen, AppCardDefaults.smallShape)
    } else {
        Modifier
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(AppCardDefaults.smallShape)
            .background(bgColor)
            .then(borderModifier)
            .then(if (!istZukunft) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = datum.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (istHeute || (bewertung != null && !istZukunft)) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            if (bewertung != null && !istZukunft) {
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.7f))
                )
            }
        }
    }
}
