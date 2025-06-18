package com.cco.tracker.data.model

import com.google.gson.annotations.SerializedName

data class TrackerUserResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)