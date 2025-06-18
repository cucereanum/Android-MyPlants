package com.example.myplants.data.data_source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplants.data.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE plantId = :plantId AND timestamp > :since")
    suspend fun countNotificationsToday(plantId: Int, since: Long): Int

    @Query("UPDATE notifications SET isRead = 1 WHERE id IN (:ids) AND isRead = 0")
    suspend fun markAsReadByIds(ids: List<Int>)
}