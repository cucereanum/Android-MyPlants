package com.example.myplants.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SinglePlantWidgetPinReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("widget_pending", Context.MODE_PRIVATE)
        val plantId = prefs.getInt("pending_plant_id", -1)

        if (plantId == -1) return

        prefs.edit().remove("pending_plant_id").apply()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                delay(300)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetProvider = ComponentName(context, SinglePlantWidgetReceiver::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider)

                if (appWidgetIds.isNotEmpty()) {
                    val newWidgetId = appWidgetIds.last()
                    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(newWidgetId)

                    updateAppWidgetState(context, glanceId) { widgetPrefs ->
                        widgetPrefs[SinglePlantWidget.PLANT_ID_KEY] = plantId
                    }

                    SinglePlantWidget().update(context, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
