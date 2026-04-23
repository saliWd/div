package ch.widmedia.guetetag.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ch.widmedia.guetetag.R

// ─────────────────────────────────────────────
// Font families
// ─────────────────────────────────────────────

private val TitleFont = FontFamily(
    Font(
        resId = R.font.raleway_regular,
        weight = FontWeight.Normal
    )
)

private val BodyFont = FontFamily.SansSerif

// ─────────────────────────────────────────────
// Typography tuning
// ─────────────────────────────────────────────

private val AppTypography = Typography(

    // App title (e.g. “GueteTag”)
    titleLarge = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.04.em   // ✅ subtle openness
    ),

    // Section headers (e.g. “Bewertung”, “Kalender”)
    titleMedium = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.03.em
    ),

    // Smaller headers / captions
    titleSmall = TextStyle(
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.em
    ),

    // Body text (entries, labels, dialogs)
    bodyLarge = TextStyle(
        fontFamily = BodyFont,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.em     // ✅ keep neutral
    ),

    bodyMedium = TextStyle(
        fontFamily = BodyFont,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.em
    )
)

// ─────────────────────────────────────────────
// Color scheme (unchanged)
 // ─────────────────────────────────────────────

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF81C784),
    background = Color(0xFFF1F8E9)
)

// ─────────────────────────────────────────────
// App theme
// ─────────────────────────────────────────────

@Composable
fun GueteTagTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}