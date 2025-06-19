package com.cco.tracker.domain

import android.util.Log
import com.cco.tracker.data.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class LocationUseCase(private val repository: LocationRepository) {
    private val tag = "LocationUseCase"

    // Este es ahora el único método público. Encapsula toda la lógica.
    suspend fun getAndSendLocation(): Response<Void> = withContext(Dispatchers.IO) {
        Log.d(tag, "Iniciando caso de uso: getAndSendLocation")

        // 1. Obtener el usuario guardado para conseguir su ID.
        val user = repository.getSavedUser()
        if (user == null) {
            // Si no hay usuario, no podemos continuar. Lanzamos una excepción
            // que será atrapada por el try/catch del ViewModel.
            Log.e(tag, "Operación fallida: No hay un usuario seleccionado.")
            throw IllegalStateException("No se puede enviar la ubicación sin un usuario seleccionado.")
        }
        Log.d(tag, "Usuario obtenido: ${user.name} (ID: ${user.id})")

        // 2. Obtener los datos de localización, pasando el ID del usuario.
        val locationData = repository.getCurrentLocationData(user.id)
        if (locationData == null) {
            // Si no se pudo obtener la ubicación GPS, tampoco podemos continuar.
            Log.e(tag, "Operación fallida: No se pudo obtener la ubicación física del dispositivo.")
            throw IllegalStateException("No se pudo obtener la ubicación física del dispositivo.")
        }
        Log.d(tag, "Datos de localización preparados: $locationData")

        // 3. Enviar los datos completos al servidor y devolver la respuesta.
        Log.d(tag, "Enviando datos al repositorio...")
        return@withContext repository.sendLocation(locationData)
    }
}