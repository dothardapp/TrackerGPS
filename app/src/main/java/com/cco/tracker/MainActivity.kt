package com.cco.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.cco.tracker.ui.ConfigScreen
import com.cco.tracker.ui.LocationScreen
import com.cco.tracker.ui.theme.TrackerGPSTheme
import com.cco.tracker.ui.viewmodel.LocationViewModel
import com.cco.tracker.ui.viewmodel.LocationViewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {


    private val viewModelFactory by lazy {
        LocationViewModelFactory(application) // Esto es correcto si LocationViewModelFactory solo necesita Application
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackerGPSTheme {
                val navController = rememberNavController()

                val locationViewModel: LocationViewModel = viewModel(factory = viewModelFactory)

                NavHost(navController = navController, startDestination = "config") {
                    composable("config") {
                        ConfigScreen(
                            viewModel = locationViewModel,
                            onSave = { navController.navigate("location") }
                        )
                    }
                    composable("location") {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            LocationScreen(
                                viewModel = locationViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}