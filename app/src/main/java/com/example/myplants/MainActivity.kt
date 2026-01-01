package com.example.myplants

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myplants.navigation.Route
import com.example.myplants.presentation.addEditPlant.AddEditPlantScreen
import com.example.myplants.presentation.ble.BleScreen
import com.example.myplants.presentation.deeplink.DeepLinkViewModel
import com.example.myplants.presentation.notifications.NotificationScreen
import com.example.myplants.presentation.plantDetails.PlantDetailsScreen
import com.example.myplants.presentation.plantList.PlantListScreen
import com.example.myplants.presentation.settings.SettingsScreen
import com.example.myplants.presentation.settings.appearance.SettingsAppearanceScreen
import com.example.myplants.presentation.settings.appearance.SettingsAppearanceViewModel
import com.example.myplants.presentation.settings.language.SettingsLanguageScreen
import com.example.myplants.presentation.settings.notifications.SettingsNotificationsScreen
import com.example.myplants.presentation.settings.notifications.SettingsNotificationsViewModel
import com.example.myplants.presentation.theme.MyPlantsTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myplants.presentation.bleLink.BleLinkViewModel

@RequiresApi(Build.VERSION_CODES.S)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {


    private val deepLinkVm: DeepLinkViewModel by viewModels()

    private fun handleIntent(i: Intent?) {
        if (i?.getBooleanExtra("fromNotification", false) == true) {
            val id = i.getIntExtra("plantId", -1)
            if (id != -1) deepLinkVm.open(id)
        }
    }


    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearanceViewModel: SettingsAppearanceViewModel = hiltViewModel()
            val isDarkModeEnabled = appearanceViewModel.isDarkModeEnabled.collectAsState().value

            val view = LocalView.current
            SideEffect {
                if (view.isInEditMode) return@SideEffect

                val activity = view.context as? Activity ?: return@SideEffect
                val window = activity.window
                window.statusBarColor = android.graphics.Color.TRANSPARENT

                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkModeEnabled
            }

            MyPlantsTheme(darkTheme = isDarkModeEnabled) {
                val navController = rememberNavController()
                // Listen for plant open events and navigate
                LaunchedEffect(Unit) {
                    deepLinkVm.openPlant.collect { plantId ->
                        // navigate to the route that expects an id
                        navController.navigate("${Route.ADD_EDIT_PLANT}/$plantId") {
                            launchSingleTop = true
                        }
                    }
                }
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
                        composable(Route.SETTINGS_LANGUAGE) {
                            SettingsLanguageScreen(navController)
                        }
                        composable(Route.SETTINGS_APPEARANCE) {
                            SettingsAppearanceScreen(navController)
                        }
                        composable(Route.SETTINGS_NOTIFICATIONS) {
                            val viewModel: SettingsNotificationsViewModel = hiltViewModel()
                            SettingsNotificationsScreen(navController, viewModel)
                        }
                        composable(Route.BLE) {
                            BleScreen(
                                navController
                            )
                        }
                        composable(
                            Route.BLE_LINK,
                            arguments = listOf(navArgument("plantId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val plantId = backStackEntry.arguments?.getInt("plantId") ?: 0
                            val viewModel: BleLinkViewModel = hiltViewModel()
                            BleScreen(
                                navController = navController,
                                onClose = { navController.popBackStack() },
                                onDeviceSelected = { device ->
                                    viewModel.linkDeviceToPlant(plantId = plantId, device = device)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
        handleIntent(intent)

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // Handle intents when Activity already exists (foreground case)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

}
