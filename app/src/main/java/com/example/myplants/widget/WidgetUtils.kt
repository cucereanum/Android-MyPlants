package com.example.myplants.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.edit


object WidgetUtils {


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
     * @param plantId The ID of the plant to display in the widget
     * @return true if the request was made successfully, false otherwise
     */
    suspend fun requestPinSinglePlantWidget(context: Context, plantId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            return false
        }

        val widgetProvider = ComponentName(context, SinglePlantWidgetReceiver::class.java)

        val prefs = context.getSharedPreferences("widget_pending", Context.MODE_PRIVATE)
        prefs.edit { putInt("pending_plant_id", plantId) }

        val callbackIntent = Intent(context, SinglePlantWidgetPinReceiver::class.java)

        val successCallback = android.app.PendingIntent.getBroadcast(
            context,
            plantId,
            callbackIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
        )

        return appWidgetManager.requestPinAppWidget(widgetProvider, null, successCallback)
    }

    private fun requestPinWidget(
        context: Context,
        receiverClass: Class<*>
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)

        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            return false
        }

        val widgetProvider = ComponentName(context, receiverClass)

        return appWidgetManager.requestPinAppWidget(widgetProvider, null, null)
    }


    fun isWidgetPinningSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }
        return AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    }


    suspend fun updateAllPlantsTodayWidgets(context: Context) {
        withContext(Dispatchers.Main) {
            val widget = PlantsTodayWidget()
            widget.updateAll(context)
        }
    }


    suspend fun updateAllSinglePlantWidgets(context: Context) {
        withContext(Dispatchers.Main) {
            val widget = SinglePlantWidget()
            widget.updateAll(context)
        }
    }

  
    suspend fun updateAllWidgets(context: Context) {
        updateAllPlantsTodayWidgets(context)
        updateAllSinglePlantWidgets(context)
    }
}
