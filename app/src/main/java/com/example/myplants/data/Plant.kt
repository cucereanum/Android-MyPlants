package com.example.myplants.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Plant(
    val plantName: String,
    val imageUri: String,
    val time: Long,
    val selectedDays: DayOfWeek,
    val waterAmount: String,
    val description: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)
