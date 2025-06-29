package com.cco.tracker.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cco.tracker.R
import com.cco.tracker.data.model.LocationData
import com.cco.tracker.data.repository.LocationRepository
import com.cco.tracker.domain.LocationUseCase
import com.cco.tracker.util.DebugLog
import com.cco.tracker.util.TrackingStateHolder
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LocationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: LocationRepository
    private lateinit var useCase: LocationUseCase
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        DebugLog.addLog("LocationService: onCreate")
        // La inicialización de componentes se mantiene aquí, es rápido.
        repository = LocationRepository(applicationContext)
        useCase = LocationUseCase(repository)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        setupNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DebugLog.addLog("LocationService: onStartCommand")

        // --- ESTA ES LA CORRECCIÓN CLAVE ---
        // 1. Llamamos a startForeground() INMEDIATAMENTE al iniciar el comando.
        startForegroundService()

        // 2. Informamos al resto de la app que estamos activos.
        TrackingStateHolder.setTrackingState(true)

        // 3. Ahora que el sistema está satisfecho, iniciamos el resto de las operaciones.
        startLocationUpdates()
        syncQueuedLocations()

        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        val channelName = "Location Service"
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Es seguro llamar a createNotificationChannel varias veces.
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracker GPS Activo")
            .setContentText("Tu ubicación está siendo monitoreada.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener este icono
            .build()

        // Esta es la llamada que el sistema operativo espera recibir rápidamente.
        startForeground(1, notification)
        DebugLog.addLog("Servicio puesto en primer plano.")
    }

    // El resto del archivo no necesita cambios...
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                // Si la precisión es mayor a 50 metros, consideramos que es un punto de mala calidad y lo ignoramos.
                // Puedes ajustar este valor según tus pruebas.
                if (location.hasAccuracy() && location.accuracy > 40.0f) {
                    DebugLog.addLog("Punto descartado: baja precisión (${location.accuracy}m)")
                    return
                }

                val locText = "Lat: ${location.latitude}, Lon: ${location.longitude}"
                DebugLog.addLog("Nueva ubicación detectada: $locText")

                serviceScope.launch {
                    try {
                        val success = useCase.getAndSendLocation(location)
                        if (success) {
                            DebugLog.addLog("-> Envío exitoso.")
                        } else {
                            DebugLog.addLog("-> Fallo de red. Guardando en cola.")
                            repository.getSavedUser()?.let { user ->
                                val locationData = repository.buildLocationData(location, user.id)
                                repository.queueLocation(locationData)
                            }
                        }
                    } catch (e: Exception) {
                        DebugLog.addLog("ERROR en el proceso de envío: ${e.message}")
                    }
                }
            }
        }
    }

    // Hacemos que esta función sea 'suspend' para poder llamar al repositorio
    private fun startLocationUpdates() = serviceScope.launch {
        // --- LEEMOS LOS AJUSTES GUARDADOS ---
        val (intervalSeconds, distanceMeters) = repository.getTrackingSettings()
        DebugLog.addLog("Iniciando updates con: ${intervalSeconds}s / ${distanceMeters}m")

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, intervalSeconds * 1000 // Convertimos a milisegundos
        ).apply {
            setMinUpdateIntervalMillis((intervalSeconds * 1000) / 2) // La mitad del intervalo
            setMinUpdateDistanceMeters(distanceMeters.toFloat())
        }.build()

        if (ContextCompat.checkSelfPermission(this@LocationService, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                DebugLog.addLog("Petición de actualizaciones de ubicación iniciada.")
            } catch (_: SecurityException) {
                DebugLog.addLog("ERROR INESPERADO: SecurityException a pesar de tener permisos.")
                stopSelf()
            }
        } else {
            DebugLog.addLog("ERROR: El servicio arrancó sin permisos de ubicación.")
            stopSelf()
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
                    timestamp = queuedLocation.timestamp,
                    speed = queuedLocation.speed,
                    bearing = queuedLocation.bearing,
                    altitude = queuedLocation.altitude,
                    accuracy = queuedLocation.accuracy
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
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                DebugLog.addLog("Conexión a internet recuperada.")
                syncQueuedLocations()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        TrackingStateHolder.setTrackingState(false)
        DebugLog.addLog("LocationService: onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
