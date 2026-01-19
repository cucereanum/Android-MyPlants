package com.example.myplants.domain.model

import java.time.LocalDate


data class StreakData(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastWateredDate: LocalDate?
)
