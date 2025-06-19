package com.cco.tracker.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cco.tracker.data.model.TrackerUserResponse
import com.cco.tracker.data.repository.LocationRepository
import com.cco.tracker.domain.LocationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "user_preferences")

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocationRepository(application.applicationContext)
    private val useCase = LocationUseCase(repository) // El UseCase también lo actualizaremos
    private val tag = "LocationViewModel"

    // --- ESTADOS ACTUALIZADOS ---
    private val _isReady = MutableStateFlow<Boolean>(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _userList = MutableStateFlow<List<TrackerUserResponse>>(emptyList())
    val userList: StateFlow<List<TrackerUserResponse>> = _userList.asStateFlow()

    private val _currentUser = MutableStateFlow<TrackerUserResponse?>(null)
    val currentUser: StateFlow<TrackerUserResponse?> = _currentUser.asStateFlow()

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Usamos el nuevo método del repositorio
            _currentUser.value = repository.getSavedUser()
            fetchUserList()
            _isReady.value = true
        }
    }

    // --- NUEVO MÉTODO PARA GUARDAR EL USUARIO COMPLETO ---
    fun setSelectedUser(user: TrackerUserResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSelectedUser(user)
            _currentUser.value = user
            Log.d(tag, "Usuario seleccionado y guardado: ${user.name} (ID: ${user.id})")
        }
    }

    // --- MÉTODO ACTUALIZADO ---
    // Ahora obtiene la lista de objetos TrackerUserResponse
    fun fetchUserList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val users = repository.getUsers()
                _userList.value = users
                Log.d(tag, "Lista de usuarios actualizada desde el ViewModel: ${users.size} usuarios")
            } catch (e: Exception) {
                Log.e(tag, "Excepción en ViewModel al obtener la lista de usuarios: ${e.message}")
                _userList.value = emptyList()
            }
        }
    }

    // --- MÉTODO ACTUALIZADO Y SIMPLIFICADO ---
    // Ahora solo llama al caso de uso, que se encarga de todo.
    fun getAndSendLocation() {
        Log.d(tag, "Iniciando getAndSendLocation...")
        viewModelScope.launch(Dispatchers.IO) {
            _locationState.value = LocationState.Loading
            try {
                // El UseCase hará todo el trabajo de obtener ID, coordenadas y enviar.
                val response = useCase.getAndSendLocation()
                _locationState.value = if (response.isSuccessful) {
                    Log.d(tag, "Ubicación enviada con éxito desde el ViewModel")
                    LocationState.Success
                } else {
                    Log.w(tag, "Error en la respuesta del UseCase: ${response.code()} - ${response.message()}")
                    LocationState.Error("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Excepción capturada por el ViewModel: ${e.message}")
                _locationState.value = LocationState.Error("Error: ${e.message}")
            }
        }
    }
}

// La clase sellada LocationState no necesita cambios.
sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    object Success : LocationState()
    data class Error(val message: String) : LocationState()
}