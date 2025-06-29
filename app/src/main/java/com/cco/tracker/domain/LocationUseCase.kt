package com.cco.tracker.domain

import android.location.Location // <-- Importante: usa android.location.Location
import android.util.Log
import com.cco.tracker.data.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationUseCase(private val repository: LocationRepository) {
    private val tag = "LocationUseCase"

    // --- CAMBIO: La función ahora RECIBE el objeto Location ---
    suspend fun getAndSendLocation(location: Location): Boolean = withContext(Dispatchers.IO) {
        Log.d(tag, "Iniciando caso de uso: getAndSendLocation")

        val user = repository.getSavedUser()
            ?: throw IllegalStateException("No se puede enviar la ubicación sin un usuario seleccionado.")

        Log.d(tag, "Usuario obtenido: ${user.name} (ID: ${user.id})")

        // --- CAMBIO: Usamos la nueva función del repositorio para construir los datos ---
        val locationData = repository.buildLocationData(location, user.id)

        Log.d(tag, "Datos de localización preparados: $locationData")

        Log.d(tag, "Enviando datos al repositorio...")
        return@withContext repository.sendLocation(locationData)
    }
}