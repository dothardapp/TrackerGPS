package com.cco.tracker.data.model

import com.google.gson.annotations.SerializedName

data class LocationData(
    @SerializedName("tracker_user_id") val trackerUserId: Long,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("speed") val speed: Float?,
    @SerializedName("bearing") val bearing: Float?,
    @SerializedName("altitude") val altitude: Double?,
    @SerializedName("accuracy") val accuracy: Float?
)