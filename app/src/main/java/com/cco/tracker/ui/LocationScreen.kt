package com.cco.tracker.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cco.tracker.service.LocationService
import com.cco.tracker.ui.viewmodel.LocationViewModel
import com.cco.tracker.util.DebugLog
import com.cco.tracker.util.PermissionsUtil
import com.cco.tracker.util.TrackingStateHolder // <-- Importa el nuevo Holder

@Composable
fun LocationScreen(viewModel: LocationViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // --- CAMBIO CLAVE: Observamos el estado global en lugar de usar uno local ---
    val isTracking by TrackingStateHolder.isTracking.collectAsStateWithLifecycle()

    val debugLogs by DebugLog.logMessages.collectAsStateWithLifecycle()

    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            DebugLog.addLog("Permiso de segundo plano CONCEDIDO.")
            startTrackingService(context)
        } else {
            DebugLog.addLog("ERROR: Permiso de segundo plano DENEGADO.")
        }
    }

    val foregroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isFineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val isCoarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (isFineGranted || isCoarseGranted) {
            DebugLog.addLog("Permisos de primer plano CONCEDIDOS.")
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startTrackingService(context)
            } else {
                DebugLog.addLog("Pidiendo permiso de segundo plano...")
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            DebugLog.addLog("ERROR: Permisos de primer plano DENEGADOS.")
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    if (PermissionsUtil.hasLocationPermissions(context)) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            startTrackingService(context)
                        }
                    } else {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            reverseLayout = true
        ) {
            items(debugLogs) { logMsg ->
                Text(
                    text = logMsg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

private fun startTrackingService(context: Context) {
    DebugLog.addLog("Iniciando el servicio de seguimiento...")
    Intent(context, LocationService::class.java).also {
        context.startForegroundService(it)
    }
}