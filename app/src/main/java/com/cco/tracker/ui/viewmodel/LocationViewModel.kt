package com.cco.tracker.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cco.tracker.data.model.TrackerUserResponse
import com.cco.tracker.data.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "user_preferences")

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocationRepository(application.applicationContext)
    private val tag = "LocationViewModel"

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _userList = MutableStateFlow<List<TrackerUserResponse>>(emptyList())
    val userList: StateFlow<List<TrackerUserResponse>> = _userList.asStateFlow()

    private val _currentUser = MutableStateFlow<TrackerUserResponse?>(null)
    val currentUser: StateFlow<TrackerUserResponse?> = _currentUser.asStateFlow()

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

}

