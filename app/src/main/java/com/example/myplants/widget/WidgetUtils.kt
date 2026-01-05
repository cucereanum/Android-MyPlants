package com.example.myplants.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility object for widget-related operations
 */
object WidgetUtils {

    /**
     * Request to pin the Plants Today Widget to the home screen
     * Only works on Android 8.0 (API 26) and above
     *
     * @return true if the request was made successfully, false otherwise
     */
    fun requestPinPlantsTodayWidget(context: Context): Boolean {
        return requestPinWidget(
            context = context,
            receiverClass = PlantsTodayWidgetReceiver::class.java
        )
    }

    /**
     * Request to pin the Single Plant Widget to the home screen
     * Only works on Android 8.0 (API 26) and above
     *
     * @return true if the request was made successfully, false otherwise
     */
    fun requestPinSinglePlantWidget(context: Context): Boolean {
        return requestPinWidget(
            context = context,
            receiverClass = SinglePlantWidgetReceiver::class.java
        )
    }

    /**
     * Generic function to request pinning a widget
     */
    private fun requestPinWidget(
        context: Context,
        receiverClass: Class<*>
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Widget pinning not supported below Android 8.0
            return false
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Check if the launcher supports widget pinning
        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            return false
        }

        val widgetProvider = ComponentName(context, receiverClass)

        // Request to pin the widget
        return appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
    }

    /**
     * Check if widget pinning is supported on this device
     */
    fun isWidgetPinningSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }
        return AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    }

    /**
     * Update all Plants Today widgets
     */
    suspend fun updateAllPlantsTodayWidgets(context: Context) {
        withContext(Dispatchers.Main) {
            val widget = PlantsTodayWidget()
            widget.updateAll(context)
        }
    }

    /**
     * Update all Single Plant widgets
     */
    suspend fun updateAllSinglePlantWidgets(context: Context) {
        withContext(Dispatchers.Main) {
            val widget = SinglePlantWidget()
            widget.updateAll(context)
        }
    }

    /**
     * Update all widgets (both types)
     */
    suspend fun updateAllWidgets(context: Context) {
        updateAllPlantsTodayWidgets(context)
        updateAllSinglePlantWidgets(context)
    }
}
