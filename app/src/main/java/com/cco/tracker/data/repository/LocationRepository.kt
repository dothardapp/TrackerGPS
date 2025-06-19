package com.cco.tracker.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cco.tracker.data.local.AppDatabase
import com.cco.tracker.data.model.LocationData
import com.cco.tracker.data.model.QueuedLocation
import com.cco.tracker.data.model.TrackerUserResponse
import com.cco.tracker.data.network.RetrofitClient
import com.cco.tracker.ui.viewmodel.dataStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocationRepository(private val context: Context) {
    // ... (el resto de la clase no cambia)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val apiService = RetrofitClient.apiService
    private val tag = "LocationRepository"
    private val queuedLocationDao = AppDatabase.getDatabase(context).queuedLocationDao()
    private val deviceId by lazy {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        id
    }
    private val userIdKey = longPreferencesKey("user_id")
    private val userNameKey = stringPreferencesKey("user_name")
    suspend fun getUsers(): List<TrackerUserResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUsers()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun saveSelectedUser(user: TrackerUserResponse) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences[userIdKey] = user.id
            preferences[userNameKey] = user.name
        }
    }
    suspend fun getSavedUser(): TrackerUserResponse? = withContext(Dispatchers.IO) {
        val preferences = context.dataStore.data.first()
        val userId = preferences[userIdKey]
        val userName = preferences[userNameKey]

        if (userId != null && userName != null) {
            TrackerUserResponse(id = userId, name = userName, createdAt = "", updatedAt = "")
        } else {
            null
        }
    }
    suspend fun getCurrentLocationData(userId: Long): LocationData? = withContext(Dispatchers.IO) {
        try {
            fusedLocationClient.lastLocation.await()?.let { location ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
                LocationData(
                    tracker_user_id = userId,
                    device_id = deviceId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = timestamp
                )
            }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }


    // --- MODIFICADO: Añadimos un log para el cuerpo del error ---
    suspend fun sendLocation(location: LocationData): Boolean = withContext(Dispatchers.IO) {
        Log.d(tag, "Intentando enviar ubicación al servidor: $location")
        try {
            val response = apiService.sendLocation(location)
            if (response.isSuccessful) {
                Log.d(tag, "Ubicación enviada con éxito.")
                true
            } else {
                Log.w(tag, "El servidor devolvió un error: ${response.code()}")
                // ESTA ES LA LÍNEA NUEVA E IMPORTANTE:
                val errorBody = response.errorBody()?.string()
                Log.e(tag, "Cuerpo del error del servidor: $errorBody")
                false
            }
        } catch (e: IOException) {
            Log.e(tag, "Error de red al enviar ubicación: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(tag, "Error desconocido al enviar ubicación: ${e.message}")
            false
        }
    }

    // ... (el resto de los métodos para la cola no cambian)
    suspend fun queueLocation(location: LocationData) {
        val queuedLocation = QueuedLocation(
            tracker_user_id = location.tracker_user_id,
            device_id = location.device_id,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = location.timestamp
        )
        queuedLocationDao.insert(queuedLocation)
        Log.i(tag, "Ubicación guardada en la cola local: ${queuedLocation.id}")
    }

    suspend fun getQueuedLocations(): List<QueuedLocation> {
        return queuedLocationDao.getAll()
    }

    suspend fun deleteQueuedLocation(id: Int) {
        queuedLocationDao.deleteById(id)
    }
}