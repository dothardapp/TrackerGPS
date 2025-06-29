package com.cco.tracker.data.repository

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocationRepository(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val apiService = RetrofitClient.apiService
    private val tag = "LocationRepository"
    private val queuedLocationDao = AppDatabase.getDatabase(context).queuedLocationDao()

    private val trackingIntervalKey = longPreferencesKey("tracking_interval_seconds")
    private val trackingDistanceKey = intPreferencesKey("tracking_distance_meters")

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

    // La firma de la función recibe un objeto Location de Android ---
    fun buildLocationData(location: Location, userId: Long): LocationData {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date(location.time))
        return LocationData(
            tracker_user_id = userId,
            device_id = deviceId,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = timestamp,
            speed = if (location.hasSpeed()) location.speed else null,
            bearing = if (location.hasBearing()) location.bearing else null,
            altitude = if (location.hasAltitude()) location.altitude else null,
            accuracy = if (location.hasAccuracy()) location.accuracy else null
        )
    }

    suspend fun queueLocation(locationData: LocationData) {
        val queuedLocation = QueuedLocation(
            tracker_user_id = locationData.tracker_user_id,
            device_id = locationData.device_id,
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            timestamp = locationData.timestamp,
            speed = locationData.speed,
            bearing = locationData.bearing,
            altitude = locationData.altitude,
            accuracy = locationData.accuracy
        )
        queuedLocationDao.insert(queuedLocation)
        Log.i(tag, "Ubicación guardada en la cola local: ${queuedLocation.id}")
    }

    // Las demás funciones (getUsers, saveSelectedUser, getSavedUser, etc.)
    // se mantienen igual, ya que su lógica no se ve afectada.
    // ...
    suspend fun getUsers(): List<TrackerUserResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUsers()
            if (response.isSuccessful) { response.body() ?: emptyList() } else { emptyList() }
        } catch (e: Exception) { emptyList() }
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
        } else { null }
    }
    suspend fun sendLocation(location: LocationData): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.sendLocation(location)
            if (response.isSuccessful) {
                Log.d(tag, "Ubicación enviada con éxito.")
                true
            } else {
                Log.w(tag, "El servidor devolvió un error: ${response.code()}")
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
    suspend fun getQueuedLocations(): List<QueuedLocation> {
        return queuedLocationDao.getAll()
    }
    suspend fun deleteQueuedLocation(id: Int) {
        queuedLocationDao.deleteById(id)
    }


    // MÉTODOS PARA GUARDAR Y LEER LOS AJUSTES ---
    suspend fun saveTrackingSettings(intervalSeconds: Long, distanceMeters: Int) {
        context.dataStore.edit { preferences ->
            preferences[trackingIntervalKey] = intervalSeconds
            preferences[trackingDistanceKey] = distanceMeters
        }
        Log.d(tag, "Ajustes de tracking guardados: $intervalSeconds s, $distanceMeters m")
    }

    suspend fun getTrackingSettings(): Pair<Long, Int> {
        val preferences = context.dataStore.data.first()
        // Devolvemos los valores guardados, o unos valores por defecto si no existen
        val interval = preferences[trackingIntervalKey] ?: 30L // 30 segundos por defecto
        val distance = preferences[trackingDistanceKey] ?: 10 // 10 metros por defecto
        return Pair(interval, distance)
    }

}