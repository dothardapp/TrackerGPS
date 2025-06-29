package com.cco.tracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cco.tracker.data.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    // Usamos el mismo repositorio
    private val repository = LocationRepository(application.applicationContext)

    private val _interval = MutableStateFlow(30f)
    val interval = _interval.asStateFlow()

    private val _distance = MutableStateFlow(10f)
    val distance = _distance.asStateFlow()

    init {
        // Al iniciar, cargamos los ajustes guardados
        viewModelScope.launch {
            val (savedInterval, savedDistance) = repository.getTrackingSettings()
            _interval.value = savedInterval.toFloat()
            _distance.value = savedDistance.toFloat()
        }
    }

    fun saveSettings(intervalSeconds: Float, distanceMeters: Float) {
        viewModelScope.launch {
            repository.saveTrackingSettings(intervalSeconds.toLong(), distanceMeters.toInt())
        }
    }
}
