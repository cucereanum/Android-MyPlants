package com.example.myplants.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.myplants.MainActivity
import com.example.myplants.R

/**
 * Plants Today Widget (4x2)
 * Shows a list of plants that need watering today
 */
class PlantsTodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            PlantsTodayWidgetContent(context)
        }
    }
}

@Composable
private fun PlantsTodayWidgetContent(context: Context) {
    // For now, using hardcoded data to verify the widget works
    // We'll connect to the database in the next step
    val plants = listOf(
        WidgetPlantData(1, "Monstera", false),
        WidgetPlantData(2, "Pothos", true),
        WidgetPlantData(3, "Snake Plant", false)
    )

    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColorProviders.surface)
                .cornerRadius(16.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = context.getString(R.string.widget_plants_today_title),
                        style = TextStyle(
                            color = WidgetColorProviders.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Divider
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(WidgetColorProviders.secondary)
                ) {}

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (plants.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.widget_no_plants),
                            style = TextStyle(
                                color = WidgetColorProviders.secondary,
                                fontSize = 14.sp
                            )
                        )
                    }
                } else {
                    // Plant list
                    LazyColumn {
                        items(plants, itemId = { it.id.toLong() }) { plant ->
                            PlantRowItem(
                                context = context,
                                plant = plant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantRowItem(
    context: Context,
    plant: WidgetPlantData
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Plant name with emoji
        Text(
            text = "🪴 ${plant.name}",
            style = TextStyle(
                color = WidgetColorProviders.onSurface,
                fontSize = 14.sp
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Water button or watered status
        if (plant.isWatered) {
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
                    .cornerRadius(6.dp)
                    .clickable(
                        actionRunCallback<MarkAsWateredAction>(
                            actionParametersOf(plantIdKey to plant.id)
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
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

/**
 * Data class for widget plant display
 */
data class WidgetPlantData(
    val id: Int,
    val name: String,
    val isWatered: Boolean
)

/**
 * Action parameter key for plant ID
 */
val plantIdKey = ActionParameters.Key<Int>("plant_id")

/**
 * Action callback for marking a plant as watered
 */
class MarkAsWateredAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val plantId = parameters[plantIdKey] ?: return

        // TODO: Connect to repository to update plant
        // For now, just update the widget
        PlantsTodayWidget().update(context, glanceId)
    }
}
