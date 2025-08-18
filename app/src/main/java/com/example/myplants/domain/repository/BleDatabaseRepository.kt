package com.example.myplants.domain.repository

import com.example.myplants.data.ConnectedBleDeviceEntity
import com.example.myplants.data.ble.BleDevice
import kotlinx.coroutines.flow.Flow

interface BleDatabaseRepository {
    fun getLinkedDevices(): Flow<List<ConnectedBleDeviceEntity>>
    suspend fun linkDeviceToPlant(plantId: Int, device: BleDevice)
    suspend fun forgetDevice(deviceId: String)
    suspend fun forgetDeviceByPlant(plantId: Int)
    suspend fun getDeviceByPlantId(plantId: Int): ConnectedBleDeviceEntity?
    suspend fun getLiveConnectedDevices(): List<BleDevice> // from manager
}