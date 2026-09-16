package com.crumbandember.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = EmberOrange,
    onPrimary = Cream,
    primaryContainer = Butter,
    onPrimaryContainer = Espresso,
    secondary = CrustBrown,
    onSecondary = Cream,
    secondaryContainer = SageGreenSoft,
    onSecondaryContainer = Espresso,
    tertiary = SageGreen,
    background = Cream,
    onBackground = Espresso,
    surface = Color(0xFFFFFFFF),
    onSurface = Espresso,
    surfaceVariant = Butter,
    onSurfaceVariant = CrustBrown,
    error = ErrorRed,
    outline = Caramel
)

private val DarkColors = darkColorScheme(
    primary = EmberOrangeLight,
    onPrimary = EspressoDeep,
    primaryContainer = CrustBrown,
    onPrimaryContainer = Butter,
    secondary = GoldShimmer,
    onSecondary = EspressoDeep,
    secondaryContainer = Color(0xFF4A2F1C),
    onSecondaryContainer = Butter,
    tertiary = SageGreen,
    background = EspressoDeep,
    onBackground = Cream,
    surface = Espresso,
    onSurface = Cream,
    surfaceVariant = Color(0xFF4A2F1C),
    onSurfaceVariant = OnDarkMuted,
    error = Color(0xFFFF8A80),
    outline = Caramel
)

/**
 * Extra design tokens Material3's ColorScheme doesn't model: gradients,
 * shadow tints and shimmer accents used by the 3D hero carousel, product
 * cards and branded illustrations. Kept separate from ColorScheme so screens
 * opt in explicitly (`BakeryTokens.current`) instead of it leaking into
 * every default Material component.
 */
data class BakeryColorTokens(
    val heroGradient: Brush,
    val cardGradient: Brush,
    val shimmer: Color,
    val shadow: Color,
    val success: Color
)

private val LightTokens = BakeryColorTokens(
    heroGradient = Brush.linearGradient(listOf(EspressoDeep, Espresso, CrustBrown)),
    cardGradient = Brush.linearGradient(listOf(Butter, Cream)),
    shimmer = GoldShimmer,
    shadow = CardShadow,
    success = SuccessGreen
)

private val DarkTokens = BakeryColorTokens(
    heroGradient = Brush.linearGradient(listOf(Color(0xFF120901), EspressoDeep, Color(0xFF3A2213))),
    cardGradient = Brush.linearGradient(listOf(Color(0xFF4A2F1C), Espresso)),
    shimmer = GoldShimmer,
    shadow = Color(0x66000000),
    success = SuccessGreen
)

val LocalBakeryTokens = staticCompositionLocalOf { LightTokens }

object BakeryTokens {
    val current: BakeryColorTokens
        @Composable get() = LocalBakeryTokens.current
}

@Composable
fun CrumbAndEmberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val tokens = if (darkTheme) DarkTokens else LightTokens
    CompositionLocalProvider(LocalBakeryTokens provides tokens) {
        MaterialTheme(
            colorScheme = colors,
            typography = BakeryTypography,
            content = content
        )
    }
}
