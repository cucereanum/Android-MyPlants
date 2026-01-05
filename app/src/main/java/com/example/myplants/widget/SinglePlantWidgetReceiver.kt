package com.example.myplants.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver for the Single Plant Widget (2x2)
 * This is the entry point that Android uses to communicate with the widget
 */
class SinglePlantWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SinglePlantWidget()
}
