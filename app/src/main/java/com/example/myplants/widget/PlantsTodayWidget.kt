package com.example.myplants.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
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
import com.example.myplants.data.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class PlantsTodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val updateTimestamp = currentState(key = LAST_UPDATE_KEY) ?: 0L

            val plants = remember(updateTimestamp) {
                runBlocking {
                    getPlantsNeedingWaterToday(context)
                }
            }

            PlantsTodayWidgetContent(context, plants)
        }
    }

    companion object {
        val LAST_UPDATE_KEY = longPreferencesKey("last_update_timestamp")
    }

    private suspend fun getPlantsNeedingWaterToday(context: Context): List<WidgetPlantData> {
        val database = WidgetDatabaseHelper.getDatabase(context)

        return try {
            val allPlants = database.plantDao().getPlants().first()
            val today = DayOfWeek.today()

            allPlants
                .filter { plant -> plant.selectedDays.contains(today) }
                .map { plant ->
                    WidgetPlantData(
                        id = plant.id,
                        name = plant.plantName,
                        isWatered = plant.isWatered
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Composable
private fun PlantsTodayWidgetContent(context: Context, plants: List<WidgetPlantData>) {
    val plantsNeedingWater = plants.filter { !it.isWatered }

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

                if (plantsNeedingWater.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = GlanceModifier.padding(16.dp)
                        ) {
                            Text(
                                text = "✓",
                                style = TextStyle(
                                    color = WidgetColorProviders.primary,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(8.dp))
                            Text(
                                text = if (plants.isEmpty()) {
                                    context.getString(R.string.widget_no_plants_today)
                                } else {
                                    context.getString(R.string.widget_all_watered)
                                },
                                style = TextStyle(
                                    color = WidgetColorProviders.secondary,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn {
                        items(plantsNeedingWater, itemId = { it.id.toLong() }) { plant ->
                            PlantRowItem(context = context, plant = plant)
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
        val database = WidgetDatabaseHelper.getDatabase(context)

        try {
            val plant = database.plantDao().getPlantById(plantId) ?: return
            
            // Save watering event to history for analytics
            database.wateringHistoryDao().insertWateringEvent(
                com.example.myplants.data.WateringHistory(
                    plantId = plant.id,
                    plantName = plant.plantName,
                    wateredAt = System.currentTimeMillis()
                )
            )
            
            // Update plant watered status
            database.plantDao().updatePlant(plant.copy(isWatered = true))
            kotlinx.coroutines.delay(50)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[PlantsTodayWidget.LAST_UPDATE_KEY] = System.currentTimeMillis()
        }

        PlantsTodayWidget().update(context, glanceId)
    }
}
