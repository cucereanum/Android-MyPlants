package com.example.myplants.data.data_source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myplants.data.ConnectedBleDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BleDeviceDao {
    @Query("SELECT * FROM connected_ble_devices")
    fun getAllDevices(): Flow<List<ConnectedBleDeviceEntity>>

    @Query("SELECT * FROM connected_ble_devices WHERE plantId = :plantId")
    suspend fun getDeviceByPlantId(plantId: Int): ConnectedBleDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: ConnectedBleDeviceEntity)

    @Query("DELETE FROM connected_ble_devices WHERE deviceId = :deviceId")
    suspend fun deleteById(deviceId: String)

    @Query("DELETE FROM connected_ble_devices WHERE plantId = :plantId")
    suspend fun deleteByPlantId(plantId: Int)
}