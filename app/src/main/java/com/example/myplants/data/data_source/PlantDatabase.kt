package com.example.myplants.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myplants.data.Plant

@Database(
    entities = [Plant::class],
    version = 2
)
abstract class PlantDatabase : RoomDatabase() {
    abstract val plantDao: PlantDao

    companion object {
        const val DATABASE_NAME = "plant_db"
    }
}