package com.example.myplants.ui.plantList

import com.example.myplants.ui.util.FilterType


enum class PlantListFilter(override val displayName: String) : FilterType {
    UPCOMING("Upcoming"),
    FORGOT_TO_WATER("Forgot to Water"),
    HISTORY("History")
}
