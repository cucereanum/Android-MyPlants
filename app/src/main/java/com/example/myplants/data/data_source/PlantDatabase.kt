package com.example.myplants.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myplants.data.ConnectedBleDeviceEntity
import com.example.myplants.data.NotificationEntity
import com.example.myplants.data.Plant
import com.example.myplants.data.converters.Converters
import com.example.myplants.data.converters.NotificationConverter

@Database(
    entities = [Plant::class, NotificationEntity::class, ConnectedBleDeviceEntity::class],
    version = 9
)
@TypeConverters(value = [Converters::class, NotificationConverter::class])
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun notificationDao(): NotificationDao
    abstract fun bleDeviceDao(): BleDeviceDao

    companion object {
        const val DATABASE_NAME = "plant_db"
    }
}