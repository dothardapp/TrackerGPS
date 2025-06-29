package com.cco.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cco.tracker.ui.ConfigScreen
import com.cco.tracker.ui.LocationScreen
import com.cco.tracker.ui.SettingsScreen
import com.cco.tracker.ui.UserSearchScreen
import com.cco.tracker.ui.theme.TrackerGPSTheme
import com.cco.tracker.ui.viewmodel.LocationViewModel
import com.cco.tracker.ui.viewmodel.LocationViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy {
        LocationViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackerGPSTheme {
                val navController = rememberNavController()
                val locationViewModel: LocationViewModel = viewModel(factory = viewModelFactory)

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text("Tracker GPS", modifier = Modifier.padding(16.dp))
                            HorizontalDivider()
                            NavigationDrawerItem(
                                label = { Text("Localización") },
                                selected = currentRoute == "location",
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate("location") { launchSingleTop = true }
                                }
                            )
                            NavigationDrawerItem(
                                label = { Text("Configuración") },
                                selected = currentRoute == "config",
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate("config") { launchSingleTop = true }
                                }
                            )
                            // --- AÑADIMOS EL NUEVO ITEM DE MENÚ ---
                            NavigationDrawerItem(
                                label = { Text("Ajustes") },
                                selected = currentRoute == "settings",
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate("settings") { launchSingleTop = true }
                                }
                            )
                        }
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    val title = when (currentRoute) {
                                        "location" -> "Localización"
                                        "config" -> "Configuración"
                                        "user_search" -> "Buscar Usuario"
                                        "settings" -> "Ajustes" // <-- TÍTULO PARA LA BARRA
                                        else -> "Tracker GPS"
                                    }
                                    Text(text = title)
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        val isReady by locationViewModel.isReady.collectAsStateWithLifecycle()
                        val savedUser by locationViewModel.currentUser.collectAsStateWithLifecycle()

                        if (!isReady) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val startDestination = if (savedUser != null) "location" else "config"
                            NavHost(
                                navController = navController,
                                startDestination = startDestination,
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                composable("config") {
                                    ConfigScreen(
                                        viewModel = locationViewModel,
                                        navController = navController,
                                        onSave = {
                                            navController.navigate("location") {
                                                popUpTo("config") { inclusive = true }
                                            }
                                        }
                                    )
                                }
                                composable("location") {
                                    LocationScreen(viewModel = locationViewModel)
                                }
                                composable("user_search") {
                                    UserSearchScreen(
                                        viewModel = locationViewModel,
                                        navController = navController
                                    )
                                }
                                // --- AÑADIMOS LA NUEVA RUTA ---
                                composable("settings") {
                                    SettingsScreen(viewModelFactory = viewModelFactory)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}