package com.example.myplants.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MyPlantsPalette.Brand.teal400,
    onPrimary = Color.White,
    primaryContainer = MyPlantsPalette.Brand.teal600,
    onPrimaryContainer = Color.White,
    secondary = MyPlantsPalette.Neutral.n300,
    onSecondary = MyPlantsPalette.Dark.onSurface,
    secondaryContainer = MyPlantsPalette.Dark.surfaceVariant,
    onSecondaryContainer = MyPlantsPalette.Dark.onSurface,
    tertiary = MyPlantsPalette.Brand.teal400,
    onTertiary = Color.White,
    tertiaryContainer = MyPlantsPalette.Brand.teal600,
    onTertiaryContainer = Color.White,
    background = MyPlantsPalette.Dark.background,
    onBackground = MyPlantsPalette.Dark.onSurface,
    surface = MyPlantsPalette.Dark.surface,
    onSurface = MyPlantsPalette.Dark.onSurface,
    surfaceVariant = MyPlantsPalette.Dark.surfaceVariant,
    onSurfaceVariant = MyPlantsPalette.Dark.onSurfaceVariant,
    outline = MyPlantsPalette.Dark.outline,
)

private val LightColorScheme = lightColorScheme(
    primary = MyPlantsPalette.Brand.teal600,
    onPrimary = Color.White,
    primaryContainer = MyPlantsPalette.Brand.teal400,
    onPrimaryContainer = MyPlantsPalette.Brand.teal900,
    secondary = MyPlantsPalette.Neutral.n500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9EFEA),
    onSecondaryContainer = MyPlantsPalette.Neutral.n900,
    tertiary = MyPlantsPalette.Brand.teal400,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBFE7EA),
    onTertiaryContainer = MyPlantsPalette.Brand.teal900,
    background = MyPlantsPalette.Light.background,
    onBackground = MyPlantsPalette.Light.onBackground,
    surface = MyPlantsPalette.Light.surface,
    onSurface = MyPlantsPalette.Light.onSurface,
    surfaceVariant = Color(0xFFE9EFEA),
    onSurfaceVariant = Color(0xFF2F3A35),
    outline = MyPlantsPalette.Light.outline,
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MyPlantsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}