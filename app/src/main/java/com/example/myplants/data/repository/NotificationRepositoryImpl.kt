package com.example.myplants.data.repository

import com.example.myplants.data.NotificationEntity
import com.example.myplants.data.NotificationType
import com.example.myplants.data.data_source.NotificationDao
import com.example.myplants.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationRepositoryImpl(
    private val dao: NotificationDao
) : NotificationRepository {

    override suspend fun insertNotification(
        plantId: Int,
        plantName: String,
        message: String,
        type: NotificationType
    ) {
        val notification = NotificationEntity(
            plantId = plantId,
            plantName = plantName,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        dao.insert(notification)
    }

    override fun getAllNotifications(): Flow<List<NotificationEntity>> {
        return dao.getAll()
    }

    override suspend fun countTodayNotifications(plantId: Int, since: Long): Int {
        return dao.countNotificationsToday(plantId, since)
    }

    override suspend fun markAsReadByIds(ids: List<Int>) {
        return dao.markAsReadByIds(ids)
    }

    override fun hasUnreadNotificationsFlow(): Flow<Boolean> = dao
        .observeUnreadNotifications()
        .map { notifications -> notifications.isNotEmpty() }
}