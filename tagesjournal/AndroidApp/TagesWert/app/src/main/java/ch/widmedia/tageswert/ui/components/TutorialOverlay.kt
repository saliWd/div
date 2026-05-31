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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) {}
    ) {
        if (targetRect != null) {
            val isTopHalf = targetRect.center.y < (with(density) { LocalBoxHeight() } / 2)
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isTopHalf) Arrangement.Top else Arrangement.Bottom
            ) {
                if (isTopHalf) {
                    // Space for target
                    Spacer(modifier = Modifier.height(with(density) { targetRect.bottom.toDp() } + 8.dp))
                    
                    // Arrow pointing UP to target
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    TutorialCard(text, onNext, onSkip, isLastStep, modifier)
                } else {
                    TutorialCard(text, onNext, onSkip, isLastStep, modifier)
                    
                    // Arrow pointing DOWN to target
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    // Space for target
                    Spacer(modifier = Modifier.height(with(density) { (LocalBoxHeight() - targetRect.top).toDp() } + 8.dp))
                }
            }
        } else {
            // Default center positioning if no target
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TutorialCard(text, onNext, onSkip, isLastStep, modifier)
            }
        }
    }
}

@Composable
private fun LocalBoxHeight(): Float {
    // A simplified way to get height, though usually we'd use BoxWithConstraints
    return with(LocalDensity.current) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }
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
            .padding(horizontal = 24.dp, vertical = 8.dp)
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
