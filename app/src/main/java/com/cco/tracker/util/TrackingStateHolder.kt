package com.cco.tracker.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Objeto Singleton para que su estado sea accesible desde toda la app.
object TrackingStateHolder {
    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    fun setTrackingState(isTracking: Boolean) {
        _isTracking.value = isTracking
    }
}