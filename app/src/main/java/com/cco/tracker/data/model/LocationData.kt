package com.cco.tracker.data.model

import com.google.gson.annotations.SerializedName

data class LocationData(
    @SerializedName("tracker_user_id") val tracker_user_id: Long,
    @SerializedName("device_id") val device_id: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("speed") val speed: Float?,
    @SerializedName("bearing") val bearing: Float?,
    @SerializedName("altitude") val altitude: Double?,
    @SerializedName("accuracy") val accuracy: Float?
)