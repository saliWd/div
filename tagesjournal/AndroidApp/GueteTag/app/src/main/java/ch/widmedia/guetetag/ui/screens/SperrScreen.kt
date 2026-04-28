package ch.widmedia.guetetag.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.widmedia.guetetag.R
import ch.widmedia.guetetag.ui.theme.*

enum class AuthStatus { WAITING, SCANNING, SUCCESS, FAILED, ERROR }

@Composable
fun SperrScreen(
    onAuthentifiziert: () -> Unit,
    onTriggerAuth: () -> Unit,
    authStatus: AuthStatus,
    fehlermeldung: String?,
    modifier: Modifier = Modifier,
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )

    val iconColor = when (authStatus) {
        AuthStatus.SUCCESS -> SageGreen
        AuthStatus.FAILED, AuthStatus.ERROR -> ErrorRed
        AuthStatus.SCANNING -> GoldAmber
        else -> Color.White.copy(alpha = 0.8f)
    }

    LaunchedEffect(authStatus) {
        if (authStatus == AuthStatus.SUCCESS) {
            kotlinx.coroutines.delay(400)
            onAuthentifiziert()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to DeepForest,
                        0.6f to SageGreen.copy(alpha = 0.85f),
                        1.0f to Color(0xFF2D6B4A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            // App Icon / Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // Fingerprint Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .scale(if (authStatus == AuthStatus.SCANNING) pulse else 1f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.auth_title),
                        tint = iconColor,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            // Status text
            AnimatedContent(
                targetState = authStatus,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "statusText"
            ) { status ->
                val (text, color) = when (status) {
                    AuthStatus.WAITING  -> stringResource(R.string.auth_waiting) to Color.White.copy(alpha = 0.75f)
                    AuthStatus.SCANNING -> stringResource(R.string.auth_subtitle) to GoldAmber
                    AuthStatus.SUCCESS  -> stringResource(R.string.auth_welcome) to SageGreen
                    AuthStatus.FAILED   -> stringResource(R.string.auth_retry) to ErrorRed
                    AuthStatus.ERROR    -> (fehlermeldung ?: stringResource(R.string.auth_error)) to ErrorRed
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            // Unlock Button
            Button(
                onClick = onTriggerAuth,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                ),
                enabled = (authStatus != AuthStatus.SCANNING) && (authStatus != AuthStatus.SUCCESS)
            ) {
                Text(
                    text = stringResource(R.string.auth_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        // Bottom spacing (10%)
        Spacer(modifier = Modifier.align(Alignment.BottomCenter).height(80.dp))
    }
}
