package com.example.myplants.data.data_source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.myplants.data.WateringHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface WateringHistoryDao {

    @Insert
    suspend fun insertWateringEvent(event: WateringHistory)

    @Query("SELECT * FROM watering_history ORDER BY wateredAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<WateringHistory>>


    @Query(
        """
        SELECT * FROM watering_history 
        WHERE wateredAt >= :startTime 
        ORDER BY wateredAt DESC
    """
    )
    fun getHistorySince(startTime: Long): Flow<List<WateringHistory>>


    @Query(
        """
        SELECT * FROM watering_history 
        WHERE plantId = :plantId 
        ORDER BY wateredAt DESC
    """
    )
    fun getHistoryForPlant(plantId: Int): Flow<List<WateringHistory>>


    @Query(
        """
        SELECT COUNT(DISTINCT DATE(wateredAt / 1000, 'unixepoch')) as days
        FROM watering_history
        WHERE wateredAt >= :since
    """
    )
    suspend fun getDaysWateredSince(since: Long): Int


    @Query("SELECT * FROM watering_history ORDER BY wateredAt DESC")
    fun getAllHistory(): Flow<List<WateringHistory>>


    @Query("DELETE FROM watering_history")
    suspend fun deleteAllHistory()
}
