package ch.widmedia.guetetag.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ch.widmedia.guetetag.R

// ── Farben ──────────────────────────────────────────────────────────────────
val Chamois       = Color(0xFFF5EDD6)     // warm parchment background
val DeepForest    = Color(0xFF1A3A2E)     // primary dark green
val SageGreen     = Color(0xFF4A7C59)     // primary variant
val MossLight     = Color(0xFFB8D4C0)     // secondary
val Terracotta    = Color(0xFFC4673A)     // accent / rated high
val GoldAmber     = Color(0xFFD4A843)     // rating accent
val SlateGray     = Color(0xFF5A5F60)     // secondary text
val PaleGreen     = Color(0xFFDDEEE2)     // calendar: has entry
val LightChamois  = Color(0xFFEEE4CC)     // calendar: no entry
val ErrorRed      = Color(0xFFB53A2A)
val Surface       = Color(0xFFFBF7EE)
val CardBg        = Color(0xFFFFFFFF)
val DividerColor  = Color(0xFFE0D8C8)

// Rating colors (1=red, 5=yellow, 10=green)
fun ratingColor(rating: Int): Color = when {
    rating <= 3  -> Color(0xFFD95B3B)
    rating <= 5  -> Color(0xFFE8A930)
    rating <= 7  -> Color(0xFF8BBB5A)
    else         -> Color(0xFF3A9B6F)
}

// ── Fonts ────────────────────────────────────────────────────────────────────
val RalewayFamily = FontFamily(
    Font(R.font.raleway_regular, FontWeight.Normal),
    Font(R.font.raleway_semibold, FontWeight.SemiBold),
    Font(R.font.raleway_bold, FontWeight.Bold),
)

val NunitoFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

// ── Typography ───────────────────────────────────────────────────────────────
val GueteTagTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = RalewayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        color = DeepForest
    ),
    displayMedium = TextStyle(
        fontFamily = RalewayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        color = DeepForest
    ),
    headlineLarge = TextStyle(
        fontFamily = RalewayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = DeepForest
    ),
    headlineMedium = TextStyle(
        fontFamily = RalewayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        color = DeepForest
    ),
    headlineSmall = TextStyle(
        fontFamily = RalewayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = DeepForest
    ),
    titleLarge = TextStyle(
        fontFamily = RalewayFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = DeepForest
    ),
    titleMedium = TextStyle(
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = DeepForest
    ),
    bodyLarge = TextStyle(
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        color = SlateGray
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = SlateGray
    ),
    bodySmall = TextStyle(
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = SlateGray
    ),
    labelLarge = TextStyle(
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = DeepForest
    ),
    labelMedium = TextStyle(
        fontFamily = NunitoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = SlateGray
    ),
)

// ── Color Scheme ─────────────────────────────────────────────────────────────
val GueteTagColorScheme = lightColorScheme(
    primary          = SageGreen,
    onPrimary        = Color.White,
    primaryContainer = MossLight,
    onPrimaryContainer = DeepForest,
    secondary        = Terracotta,
    onSecondary      = Color.White,
    secondaryContainer = Color(0xFFFFDDD0),
    onSecondaryContainer = Color(0xFF4A1A0A),
    tertiary         = GoldAmber,
    background       = Chamois,
    onBackground     = DeepForest,
    surface          = Surface,
    onSurface        = DeepForest,
    surfaceVariant   = LightChamois,
    onSurfaceVariant = SlateGray,
    outline          = DividerColor,
    error            = ErrorRed,
)

// ── Theme Composable ──────────────────────────────────────────────────────────
@Composable
fun GueteTagTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GueteTagColorScheme,
        typography  = GueteTagTypography,
        content     = content
    )
}
