package com.cco.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queued_locations")
data class QueuedLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tracker_user_id: Long,
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val speed: Float?,
    val bearing: Float?,
    val altitude: Double?,
    val accuracy: Float?
)