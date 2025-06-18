package com.cco.tracker.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cco.tracker.data.network.RetrofitClient.apiService
import com.cco.tracker.data.repository.LocationRepository // Importar LocationRepository
import com.cco.tracker.domain.LocationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Extensión para DataStore: DEBE estar visible para LocationRepository y ViewModel
val Context.dataStore by preferencesDataStore(name = "user_preferences")

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    // Inicializa el repositorio y el caso de uso
    private val repository = LocationRepository(application.applicationContext) // Pasar applicationContext
    private val useCase = LocationUseCase(repository)
    private val tag = "LocationViewModel"

    // StateFlow para el estado de la ubicación
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    // StateFlow para la lista de usuarios
    private val _userList = MutableStateFlow<List<String>>(emptyList())
    val userList: StateFlow<List<String>> = _userList.asStateFlow()

    // StateFlow para el nombre de usuario actual (observado por ConfigScreen)
    private val _currentUserName = MutableStateFlow<String?>(null)
    val currentUserName: StateFlow<String?> = _currentUserName.asStateFlow()


    init {
        viewModelScope.launch(Dispatchers.IO) {
            val savedName = repository.getSavedUserName() // <--- Llamar al repositorio
            _currentUserName.value = savedName // Actualizar el StateFlow para la UI
            if (savedName != null) {
                // Si el nombre ya está cargado, no es necesario hacer un setUserName en el repositorio de nuevo
                // A menos que tu repositorio necesite "saber" el nombre para otras operaciones futuras
                // Por ahora, el ViewModel lo tiene en _currentUserName y el repositorio puede acceder a él si lo necesita.
                Log.d(tag, "Usuario cargado desde preferencias: $savedName")
            } else {
                Log.d(tag, "No hay usuario guardado en preferencias.")
            }
            fetchUserList() // Cargar la lista de usuarios al iniciar
        }
    }

    // El ViewModel ahora delega el guardado del nombre de usuario al repositorio
    fun setUserName(userName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setUserName(userName) // <--- Llamar al repositorio
            _currentUserName.value = userName // Actualizar el StateFlow inmediatamente para la UI
            Log.d(tag, "Nombre de usuario establecido y guardado: $userName")
        }
    }

    // getSavedUserName() ya no es necesario aquí, ya que el repositorio lo maneja y el init lo usa.

    fun fetchUserList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getUsers() // El ApiService se inyecta en el LocationRepository, pero aquí lo estás llamando directamente.
                // Esto es inconsistente con la arquitectura que estás construyendo.
                // Lo ideal sería que este método también se delegue al repositorio.
                if (response.isSuccessful) {
                    val users = response.body()?.map { it.name } ?: emptyList()
                    _userList.value = users
                    Log.d(tag, "Lista de usuarios obtenida: $users")
                } else {
                    Log.w(tag, "Error al obtener la lista de usuarios: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Excepción al obtener la lista de usuarios: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Esta función ya la tenías y se ve correcta en su lógica
    fun getAndSendLocation() {
        Log.d(tag, "Iniciando getAndSendLocation...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(tag, "Cambiando estado a Loading...")
                _locationState.value = LocationState.Loading
                val location = useCase.getLocation()
                Log.d(tag, "Ubicación obtenida: $location")
                if (location != null) {
                    val response = useCase.sendLocation(location)
                    Log.d(tag, "Respuesta del useCase: isSuccessful=${response.isSuccessful}, code=${response.code()}, message=${response.message()}")
                    _locationState.value = if (response.isSuccessful) {
                        Log.d(tag, "Ubicación enviada con éxito")
                        LocationState.Success
                    } else {
                        Log.w(tag, "Error en la respuesta: ${response.code()} - ${response.message()}")
                        LocationState.Error("Error: ${response.code()} - ${response.message()}")
                    }
                } else {
                    Log.w(tag, "Ubicación nula, cambiando a Error")
                    _locationState.value = LocationState.Error("No se pudo obtener la ubicación")
                }
            } catch (e: Exception) {
                Log.e(tag, "Excepción capturada: ${e.message}")
                e.printStackTrace()
                _locationState.value = LocationState.Error("Error: ${e.message}")
            }
        }
    }
}

sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    object Success : LocationState()
    data class Error(val message: String) : LocationState()
}