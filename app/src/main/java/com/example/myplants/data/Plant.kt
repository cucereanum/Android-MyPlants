package com.example.myplants.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    indices = [
        Index(value = ["isWatered"]),
        Index(value = ["time"]),
        Index(value = ["id"])
    ]
)
data class Plant(
    val plantName: String,
    val imageUri: String,
    val time: Long,
    val selectedDays: List<DayOfWeek>,
    val waterAmount: String,
    val size: String,
    val description: String,
    val isWatered: Boolean = false,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)
