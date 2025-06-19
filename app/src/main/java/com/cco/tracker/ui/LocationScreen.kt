package com.cco.tracker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cco.tracker.service.LocationService
import com.cco.tracker.ui.viewmodel.LocationViewModel
import com.cco.tracker.util.DebugLog
import com.cco.tracker.util.PermissionsUtil

@Composable
fun LocationScreen(viewModel: LocationViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var isTracking by remember { mutableStateOf(false) } // Este estado debería ser más robusto en producción
    val debugLogs by DebugLog.logMessages.collectAsStateWithLifecycle()

    // --- NUEVA LÓGICA DE PERMISOS EN DOS PASOS ---

    // Launcher para el permiso de segundo plano (BACKGROUND)
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            DebugLog.addLog("Permiso de segundo plano CONCEDIDO.")
            startTrackingService(context)
            isTracking = true
        } else {
            DebugLog.addLog("ERROR: Permiso de segundo plano DENEGADO.")
            // Aquí podrías mostrar un diálogo explicando por qué es necesario
        }
    }

    // Launcher para los permisos de primer plano (FINE/COARSE)
    val foregroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isFineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val isCoarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (isFineGranted || isCoarseGranted) {
            DebugLog.addLog("Permisos de primer plano CONCEDIDOS.")
            // Una vez concedido el primer plano, si es Android 10+ pedimos el de segundo plano
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    startTrackingService(context)
                    isTracking = true
                } else {
                    DebugLog.addLog("Pidiendo permiso de segundo plano...")
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            } else {
                // En versiones antiguas de Android, con el primer permiso es suficiente
                startTrackingService(context)
                isTracking = true
            }
        } else {
            DebugLog.addLog("ERROR: Permisos de primer plano DENEGADOS.")
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ... (el panel de control superior se mantiene igual)
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Usuario activo: ${currentUser?.name ?: "Cargando..."}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = {
                    // --- LÓGICA DE INICIO CON NUEVO FLUJO DE PERMISOS ---
                    if (PermissionsUtil.hasLocationPermissions(context)) {
                        // Ya tenemos permiso de primer plano, comprobamos el de segundo plano
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            DebugLog.addLog("Ya tiene permiso FG, pidiendo BG...")
                            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            startTrackingService(context)
                            isTracking = true
                        }
                    } else {
                        // No tenemos ningún permiso, iniciamos la petición desde cero
                        DebugLog.addLog("Pidiendo permisos de primer plano...")
                        foregroundLocationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }, enabled = !isTracking) {
                    Text("Iniciar")
                }

                Button(
                    onClick = {
                        isTracking = false
                        context.stopService(Intent(context, LocationService::class.java))
                    },
                    enabled = isTracking,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Detener")
                }
            }
        }

        HorizontalDivider()

        // El panel de logs se mantiene igual...
    }
}

// Función de ayuda para no repetir código
private fun startTrackingService(context: Context) {
    DebugLog.addLog("Iniciando el servicio de seguimiento...")
    Intent(context, LocationService::class.java).also {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(it)
        } else {
            context.startService(it)
        }
    }
}