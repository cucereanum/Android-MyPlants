package com.example.myplants.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

/**
 * Color scheme for widgets - simplified for widget compatibility
 */
object WidgetColors {
    val Primary = Color(0xFF4CAF50)        // Green
    val PrimaryDark = Color(0xFF81C784)    // Light green for dark mode
    val OnPrimary = Color.White

    val Surface = Color.White
    val SurfaceDark = Color(0xFF1E1E1E)

    val OnSurface = Color(0xFF1C1B1F)
    val OnSurfaceDark = Color(0xFFE6E1E5)

    val Secondary = Color(0xFF666666)
    val SecondaryDark = Color(0xFFAAAAAA)

    val Error = Color(0xFFBA1A1A)
    val Warning = Color(0xFFFFA000)
    val Success = Color(0xFF4CAF50)
}

/**
 * Color providers that handle light/dark mode automatically
 */
object WidgetColorProviders {
    val primary = ColorProvider(
        day = WidgetColors.Primary,
        night = WidgetColors.PrimaryDark
    )

    val onPrimary = ColorProvider(
        day = WidgetColors.OnPrimary,
        night = WidgetColors.OnPrimary
    )

    val surface = ColorProvider(
        day = WidgetColors.Surface,
        night = WidgetColors.SurfaceDark
    )

    val onSurface = ColorProvider(
        day = WidgetColors.OnSurface,
        night = WidgetColors.OnSurfaceDark
    )

    val secondary = ColorProvider(
        day = WidgetColors.Secondary,
        night = WidgetColors.SecondaryDark
    )
}
