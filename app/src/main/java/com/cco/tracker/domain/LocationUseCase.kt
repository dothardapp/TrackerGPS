package com.cco.tracker.domain

import android.util.Log
import com.cco.tracker.data.model.LocationData
import com.cco.tracker.data.repository.LocationRepository
import retrofit2.Response

class LocationUseCase(private val repository: LocationRepository) {
    private val tag = "LocationUseCase"

    suspend fun getLocation(): LocationData? {
        Log.d(tag, "Solicitando ubicación al repositorio...")
        val location = repository.getLocation()
        Log.d(tag, "Ubicación recibida del repositorio: $location")
        return location
    }

    suspend fun sendLocation(location: LocationData): Response<Void> {
        Log.d(tag, "Enviando ubicación al repositorio: $location")
        val response = repository.sendLocation(location)
        Log.d(tag, "Respuesta recibida del repositorio: isSuccessful=${response.isSuccessful}, code=${response.code()}")
        return response
    }
}