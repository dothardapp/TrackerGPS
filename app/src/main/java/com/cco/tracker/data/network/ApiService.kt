package com.cco.tracker.data.network

import com.cco.tracker.data.model.LocationData
import com.cco.tracker.data.model.TrackerUserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("api/location")
    suspend fun sendLocation(@Body location: LocationData): Response<Void>

    @GET("api/tracker-users")
    suspend fun getUsers(): Response<List<TrackerUserResponse>>
}