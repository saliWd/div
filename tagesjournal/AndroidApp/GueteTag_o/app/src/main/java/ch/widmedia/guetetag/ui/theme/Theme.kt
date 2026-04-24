package ch.widmedia.guetetag.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Colour Palette ───────────────────────────────────────────────────────────

val Plum          = Color(0xFF3B1F2B)   // primary  – deep, noble plum
val PlumDark      = Color(0xFF2A1320)
val PlumContainer = Color(0xFF7A3E58)
val Gold          = Color(0xFFC5973E)   // secondary – antique gold
val GoldLight     = Color(0xFFF5D98C)
val Ivory         = Color(0xFFF9F4EE)   // background – warm ivory
val WarmWhite     = Color(0xFFFDFAF6)   // surface
val SurfaceVar    = Color(0xFFEDE3DA)
val Charcoal      = Color(0xFF2C1810)   // on-background
val Outline       = Color(0xFF9E8A7F)
val OutlineVar    = Color(0xFFD4C5BB)

private val DiaryColorScheme = lightColorScheme(
    primary              = Plum,
    onPrimary            = Color.White,
    primaryContainer     = PlumContainer,
    onPrimaryContainer   = Color(0xFFFFEBF3),
    secondary            = Gold,
    onSecondary          = Color.White,
    secondaryContainer   = GoldLight,
    onSecondaryContainer = Color(0xFF3A2800),
    background           = Ivory,
    onBackground         = Charcoal,
    surface              = WarmWhite,
    onSurface            = Charcoal,
    surfaceVariant       = SurfaceVar,
    onSurfaceVariant     = Color(0xFF5C4B43),
    outline              = Outline,
    outlineVariant       = OutlineVar
)

// ── Typography ───────────────────────────────────────────────────────────────
// Titles → built-in Serif  |  Everything else → built-in SansSerif

private val DiaryTypography = Typography(
    // Serif titles
    displayLarge  = TextStyle(fontFamily = FontFamily.Serif,    fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Serif,    fontWeight = FontWeight.Bold,   fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif,    fontWeight = FontWeight.Bold,   fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = 0.sp),
    headlineMedium= TextStyle(fontFamily = FontFamily.Serif,    fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif,    fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    titleLarge    = TextStyle(fontFamily = FontFamily.Serif,    fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium   = TextStyle(fontFamily = FontFamily.Serif,    fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    titleSmall    = TextStyle(fontFamily = FontFamily.Serif,    fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    // SansSerif body & labels
    bodyLarge     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall     = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)

// ── Theme Composable ─────────────────────────────────────────────────────────

@Composable
fun GueteTagTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DiaryColorScheme,
        typography  = DiaryTypography,
        content     = content
    )
}
