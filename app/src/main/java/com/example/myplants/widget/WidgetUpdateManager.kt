package com.example.myplants.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages widget updates across the app
 * Call these methods whenever plant data changes to keep widgets in sync
 */
object WidgetUpdateManager {

    /**
     * Update all Plants Today widgets (async)
     * Call this when:
     * - A plant is added/deleted
     * - A plant's watering schedule changes
     * - A plant is marked as watered/unwatered
     */
    fun updatePlantsTodayWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            updatePlantsTodayWidgetsSync(context)
        }
    }

    /**
     * Update all Plants Today widgets synchronously
     * Use this when already in a coroutine
     */
    suspend fun updatePlantsTodayWidgetsSync(context: Context) = withContext(Dispatchers.IO) {
        try {
            val glanceManager = GlanceAppWidgetManager(context)
            val glanceIds = glanceManager.getGlanceIds(PlantsTodayWidget::class.java)

            glanceIds.forEach { glanceId ->
                try {
                    PlantsTodayWidget().update(context, glanceId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            // Handle error silently - widget updates are not critical
            e.printStackTrace()
        }
    }

    /**
     * Update all Single Plant widgets (async)
     * Call this when a plant's data changes (name, watered status, etc.)
     */
    fun updateSinglePlantWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            updateSinglePlantWidgetsSync(context)
        }
    }

    /**
     * Update all Single Plant widgets synchronously
     * Use this when already in a coroutine
     */
    suspend fun updateSinglePlantWidgetsSync(context: Context) = withContext(Dispatchers.IO) {
        try {
            val glanceManager = GlanceAppWidgetManager(context)
            val glanceIds = glanceManager.getGlanceIds(SinglePlantWidget::class.java)

            glanceIds.forEach { glanceId ->
                try {
                    SinglePlantWidget().update(context, glanceId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            // Handle error silently - widget updates are not critical
            e.printStackTrace()
        }
    }

    /**
     * Update all widgets (both types) - async
     * Call this for major data changes
     */
    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            updateAllWidgetsSync(context)
        }
    }

    /**
     * Update all widgets synchronously
     * Use this when already in a coroutine
     */
    suspend fun updateAllWidgetsSync(context: Context) {
        updatePlantsTodayWidgetsSync(context)
        updateSinglePlantWidgetsSync(context)
    }
}
