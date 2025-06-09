package com.example.myplants.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myplants.data.NotificationEntity
import com.example.myplants.data.Plant

@Database(
    entities = [Plant::class, NotificationEntity::class],
    version = 4
)
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "plant_db"
    }
}