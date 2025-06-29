package com.cco.tracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

/**
 * Una fábrica de ViewModels que sabe cómo crear todos los ViewModels de la app
 * que requieren el 'Application' context.
 */
class LocationViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // --- ESTE ES EL CAMBIO CLAVE ---
        // Usamos un 'when' para decidir qué ViewModel crear
        return when {
            // Si nos piden un LocationViewModel, lo creamos y devolvemos.
            modelClass.isAssignableFrom(LocationViewModel::class.java) -> {
                LocationViewModel(application) as T
            }
            // Si nos piden un SettingsViewModel, lo creamos y devolvemos.
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(application) as T
            }
            // Si nos piden cualquier otra cosa, lanzamos el error.
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
