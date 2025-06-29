package com.cco.tracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cco.tracker.ui.viewmodel.LocationViewModelFactory
import com.cco.tracker.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    // Pasamos el factory para crear el ViewModel
    viewModelFactory: LocationViewModelFactory,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val interval by viewModel.interval.collectAsState()
    val distance by viewModel.distance.collectAsState()

    // Estados locales para el slider
    var sliderInterval by remember(interval) { mutableFloatStateOf(interval) }
    var sliderDistance by remember(distance) { mutableFloatStateOf(distance) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ajustes de Seguimiento", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))

        // Slider para el Intervalo
        Text("Intervalo de tiempo: ${sliderInterval.roundToInt()} segundos")
        Slider(
            value = sliderInterval,
            onValueChange = { sliderInterval = it },
            valueRange = 5f..300f, // De 5 segundos a 5 minutos
            steps = 58, // Para que vaya de 5 en 5
            onValueChangeFinished = {
                // Guardamos solo cuando el usuario suelta el slider
                viewModel.saveSettings(sliderInterval, sliderDistance)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Slider para la Distancia
        Text("Distancia mínima: ${sliderDistance.roundToInt()} metros")
        Slider(
            value = sliderDistance,
            onValueChange = { sliderDistance = it },
            valueRange = 5f..100f, // De 5 a 100 metros
            steps = 18,
            onValueChangeFinished = {
                viewModel.saveSettings(sliderInterval, sliderDistance)
            }
        )
    }
}