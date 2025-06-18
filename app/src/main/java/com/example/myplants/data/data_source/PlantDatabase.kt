package com.example.myplants.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myplants.data.NotificationEntity
import com.example.myplants.data.Plant
import com.example.myplants.data.converters.Converters

@Database(
    entities = [Plant::class, NotificationEntity::class],
    version = 5
)
@TypeConverters(Converters::class) // ✅ add this
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "plant_db"
    }
}