package com.example.myplants.widget

import android.content.Context
import androidx.room.Room
import com.example.myplants.data.data_source.PlantDatabase

/**
 * Helper object to manage database access for widgets
 * Provides a singleton database instance to avoid creating multiple connections
 */
object WidgetDatabaseHelper {

    @Volatile
    private var INSTANCE: PlantDatabase? = null

    /**
     * Get or create the database instance
     * This ensures only one database connection is created and reused
     */
    fun getDatabase(context: Context): PlantDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                PlantDatabase::class.java,
                PlantDatabase.DATABASE_NAME
            ).build()
            INSTANCE = instance
            instance
        }
    }

    /**
     * Close the database connection if it exists
     * Call this when you want to explicitly clean up resources
     */
    fun closeDatabase() {
        INSTANCE?.close()
        INSTANCE = null
    }
}
