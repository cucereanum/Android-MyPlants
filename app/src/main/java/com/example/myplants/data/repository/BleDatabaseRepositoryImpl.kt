package com.example.myplants.data.repository

import com.example.myplants.data.ConnectedBleDeviceEntity
import com.example.myplants.data.ble.BleDevice
import com.example.myplants.data.data_source.BleDeviceDao
import com.example.myplants.domain.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BleDatabaseRepositoryImpl @Inject constructor(
    private val dao: BleDeviceDao,
) : BleRepository {

    override fun getLinkedDevices(): Flow<List<ConnectedBleDeviceEntity>> {
        return dao.getAllDevices()
    }

    override suspend fun linkDeviceToPlant(plantId: Int, device: BleDevice) {
        dao.insert(
            ConnectedBleDeviceEntity(
                deviceId = device.id.toString(),
                plantId = plantId,
                name = device.name,
                lastConnected = System.currentTimeMillis()
            )
        )
    }

    override suspend fun forgetDevice(deviceId: String) {
        dao.deleteById(deviceId)
    }

    override suspend fun forgetDeviceByPlant(plantId: Int) {
        dao.deleteByPlantId(plantId)
    }

    override suspend fun getDeviceByPlantId(plantId: Int): ConnectedBleDeviceEntity? {
        return dao.getDeviceByPlantId(plantId)
    }

    override suspend fun getLiveConnectedDevices(): List<BleDevice> {
        return emptyList()
    }
}