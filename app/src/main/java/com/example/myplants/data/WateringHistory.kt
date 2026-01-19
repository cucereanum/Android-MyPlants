package com.example.myplants.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "watering_history",
    foreignKeys = [
        ForeignKey(
            entity = Plant::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plantId"]),
        Index(value = ["wateredAt"])
    ]
)
data class WateringHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val plantId: Int,
    val plantName: String,
    val wateredAt: Long
)
