package ch.widmedia.tageswert.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.widmedia.tageswert.R
import ch.widmedia.tageswert.ui.theme.*
import kotlin.math.*

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
    var cardRect by remember { mutableStateOf<Rect?>(null) }
    var overlayPos by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .onGloballyPositioned { overlayPos = it.positionInWindow() }
            .clickable(enabled = false) {}
    ) {
        val screenHeight = constraints.maxHeight.toFloat()
        
        if (targetRect != null) {
            val isTopHalf = targetRect.center.y < (screenHeight / 2)
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isTopHalf) Arrangement.Top else Arrangement.Bottom
            ) {
                if (isTopHalf) {
                    val topPadding = (targetRect.bottom / density.density).coerceAtLeast(0f).dp
                    Spacer(modifier = Modifier.height(topPadding + 48.dp))
                    
                    TutorialCard(
                        text = text,
                        onNext = onNext,
                        onSkip = onSkip,
                        isLastStep = isLastStep,
                        modifier = modifier.onGloballyPositioned { cardRect = it.boundsInWindow() }
                    )
                } else {
                    TutorialCard(
                        text = text,
                        onNext = onNext,
                        onSkip = onSkip,
                        isLastStep = isLastStep,
                        modifier = modifier.onGloballyPositioned { cardRect = it.boundsInWindow() }
                    )
                    
                    val bottomPadding = ((screenHeight - targetRect.top) / density.density).coerceAtLeast(0f).dp
                    Spacer(modifier = Modifier.height(bottomPadding + 48.dp))
                }
            }

            // Draw curved arrow
            cardRect?.let { card ->
                val localTarget = targetRect.center - overlayPos
                val localCard = card.translate(-overlayPos.x, -overlayPos.y)
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val start = if (isTopHalf) {
                        Offset(localCard.center.x, localCard.top)
                    } else {
                        Offset(localCard.center.x, localCard.bottom)
                    }
                    drawCurvedArrow(start, localTarget, Color.White)
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

private fun DrawScope.drawCurvedArrow(
    start: Offset,
    end: Offset,
    color: Color
) {
    val strokeWidth = 3.dp.toPx()
    val headLength = 16.dp.toPx()
    val headAngle = PI / 6

    val dx = end.x - start.x
    val dy = end.y - start.y
    val distance = sqrt(dx * dx + dy * dy)
    
    // Normalize direction
    val ux = dx / distance
    val uy = dy / distance
    
    // Perpendicular vector for the curve bulge
    val px = -uy
    val py = ux
    
    // Bulge based on distance, but limited
    val bulge = (distance * 0.25f).coerceAtMost(60.dp.toPx())
    
    // Control point for quadratic curve
    val control = Offset(
        x = start.x + dx * 0.5f + px * bulge,
        y = start.y + dy * 0.5f + py * bulge
    )

    val path = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(control.x, control.y, end.x, end.y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Arrow head
    // The direction at the end of a quadratic bezier is from control to end
    val angle = atan2(end.y - control.y, end.x - control.x)

    val p1 = Offset(
        x = (end.x - headLength * cos(angle - headAngle)).toFloat(),
        y = (end.y - headLength * sin(angle - headAngle)).toFloat()
    )
    val p2 = Offset(
        x = (end.x - headLength * cos(angle + headAngle)).toFloat(),
        y = (end.y - headLength * sin(angle + headAngle)).toFloat()
    )

    val headPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(p1.x, p1.y)
        moveTo(end.x, end.y)
        lineTo(p2.x, p2.y)
    }

    drawPath(
        path = headPath,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
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
        shape = AppCardDefaults.largeShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = AppCardDefaults.highElevation()
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
                    shape = AppCardDefaults.smallShape
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
