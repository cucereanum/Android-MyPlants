package com.example.myplants.domain.model

import java.time.LocalDate


data class DayData(
    val date: LocalDate,
    val plantsWatered: Int,
    val totalPlantsDue: Int
)
