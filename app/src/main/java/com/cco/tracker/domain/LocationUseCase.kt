package com.cco.tracker.domain

import android.util.Log
import com.cco.tracker.data.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationUseCase(private val repository: LocationRepository) {
    private val tag = "LocationUseCase"

    // --- CAMBIO AQUÍ: Ahora devuelve Boolean ---
    suspend fun getAndSendLocation(): Boolean = withContext(Dispatchers.IO) {
        Log.d(tag, "Iniciando caso de uso: getAndSendLocation")

        val user = repository.getSavedUser()
        if (user == null) {
            Log.e(tag, "Operación fallida: No hay un usuario seleccionado.")
            throw IllegalStateException("No se puede enviar la ubicación sin un usuario seleccionado.")
        }
        Log.d(tag, "Usuario obtenido: ${user.name} (ID: ${user.id})")

        val locationData = repository.getCurrentLocationData(user.id)
        if (locationData == null) {
            Log.e(tag, "Operación fallida: No se pudo obtener la ubicación física del dispositivo.")
            throw IllegalStateException("No se pudo obtener la ubicación física del dispositivo.")
        }
        Log.d(tag, "Datos de localización preparados: $locationData")

        Log.d(tag, "Enviando datos al repositorio...")
        // La llamada al repositorio ya devuelve un Boolean, así que esto ahora es correcto.
        return@withContext repository.sendLocation(locationData)
    }
}