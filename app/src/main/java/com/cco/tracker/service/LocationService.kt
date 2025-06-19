package com.cco.tracker.service

import android.Manifest // <-- Import necesario
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager // <-- Import necesario
import android.net.ConnectivityManager
import android.net.Network
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat // <-- Import necesario
import com.cco.tracker.R
import com.cco.tracker.data.model.LocationData
import com.cco.tracker.data.repository.LocationRepository
import com.cco.tracker.util.DebugLog
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LocationService : Service() {
    // ... (las variables se mantienen igual)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: LocationRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback


    override fun onCreate() {
        super.onCreate()
        DebugLog.addLog("LocationService: onCreate")
        repository = LocationRepository(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        setupNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DebugLog.addLog("LocationService: onStartCommand")
        startForegroundService()
        startLocationUpdates()
        syncQueuedLocations()
        return START_STICKY
    }

    // --- setupLocationCallback() y los métodos de sincronización se mantienen igual ---
    // ...

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 30000
        ).apply {
            setMinUpdateIntervalMillis(15000)
            setMinUpdateDistanceMeters(10f)
        }.build()

        // --- ESTA ES LA CORRECCIÓN CLAVE ---
        // Comprobamos el permiso OTRA VEZ aquí dentro, antes de la llamada "peligrosa".
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                DebugLog.addLog("Petición de actualizaciones de ubicación iniciada.")
            } catch (e: SecurityException) {
                // Este error ya no debería ocurrir, pero lo dejamos por seguridad.
                DebugLog.addLog("ERROR INESPERADO: SecurityException a pesar de tener permisos.")
                stopSelf()
            }
        } else {
            // Esto solo ocurriría si el servicio se inicia de alguna forma sin permisos.
            DebugLog.addLog("ERROR: El servicio arrancó sin permisos de ubicación.")
            stopSelf()
        }
    }

    // ... (el resto del servicio: sync, network callback, foreground, onDestroy, onBind) se mantiene igual
    // ...
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val locText = "Lat: ${location.latitude}, Lon: ${location.longitude}"
                DebugLog.addLog("Nueva ubicación detectada: $locText")

                serviceScope.launch {
                    val user = repository.getSavedUser() ?: return@launch
                    val locationData = repository.getCurrentLocationData(user.id) ?: return@launch

                    DebugLog.addLog("Intentando enviar al servidor...")
                    val success = repository.sendLocation(locationData)
                    if (success) {
                        DebugLog.addLog("-> Envío exitoso.")
                    } else {
                        DebugLog.addLog("-> Fallo de red. Guardando en cola.")
                        repository.queueLocation(locationData)
                    }
                }
            }
        }
    }

    private fun syncQueuedLocations() {
        serviceScope.launch {
            DebugLog.addLog("Sincronizando cola...")
            val queuedLocations = repository.getQueuedLocations()
            if (queuedLocations.isEmpty()) {
                DebugLog.addLog("Cola vacía. Nada que sincronizar.")
                return@launch
            }

            DebugLog.addLog("Enviando ${queuedLocations.size} puntos pendientes...")
            for (queuedLocation in queuedLocations) {
                val locationData = LocationData(
                    tracker_user_id = queuedLocation.tracker_user_id,
                    device_id = queuedLocation.device_id,
                    latitude = queuedLocation.latitude,
                    longitude = queuedLocation.longitude,
                    timestamp = queuedLocation.timestamp
                )
                val success = repository.sendLocation(locationData)
                if (success) {
                    repository.deleteQueuedLocation(queuedLocation.id)
                    DebugLog.addLog("-> Punto de cola #${queuedLocation.id} enviado y eliminado.")
                } else {
                    DebugLog.addLog("-> Fallo de red. Se detiene la sincronización.")
                    break
                }
            }
        }
    }

    private fun setupNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                DebugLog.addLog("Conexión a internet recuperada.")
                syncQueuedLocations()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        val channelName = "Location Service"
        val notificationManager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracker GPS Activo")
            .setContentText("Tu ubicación está siendo monitoreada.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
        DebugLog.addLog("Servicio puesto en primer plano.")
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        DebugLog.addLog("LocationService: onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}