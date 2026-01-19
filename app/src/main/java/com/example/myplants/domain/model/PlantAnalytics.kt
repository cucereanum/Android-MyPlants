package com.example.myplants.domain.model

import com.example.myplants.data.Plant


data class PlantAnalytics(
    val totalPlants: Int,
    val activePlants: Int,  // Plants that need water this week
    val totalWaterings: Int,

    val currentStreak: Int,
    val longestStreak: Int,

    val healthScore: Float,

    val wateredThisWeek: Int,
    val wateredThisMonth: Int,
    val missedWaterings: Int,

    val mostCaredForPlant: Plant? = null,
    val needsAttentionPlant: Plant? = null,

    val last30Days: List<DayData> = emptyList()
)
