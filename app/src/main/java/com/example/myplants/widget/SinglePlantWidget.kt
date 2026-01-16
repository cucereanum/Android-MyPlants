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
import com.example.myplants.data.DayOfWeek
import kotlinx.coroutines.runBlocking

/**
 * Single Plant Widget (2x2)
 * Shows a single plant with quick water action
 */
class SinglePlantWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val plantId = prefs[PLANT_ID_KEY]

            val plantData = plantId?.let {
                runBlocking {
                    getPlantData(context, it)
                }
            }

            SinglePlantWidgetContent(context, plantData)
        }
    }

    companion object {
        val PLANT_ID_KEY = intPreferencesKey("selected_plant_id")
    }

    private suspend fun getPlantData(context: Context, plantId: Int): SinglePlantWidgetData? {
        val database = WidgetDatabaseHelper.getDatabase(context)
        return try {
            val plant = database.plantDao().getPlantById(plantId) ?: return null
            val today = DayOfWeek.today()
            val needsWaterToday = plant.selectedDays.contains(today)

            SinglePlantWidgetData(
                id = plant.id,
                name = plant.plantName,
                isWatered = plant.isWatered,
                needsWaterToday = needsWaterToday
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class SinglePlantWidgetData(
    val id: Int, val name: String, val isWatered: Boolean, val needsWaterToday: Boolean
)

@Composable
private fun SinglePlantWidgetContent(context: Context, plantData: SinglePlantWidgetData?) {
    GlanceTheme {
        Box(
            modifier = GlanceModifier.fillMaxSize().background(WidgetColorProviders.surface)
                .cornerRadius(16.dp).clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            if (plantData == null) {
                NoPlantSelectedContent(context)
            } else {
                PlantContent(
                    context = context, plantData = plantData
                )
            }
        }
    }
}

@Composable
private fun NoPlantSelectedContent(context: Context) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🌱", style = TextStyle(fontSize = 32.sp)
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        Text(
            text = context.getString(R.string.widget_no_plant_selected), style = TextStyle(
                color = WidgetColorProviders.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )

    }
}

@Composable
private fun PlantContent(
    context: Context, plantData: SinglePlantWidgetData
) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🌱", style = TextStyle(fontSize = 32.sp)
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = plantData.name, style = TextStyle(
                color = WidgetColorProviders.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ), maxLines = 1
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (plantData.needsWaterToday) {
            if (plantData.isWatered) {
                Text(
                    text = context.getString(R.string.widget_watered), style = TextStyle(
                        color = WidgetColorProviders.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            } else {
                Box(
                    modifier = GlanceModifier.background(WidgetColorProviders.primary)
                        .cornerRadius(8.dp).clickable(
                            actionRunCallback<MarkAsWateredAction>(
                                actionParametersOf(plantIdKey to plantData.id)
                            )
                        ).padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "💧 ${context.getString(R.string.widget_water)}", style = TextStyle(
                            color = WidgetColorProviders.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✓", style = TextStyle(
                        color = WidgetColorProviders.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = context.getString(R.string.widget_no_need_water_today),
                    style = TextStyle(
                        color = WidgetColorProviders.secondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 2
                )
            }
        }
    }
}
