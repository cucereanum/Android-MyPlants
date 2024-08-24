package com.example.myplants

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myplants.navigation.Route
import com.example.myplants.ui.addEditPlant.AddEditPlantScreen
import com.example.myplants.ui.plantDetails.PlantDetailsScreen
import com.example.myplants.ui.plantList.PlantListScreen
import com.example.myplants.ui.theme.MyPlantsTheme

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
                            AddEditPlantScreen(navController)
                        }
                        composable(Route.PLANT_DETAILS) {
                            PlantDetailsScreen(navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyPlantsTheme {
        Greeting("Android")
    }
}