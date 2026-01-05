package com.example.myplants.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.myplants.MainActivity
import com.example.myplants.R

/**
 * Single Plant Widget (2x2)
 * Shows a single plant with quick water action
 */
class SinglePlantWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            SinglePlantWidgetContent(context)
        }
    }

    companion object {
        val PLANT_ID_KEY = intPreferencesKey("selected_plant_id")
        val PLANT_NAME_KEY =
            androidx.datastore.preferences.core.stringPreferencesKey("selected_plant_name")
        val PLANT_WATERED_KEY =
            androidx.datastore.preferences.core.booleanPreferencesKey("selected_plant_watered")
    }
}

@Composable
private fun SinglePlantWidgetContent(context: Context) {
    val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
    val plantId = prefs[SinglePlantWidget.PLANT_ID_KEY]
    val plantName = prefs[SinglePlantWidget.PLANT_NAME_KEY]
    val isWatered = prefs[SinglePlantWidget.PLANT_WATERED_KEY] ?: false

    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColorProviders.surface)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            if (plantId == null || plantName == null) {
                // No plant selected - show configuration prompt
                NoPlantSelectedContent(context)
            } else {
                // Show plant info
                PlantContent(
                    context = context,
                    plantId = plantId,
                    plantName = plantName,
                    isWatered = isWatered
                )
            }
        }
    }
}

@Composable
private fun NoPlantSelectedContent(context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🌱",
            style = TextStyle(fontSize = 32.sp)
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        Text(
            text = context.getString(R.string.widget_no_plant_selected),
            style = TextStyle(
                color = WidgetColorProviders.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = context.getString(R.string.widget_tap_to_configure),
            style = TextStyle(
                color = WidgetColorProviders.secondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
private fun PlantContent(
    context: Context,
    plantId: Int,
    plantName: String,
    isWatered: Boolean
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Plant emoji
        Text(
            text = "🌱",
            style = TextStyle(fontSize = 32.sp)
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Plant name
        Text(
            text = plantName,
            style = TextStyle(
                color = WidgetColorProviders.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Water button or watered status
        if (isWatered) {
            Text(
                text = context.getString(R.string.widget_watered),
                style = TextStyle(
                    color = WidgetColorProviders.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        } else {
            Box(
                modifier = GlanceModifier
                    .background(WidgetColorProviders.primary)
                    .cornerRadius(8.dp)
                    .clickable(
                        actionRunCallback<MarkAsWateredAction>(
                            actionParametersOf(plantIdKey to plantId)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "💧 ${context.getString(R.string.widget_water)}",
                    style = TextStyle(
                        color = WidgetColorProviders.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
