package com.cco.tracker.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Usamos un 'object' para crear un Singleton: una única instancia para toda la app.
object DebugLog {
    private val _logMessages = MutableStateFlow<List<String>>(emptyList())
    val logMessages: StateFlow<List<String>> = _logMessages.asStateFlow()

    fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newMessage = "$timestamp: $message"

        // Añadimos el nuevo mensaje al principio de la lista para que se vea arriba
        _logMessages.value = listOf(newMessage) + _logMessages.value
    }
}