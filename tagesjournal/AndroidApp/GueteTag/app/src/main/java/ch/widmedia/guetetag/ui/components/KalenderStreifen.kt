package ch.widmedia.guetetag.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.widmedia.guetetag.ui.theme.*
import ch.widmedia.guetetag.utils.DateUtil
import java.time.LocalDate

@Composable
fun KalenderStreifen(
    tageWithEintrag: Set<String>,
    onDatumKlick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tage = remember { DateUtil.letzte14Tage() }
    val heute = remember { LocalDate.now() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, tage.size - 7))

    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Letzte 14 Tage",
                style = MaterialTheme.typography.titleLarge,
                color = DeepForest
            )
            // Legend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LegendePunkt(farbe = PaleGreen, label = "Eintrag")
                LegendePunkt(farbe = LightChamois, label = "Kein Eintrag")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tage) { tag ->
                val isoDate = DateUtil.toIso(tag)
                val hatEintrag = isoDate in tageWithEintrag
                val istHeute = tag == heute
                KalenderTag(
                    datum = tag,
                    hatEintrag = hatEintrag,
                    istHeute = istHeute,
                    onClick = { onDatumKlick(isoDate) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Month indicators under row
        MonatsIndikator(tage = tage)
    }
}

@Composable
fun KalenderTag(
    datum: LocalDate,
    hatEintrag: Boolean,
    istHeute: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetBgColor = when {
        istHeute && hatEintrag -> SageGreen
        istHeute              -> SageGreen.copy(alpha = 0.35f)
        hatEintrag            -> PaleGreen
        else                  -> LightChamois
    }
    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(300),
        label = "kalenderFarbe"
    )

    val textColor = when {
        istHeute && hatEintrag -> Color.White
        istHeute              -> DeepForest
        hatEintrag            -> SageGreen
        else                  -> SlateGray
    }

    val borderMod = if (istHeute)
        Modifier.border(2.dp, SageGreen, RoundedCornerShape(14.dp))
    else
        Modifier

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(52.dp)
            .then(borderMod)
            .shadow(if (istHeute) 4.dp else 1.dp, RoundedCornerShape(14.dp), ambientColor = SageGreen.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Text(
            text = DateUtil.wochentag(datum),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = datum.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (istHeute) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor,
            fontSize = 18.sp
        )
        if (hatEintrag) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (istHeute) Color.White else SageGreen)
            )
        }
    }
}

@Composable
fun MonatsIndikator(tage: List<LocalDate>) {
    // Show month name at boundary positions
    val monate = tage.mapIndexed { index, date ->
        val prev = if (index > 0) tage[index - 1].monthValue else -1
        if (date.monthValue != prev) Pair(index, "${DateUtil.monatName(date.monthValue)}") else null
    }.filterNotNull()

    if (monate.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            monate.forEach { (_, name) ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
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
