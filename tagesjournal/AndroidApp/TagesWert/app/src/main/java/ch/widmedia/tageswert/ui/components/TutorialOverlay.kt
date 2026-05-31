package ch.widmedia.tageswert.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.widmedia.tageswert.R
import ch.widmedia.tageswert.ui.theme.DeepForest
import ch.widmedia.tageswert.ui.theme.SageGreen

@Composable
fun TutorialOverlay(
    text: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    targetRect: Rect? = null,
    isLastStep: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = false) {}
    ) {
        val screenHeight = constraints.maxHeight.toFloat()
        
        if (targetRect != null) {
            val isTopHalf = targetRect.center.y < (screenHeight / 2)
            
            // Positioning logic:
            // If target is in top half, show card below target.
            // If target is in bottom half, show card above target.
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isTopHalf) Arrangement.Top else Arrangement.Bottom
            ) {
                if (isTopHalf) {
                    // Item is at the top. 
                    // Card should be below.
                    // Spacer from top of screen to bottom of element + small margin
                    val topPadding = (targetRect.bottom / density.density).coerceAtLeast(0f).dp
                    Spacer(modifier = Modifier.height(topPadding + 8.dp))
                    
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    TutorialCard(text, onNext, onSkip, isLastStep, modifier)
                } else {
                    // Item is at the bottom.
                    // Card should be above.
                    TutorialCard(text, onNext, onSkip, isLastStep, modifier)
                    
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )

                    // Spacer from top of element to bottom of screen
                    val bottomPadding = ((screenHeight - targetRect.top) / density.density).coerceAtLeast(0f).dp
                    Spacer(modifier = Modifier.height(bottomPadding + 8.dp))
                }
            }
        } else {
            // No target: Center the card
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TutorialCard(text, onNext, onSkip, isLastStep, modifier)
            }
        }
    }
}

@Composable
private fun TutorialCard(
    text: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastStep: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.tutorial_welcome_title),
                style = MaterialTheme.typography.titleMedium,
                color = DeepForest,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isLastStep) Arrangement.End else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLastStep) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(R.string.intro_skip),
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isLastStep) stringResource(R.string.tutorial_finish) else stringResource(R.string.intro_next),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (!isLastStep) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(start = 4.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
