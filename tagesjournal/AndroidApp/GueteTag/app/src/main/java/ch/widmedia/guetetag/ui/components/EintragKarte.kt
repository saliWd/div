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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
            .shadow(3.dp, RoundedCornerShape(18.dp), ambientColor = ratingCol.copy(alpha = 0.15f))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Rating circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ratingCol.copy(alpha = 0.12f))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = eintrag.bewertung.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = ratingCol,
                        fontSize = 22.sp
                    )
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = ratingCol,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Left color bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ratingCol)
            )

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateUtil.lokalDatum(eintrag.datum),
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepForest
                )
                if (eintrag.notizen.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = eintrag.notizen,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Arrow indicator
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = DividerColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
