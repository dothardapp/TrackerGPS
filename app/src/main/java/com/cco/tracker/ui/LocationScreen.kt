package com.cco.tracker.ui

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cco.tracker.ui.viewmodel.LocationState
import com.cco.tracker.ui.viewmodel.LocationViewModel
import com.cco.tracker.util.PermissionsUtil

@Composable
fun LocationScreen(viewModel: LocationViewModel, modifier: Modifier = Modifier) {

    val state: LocationState by viewModel.locationState.collectAsStateWithLifecycle()
    // --- CAMBIO 1: Observamos el objeto de usuario completo ---
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(PermissionsUtil.hasLocationPermissions(context)) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermissions = PermissionsUtil.hasLocationPermissions(context)
        if (hasPermissions) {
            viewModel.getAndSendLocation()
        } else {
            Log.e("LocationScreen", "Permisos de ubicación no concedidos.")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            // --- CAMBIO 2: Mostramos la propiedad 'name' del objeto currentUser ---
            text = "Usuario activo: ${currentUser?.name ?: "Cargando..."}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (hasPermissions) {
                    viewModel.getAndSendLocation()
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        ) {
            Text("Obtener y Enviar Ubicación")
        }
        Spacer(modifier = Modifier.height(16.dp))
        when (state) {
            LocationState.Idle -> Text("Presiona el botón para comenzar")
            LocationState.Loading -> CircularProgressIndicator()
            LocationState.Success -> Text("Ubicación enviada con éxito")
            is LocationState.Error -> Text((state as LocationState.Error).message)
        }
    }
}