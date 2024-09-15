package com.example.myplants.data.data_source

import androidx.room.Database
import com.example.myplants.data.Plant

@Database(
    entities = [Plant::class],
    version = 1
)
abstract class PlantDatabase {
    abstract val plantDao: PlantDao

    companion object {
        const val DATABASE_NAME = "plant_db"
    }
}