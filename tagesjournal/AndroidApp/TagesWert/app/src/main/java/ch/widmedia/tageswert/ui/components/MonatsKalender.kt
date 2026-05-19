package ch.widmedia.tageswert.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CardBg)
            .padding(16.dp)
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

        // Legende
        Row(
            modifier = Modifier.fillMaxWidth(),
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

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SlateGray,
            fontSize = 10.sp
        )
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
    val hatBewertung = bewertung != null
    
    val bgColor = when {
        hatBewertung && !istZukunft -> ratingColor(bewertung!!)
        istHeute -> SageGreen.copy(alpha = 0.15f)
        istZukunft -> DividerColor.copy(alpha = 0.1f)
        else -> Color.White.copy(alpha = 0.6f)
    }
    
    val textColor = when {
        hatBewertung && !istZukunft -> Color.White
        istZukunft -> SlateGray.copy(alpha = 0.3f)
        else -> DeepForest
    }

    val borderModifier = if (istHeute) {
        Modifier.border(1.dp, SageGreen, RoundedCornerShape(10.dp))
    } else {
        Modifier
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
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
                fontWeight = if (istHeute) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            if (hatBewertung && !istZukunft) {
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                )
            }
        }
    }
}
