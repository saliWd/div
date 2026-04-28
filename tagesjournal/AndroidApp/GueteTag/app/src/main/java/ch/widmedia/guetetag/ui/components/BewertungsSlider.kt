package ch.widmedia.guetetag.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import ch.widmedia.guetetag.R
import ch.widmedia.guetetag.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun BewertungsSlider(
    bewertung: Int,
    onBewertungChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratingCol by animateColorAsState(
        targetValue = ratingColor(bewertung),
        animationSpec = tween(300),
        label = "ratingColorAnim"
    )

    Column(modifier = modifier) {
        // Label + value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = DeepForest
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ratingCol.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = bewertung.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ratingCol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slider
        Slider(
            value = bewertung.toFloat(),
            onValueChange = { onBewertungChange(it.roundToInt()) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = ratingCol,
                activeTrackColor = ratingCol,
                activeTickColor = ratingCol.copy(alpha = 0.5f),
                inactiveTrackColor = ratingCol.copy(alpha = 0.2f),
                inactiveTickColor = Color.Transparent
            )
        )

        // Scale indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (1..10).forEach { n ->
                Text(
                    text = n.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (n == bewertung) ratingCol else SlateGray.copy(alpha = 0.5f),
                    fontWeight = if (n == bewertung) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                )
            }
        }
    }
}
