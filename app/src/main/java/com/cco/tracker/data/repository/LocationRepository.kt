package com.cco.tracker.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cco.tracker.data.model.LocationData
import com.cco.tracker.data.model.TrackerUserResponse
import com.cco.tracker.data.network.RetrofitClient
import com.cco.tracker.ui.viewmodel.dataStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocationRepository(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val apiService = RetrofitClient.apiService
    private val tag = "LocationRepository"

    // --- MEJORA: deviceId persistente ---
    // Usamos SharedPreferences para guardar el ID del dispositivo y que no cambie.
    private val deviceId by lazy {
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        id
    }

    // --- CAMBIO: Nuevas claves para guardar ID y Nombre en DataStore ---
    private val userIdKey = longPreferencesKey("user_id")
    private val userNameKey = stringPreferencesKey("user_name")


    // --- CAMBIO: getUsers ahora devuelve una lista de TrackerUserResponse ---
    suspend fun getUsers(): List<TrackerUserResponse> = withContext(Dispatchers.IO) {
        Log.d(tag, "Obteniendo lista de usuarios desde el servidor...")
        try {
            val response = apiService.getUsers()
            if (response.isSuccessful) {
                val userList = response.body() ?: emptyList()
                Log.d(tag, "Lista de usuarios obtenida con éxito: $userList")
                userList // Devolvemos la lista de objetos completos
            } else {
                Log.w(tag, "Error al obtener la lista de usuarios. Código: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Excepción al obtener la lista de usuarios: ${e.message}")
            emptyList()
        }
    }

    // --- NUEVO MÉTODO: Guarda el usuario completo (ID y Nombre) ---
    suspend fun saveSelectedUser(user: TrackerUserResponse) = withContext(Dispatchers.IO) {
        context.dataStore.edit { preferences ->
            preferences[userIdKey] = user.id
            preferences[userNameKey] = user.name
        }
        Log.d(tag, "Usuario '${user.name}' (ID: ${user.id}) guardado en DataStore.")
    }

    // --- NUEVO MÉTODO: Obtiene el usuario completo guardado ---
    suspend fun getSavedUser(): TrackerUserResponse? = withContext(Dispatchers.IO) {
        val preferences = context.dataStore.data.first()
        val userId = preferences[userIdKey]
        val userName = preferences[userNameKey]

        if (userId != null && userName != null) {
            Log.d(tag, "Usuario cargado de DataStore: $userName (ID: $userId)")
            // Reconstruimos el objeto. createdAt y updatedAt no son necesarios para la lógica de la app.
            TrackerUserResponse(id = userId, name = userName, createdAt = "", updatedAt = "")
        } else {
            Log.d(tag, "No hay usuario guardado en DataStore.")
            null
        }
    }

    // --- CAMBIO: Renombramos y ajustamos el viejo 'getLocation' ---
    // Ahora esta función necesita el ID del usuario para crear el objeto LocationData completo.
    suspend fun getCurrentLocationData(userId: Long): LocationData? = withContext(Dispatchers.IO) {
        Log.d(tag, "Obteniendo ubicación para el usuario ID: $userId")
        try {
            fusedLocationClient.lastLocation.await()?.let { location ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
                LocationData(
                    tracker_user_id = userId, // <-- Usamos el ID recibido
                    device_id = deviceId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = timestamp
                ).also {
                    Log.d(tag, "Ubicación obtenida: $it")
                }
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Error de seguridad al obtener ubicación: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(tag, "Excepción al obtener ubicación: ${e.message}")
            null
        }
    }

    // --- SIN CAMBIOS ---
    // Esta función sigue siendo válida porque recibe el objeto LocationData ya completo.
    suspend fun sendLocation(location: LocationData): Response<Void> = withContext(Dispatchers.IO) {
        Log.d(tag, "Enviando ubicación al servidor: $location")
        try {
            apiService.sendLocation(location)
        } catch (e: Exception) {
            Log.e(tag, "Error al enviar ubicación: ${e.message}")
            throw e
        }
    }
}