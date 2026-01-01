package com.example.myplants.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MyPlantsPalette.Brand.teal600,
    onPrimary = Color.White,
    secondary = MyPlantsPalette.Neutral.n300,
    onSecondary = MyPlantsPalette.Dark.onSurface,
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
    secondary = MyPlantsPalette.Neutral.n500,
    onSecondary = MyPlantsPalette.Neutral.n900,
    background = MyPlantsPalette.Light.background,
    onBackground = MyPlantsPalette.Light.onBackground,
    surface = MyPlantsPalette.Light.surface,
    onSurface = MyPlantsPalette.Light.onSurface,
    surfaceVariant = Color(0xFFE9EFEA),
    onSurfaceVariant = Color(0xFF2F3A35),
    outline = MyPlantsPalette.Light.outline,
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun MyPlantsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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