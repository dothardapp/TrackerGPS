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
    private val useCase = LocationUseCase(repository)
    private val tag = "LocationViewModel"

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _userList = MutableStateFlow<List<TrackerUserResponse>>(emptyList())
    val userList: StateFlow<List<TrackerUserResponse>> = _userList.asStateFlow()

    private val _currentUser = MutableStateFlow<TrackerUserResponse?>(null)
    val currentUser: StateFlow<TrackerUserResponse?> = _currentUser.asStateFlow()

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _currentUser.value = repository.getSavedUser()
            fetchUserList()
            _isReady.value = true
        }
    }

    fun setSelectedUser(user: TrackerUserResponse) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSelectedUser(user)
            _currentUser.value = user
            Log.d(tag, "Usuario seleccionado y guardado: ${user.name} (ID: ${user.id})")
        }
    }

    fun fetchUserList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _userList.value = repository.getUsers()
            } catch (e: Exception) {
                Log.e(tag, "Excepción en ViewModel al obtener la lista de usuarios: ${e.message}")
                _userList.value = emptyList()
            }
        }
    }

    // --- CAMBIO AQUÍ: La lógica ahora maneja un Boolean ---
    fun getAndSendLocation() {
        Log.d(tag, "Iniciando getAndSendLocation...")
        viewModelScope.launch(Dispatchers.IO) {
            _locationState.value = LocationState.Loading
            try {
                // 'success' ahora es true o false.
                val success = useCase.getAndSendLocation()
                _locationState.value = if (success) {
                    Log.d(tag, "Ubicación enviada con éxito desde el ViewModel")
                    LocationState.Success
                } else {
                    // El error específico ya se logueó en el Repository
                    Log.w(tag, "Fallo en el envío gestionado por el UseCase.")
                    LocationState.Error("No se pudo enviar la ubicación.")
                }
            } catch (e: Exception) {
                // Capturamos las excepciones que lanza el UseCase (ej. si no hay usuario o GPS)
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