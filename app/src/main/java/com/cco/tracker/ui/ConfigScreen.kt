package com.cco.tracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cco.tracker.data.model.TrackerUserResponse
import com.cco.tracker.ui.viewmodel.LocationViewModel

@Composable
fun ConfigScreen(
    viewModel: LocationViewModel,
    navController: NavController,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Observamos el objeto de usuario completo guardado
    val savedUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // 2. El estado local ahora es de tipo TrackerUserResponse?
    var currentSelectedUser by remember(savedUser) { mutableStateOf(savedUser) }

    // 3. Esperamos recibir un objeto TrackerUserResponse de la pantalla de búsqueda
    val searchResult by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<TrackerUserResponse?>("selected_user", null)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }

    // Este LaunchedEffect ahora maneja el objeto completo
    LaunchedEffect(searchResult) {
        searchResult?.let { selectedUser ->
            currentSelectedUser = selectedUser
            navController.currentBackStackEntry?.savedStateHandle?.remove<TrackerUserResponse>("selected_user")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Configuración de Usuario", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Usuario seleccionado",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate("user_search") },
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 4. Mostramos la propiedad 'name' del objeto
                Text(
                    text = currentSelectedUser?.name ?: "Toca para seleccionar",
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Seleccionar usuario"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // 5. Llamamos a la nueva función del ViewModel, pasando el objeto completo
                currentSelectedUser?.let { user ->
                    viewModel.setSelectedUser(user)
                    onSave()
                }
            },
            // El botón se activa si hemos seleccionado un usuario
            enabled = currentSelectedUser != null
        ) {
            Text("Guardar Configuración")
        }
    }
}