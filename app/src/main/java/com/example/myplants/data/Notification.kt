package com.example.myplants.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class NotificationType {
    UPCOMING, FORGOT
}

@Immutable
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["plantId"]),
        Index(value = ["isRead"]),
        Index(value = ["timestamp"]),
        Index(value = ["type"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    val plantName: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: NotificationType
)