package com.cco.tracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cco.tracker.data.model.TrackerUserResponse
import com.cco.tracker.ui.viewmodel.LocationViewModel

@Composable
fun UserSearchScreen(
    viewModel: LocationViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    // 1. userList ahora es una StateFlow<List<TrackerUserResponse>>
    val userList by viewModel.userList.collectAsStateWithLifecycle()

    // 2. Filtramos usando la propiedad 'name' del objeto
    val filteredUsers = if (searchQuery.isEmpty()) {
        userList
    } else {
        userList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar usuario...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            // 3. 'user' ahora es un objeto TrackerUserResponse
            items(filteredUsers) { user ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 4. Guardamos el objeto 'user' completo.
                            // Esto funciona porque lo hicimos Parcelable.
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("selected_user", user)

                            navController.popBackStack()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Mostramos la propiedad 'name' del objeto
                    Text(text = user.name)
                }
                HorizontalDivider()
            }
        }
    }
}