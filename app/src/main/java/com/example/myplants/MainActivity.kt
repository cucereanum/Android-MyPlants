package com.example.myplants

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myplants.navigation.Route
import com.example.myplants.ui.addEditPlant.AddEditPlantScreen
import com.example.myplants.ui.ble.BleScreen
import com.example.myplants.ui.notifications.NotificationScreen
import com.example.myplants.ui.plantDetails.PlantDetailsScreen
import com.example.myplants.ui.plantList.PlantListScreen
import com.example.myplants.ui.settings.SettingsScreen
import com.example.myplants.ui.theme.MyPlantsTheme
import dagger.hilt.android.AndroidEntryPoint

@RequiresApi(Build.VERSION_CODES.S)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyPlantsTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) {

                    NavHost(navController = navController, startDestination = Route.PLANT_LIST) {
                        composable(Route.PLANT_LIST) {
                            PlantListScreen(navController)
                        }
                        composable(Route.ADD_EDIT_PLANT) {
                            AddEditPlantScreen(
                                navController, plantId = -1,
                            )
                        }
                        composable(
                            "${Route.ADD_EDIT_PLANT}/{plantId}",
                            arguments = listOf(navArgument("plantId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            AddEditPlantScreen(
                                navController,
                                plantId = backStackEntry.arguments?.getInt("plantId") ?: -1,
                            )
                        }
                        composable(
                            Route.PLANT_DETAILS,
                            arguments = listOf(navArgument("plantId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val plantId = backStackEntry.arguments?.getInt("plantId") ?: 0
                            PlantDetailsScreen(navController, plantId)
                        }
                        composable(Route.NOTIFICATIONS) {
                            NotificationScreen(navController)
                        }
                        composable(Route.SETTINGS) {
                            SettingsScreen(navController)
                        }
                        composable(Route.BLE) {
                            BleScreen(
                                navController
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
