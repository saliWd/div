package ch.widmedia.guetetag.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.widmedia.guetetag.data.model.TagEintrag
import ch.widmedia.guetetag.ui.theme.*
import ch.widmedia.guetetag.utils.DateUtil

@Composable
fun EintragKarte(
    eintrag: TagEintrag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratingCol = ratingColor(eintrag.bewertung)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Smaller Rating circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ratingCol.copy(alpha = 0.1f))
            ) {
                Text(
                    text = eintrag.bewertung.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = ratingCol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Slimmer color bar
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(ratingCol)
            )

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateUtil.lokalDatum(eintrag.datum),
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepForest,
                    fontWeight = FontWeight.SemiBold
                )
                if (eintrag.notizen.isNotBlank()) {
                    Text(
                        text = eintrag.notizen,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Arrow indicator (smaller)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DividerColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
