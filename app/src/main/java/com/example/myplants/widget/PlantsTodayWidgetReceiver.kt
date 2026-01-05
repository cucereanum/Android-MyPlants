package com.example.myplants.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver for the Plants Today Widget (4x2)
 * This is the entry point that Android uses to communicate with the widget
 */
class PlantsTodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PlantsTodayWidget()
}
