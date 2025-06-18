package com.example.myplants.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val plantName: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)