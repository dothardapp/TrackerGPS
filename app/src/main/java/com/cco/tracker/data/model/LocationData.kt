package com.cco.tracker.data.model

import com.google.gson.annotations.SerializedName

data class LocationData(
    @SerializedName("device_id") val device_id: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestamp") val timestamp: String
)