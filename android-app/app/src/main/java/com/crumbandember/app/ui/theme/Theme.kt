package com.crumbandember.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = EmberOrange,
    onPrimary = Cream,
    secondary = CrustBrown,
    background = Cream,
    surface = Butter,
    error = ErrorRed,
    onBackground = Espresso,
    onSurface = Espresso
)

private val DarkColors = darkColorScheme(
    primary = EmberOrange,
    onPrimary = Espresso,
    secondary = Butter,
    background = Espresso,
    surface = CrustBrown,
    error = ErrorRed
)

@Composable
fun CrumbAndEmberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = BakeryTypography,
        content = content
    )
}
