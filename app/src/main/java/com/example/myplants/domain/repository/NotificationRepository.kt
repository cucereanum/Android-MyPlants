package com.example.myplants.domain.repository

import com.example.myplants.data.NotificationEntity
import com.example.myplants.data.NotificationType
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    suspend fun insertNotification(
        plantId: Int,
        plantName: String,
        message: String,
        type: NotificationType
    )

    fun getAllNotifications(): Flow<List<NotificationEntity>>
    suspend fun countTodayNotifications(plantId: Int, since: Long): Int
    suspend fun markAsReadByIds(ids: List<Int>)
    fun hasUnreadNotificationsFlow(): Flow<Boolean>
}