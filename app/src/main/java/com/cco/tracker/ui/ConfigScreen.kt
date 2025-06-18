package com.cco.tracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cco.tracker.ui.viewmodel.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: LocationViewModel, onSave: () -> Unit, modifier: Modifier = Modifier) {

    val savedUserName by viewModel.currentUserName.collectAsStateWithLifecycle(initialValue = "Julieta")
    val trackerUsers by viewModel.userList.collectAsStateWithLifecycle()
    var currentSelectedUserName by remember(savedUserName) { mutableStateOf(savedUserName ?: "Julieta") }
    var expanded by remember { mutableStateOf(false) }


    LaunchedEffect(savedUserName) {
        if (savedUserName != null) { // Solo actualiza si hay un valor guardado
            currentSelectedUserName = savedUserName.toString()
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
        Spacer(modifier = Modifier.height(16.dp))

        // Selección de usuario
        Box {
            Button(onClick = { expanded = true }) {
                // Muestra el nombre de usuario actual, o un mensaje si no hay seleccionados
                Text(currentSelectedUserName.ifEmpty { "Selecciona un usuario" })
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Usa la lista de usuarios obtenida del ViewModel
                if (trackerUsers.isEmpty()) {
                    // Muestra un mensaje si no hay usuarios cargados aún
                    DropdownMenuItem(
                        text = { Text("Cargando usuarios...") },
                        onClick = {} // Deshabilita la interacción
                    )
                } else {
                    trackerUsers.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(user) }, // Aquí 'user' es directamente el String del nombre
                            onClick = {
                                currentSelectedUserName = user // Actualiza el estado local con el nombre del usuario
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.setUserName(currentSelectedUserName) // Llama al ViewModel para guardar el nombre de usuario
                onSave() // Navega o realiza la acción de guardar completada
            },
            // Habilita el botón solo si se ha seleccionado un usuario (currentSelectedUserName no está vacío)
            enabled = currentSelectedUserName.isNotEmpty() && currentSelectedUserName != "Selecciona un usuario"
        ) {
            Text("Guardar Configuración")
        }
    }
}