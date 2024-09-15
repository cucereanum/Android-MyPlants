package com.example.myplants.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class Plant(
    val plantName: String,
    val imageUri: Uri,
    val time: LocalDateTime,
    val selectedDays: DayOfWeek,
    val waterAmount: String,
    val description: String,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)
