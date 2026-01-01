package com.example.myplants.presentation.theme

import androidx.compose.ui.graphics.Color


object MyPlantsPalette {

    object Brand {
        val teal600 = Color(0xFF0A6375)
        val teal400 = Color(0xFF2FA8B6)
        val teal900 = Color(0xFF001F24)
    }

    object Neutral {
        val n900 = Color(0xFF232926)
        val n500 = Color(0xFF516370)
        val n300 = Color(0xFFAFB3B7)
        val n100 = Color(0xFFF9F9F9)
    }

    object Light {
        val background = Neutral.n100
        val surface = Color(0xFFF5F9F3)
        val onSurface = Neutral.n900
        val onBackground = Neutral.n900
        val outline = Color(0xFFCED7D2)
    }

    object Dark {
        val background = Color(0xFF0F1413)
        val surface = Color(0xFF141C1B)
        val surfaceVariant = Color(0xFF1C2624)

        val onSurface = Color(0xFFE6EEE9)
        val onSurfaceVariant = Color(0xFFB7C4BF)

        val outline = Color(0xFF4A5A57)
    }
}
