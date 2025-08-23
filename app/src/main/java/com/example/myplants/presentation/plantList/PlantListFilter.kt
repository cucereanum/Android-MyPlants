package com.example.myplants.presentation.plantList

import com.example.myplants.presentation.util.FilterType


enum class PlantListFilter(override val displayName: String) : FilterType {
    UPCOMING("Upcoming"),
    FORGOT_TO_WATER("Forgot to Water"),
    HISTORY("History")
}
