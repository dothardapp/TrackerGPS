package com.cco.tracker.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cco.tracker.data.model.LocationData
import com.cco.tracker.data.network.RetrofitClient
import com.cco.tracker.ui.viewmodel.dataStore // Importar la extensión dataStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first // Importar first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import retrofit2.Response

class LocationRepository(private val context: Context) { // Asegúrate de que el context sea accesible
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val apiService = RetrofitClient.apiService
    private val deviceId = UUID.randomUUID().toString() // ID único por dispositivo
    private val tag = "LocationRepository"

    // Clave para guardar el nombre de usuario en DataStore
    private val userNameKey = stringPreferencesKey("user_name")


    // Método para obtener la última ubicación
    suspend fun getLocation(): LocationData? = withContext(Dispatchers.IO) {
        Log.d(tag, "Obteniendo ubicación...")
        try {
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
                    .format(Date()) // Usar XXX para zona horaria
                val locationData = LocationData(
                    device_id = deviceId, // Usar device_id
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = timestamp
                )
                Log.d(tag, "Ubicación obtenida: $locationData")
                locationData
            } else {
                Log.w(tag, "No se pudo obtener la ubicación (location es null)")
                null
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Error de seguridad al obtener ubicación: ${e.message}")
            e.printStackTrace()
            null
        } catch (e: Exception) { // Capturar otras excepciones también
            Log.e(tag, "Excepción general al obtener ubicación: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Método para enviar la ubicación
    suspend fun sendLocation(location: LocationData): Response<Void> = withContext(Dispatchers.IO) {
        Log.d(tag, "Enviando ubicación al servidor: $location")
        try {
            val json = Gson().toJson(location)
            Log.d(tag, "JSON enviado: $json")
            val response = apiService.sendLocation(location)
            Log.d(tag, "Respuesta del servidor: isSuccessful=${response.isSuccessful}, code=${response.code()}, message=${response.message()}, body=${response.body()}")
            response
        } catch (e: Exception) {
            Log.e(tag, "Error al enviar ubicación: ${e.message}")
            e.printStackTrace()
            throw e // Re-lanzar para que el ViewModel pueda manejarlo si lo desea
        }
    }

    // --- NUEVOS MÉTODOS PARA MANEJAR EL NOMBRE DE USUARIO ---

    // Guardar el nombre de usuario en DataStore
    suspend fun setUserName(userName: String) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences[userNameKey] = userName
        }
        Log.d(tag, "Nombre de usuario '$userName' guardado en DataStore.")
    }

    // Obtener el nombre de usuario de DataStore
    suspend fun getSavedUserName(): String? = withContext(Dispatchers.IO) {
        val userName = context.dataStore.data.first()[userNameKey]
        Log.d(tag, "Nombre de usuario cargado de DataStore: ${userName ?: "N/A"}")
        userName
    }
}