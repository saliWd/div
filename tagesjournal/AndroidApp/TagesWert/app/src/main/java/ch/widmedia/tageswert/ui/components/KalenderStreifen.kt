package ch.widmedia.tageswert.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.widmedia.tageswert.R
import ch.widmedia.tageswert.ui.theme.*
import ch.widmedia.tageswert.utils.DateUtil
import java.time.LocalDate

@Composable
fun KalenderStreifen(
    tageWithEintrag: Map<String, Int>,
    onDatumKlick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tage = remember { DateUtil.kalenderWochen() }
    val heute = remember { LocalDate.now() }
    val limit = remember(heute) { heute.minusDays(6) }

    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.calendar_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                fontWeight = FontWeight.Normal
            )
            // Legend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LegendePunkt(farbe = ratingColor(8), label = stringResource(R.string.legend_entry))
                LegendePunkt(farbe = LightChamois, label = stringResource(R.string.legend_empty))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large Calendar Card
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // First week
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tage.subList(0, 7).forEach { tag ->
                        val isoDate = DateUtil.toIso(tag)
                        val bewertung = tageWithEintrag[isoDate]
                        val istHeute = tag == heute
                        val isClickable = !tag.isBefore(limit) && !tag.isAfter(heute)
                        
                        KalenderTag(
                            datum = tag,
                            bewertung = bewertung,
                            istHeute = istHeute,
                            isClickable = isClickable,
                            onClick = { if (isClickable) onDatumKlick(isoDate) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Divider or Spacer between weeks
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = DividerColor.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )

                // Second week
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tage.subList(7, 14).forEach { tag ->
                        val isoDate = DateUtil.toIso(tag)
                        val bewertung = tageWithEintrag[isoDate]
                        val istHeute = tag == heute
                        val isClickable = !tag.isBefore(limit) && !tag.isAfter(heute)

                        KalenderTag(
                            datum = tag,
                            bewertung = bewertung,
                            istHeute = istHeute,
                            isClickable = isClickable,
                            onClick = { if (isClickable) onDatumKlick(isoDate) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

    }
}

@Composable
fun KalenderTag(
    datum: LocalDate,
    bewertung: Int?,
    istHeute: Boolean,
    isClickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hatEintrag = bewertung != null
    val targetBgColor = when {
        istHeute && bewertung != null -> ratingColor(bewertung)
        istHeute                     -> SageGreen.copy(alpha = 0.25f)
        bewertung != null && isClickable -> ratingColor(bewertung).copy(alpha = 0.8f)
        bewertung != null && !isClickable -> ratingColor(bewertung).copy(alpha = 0.25f)
        !isClickable                -> DividerColor.copy(alpha = 0.15f)
        else                         -> LightChamois.copy(alpha = 0.6f)
    }
    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(300),
        label = "kalenderFarbe"
    )

    val textColor = when {
        istHeute && hatEintrag -> Color.White
        istHeute              -> DeepForest
        hatEintrag && isClickable -> DeepForest
        hatEintrag && !isClickable -> DeepForest.copy(alpha = 0.5f)
        !isClickable         -> SlateGray.copy(alpha = 0.3f)
        else                  -> SlateGray
    }

    val borderMod = if (istHeute)
        Modifier.border(2.dp, SageGreen, RoundedCornerShape(12.dp))
    else
        Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .aspectRatio(0.7f) // Even slightly taller
            .then(borderMod)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .then(if (isClickable) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp, horizontal = 2.dp)
    ) {
        Text(
            text = DateUtil.wochentag(datum),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontSize = 11.sp, // Slightly larger
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(6.dp)) // More spacing
        Text(
            text = datum.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = textColor,
            fontSize = 18.sp // Slightly larger
        )
        if (hatEintrag) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (istHeute) Color.White 
                        else if (isClickable) Color.White.copy(alpha = 0.8f) 
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}


@Composable
fun LegendePunkt(farbe: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(farbe)
                .border(1.dp, DividerColor, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SlateGray,
            fontSize = 10.sp
        )
    }
}
