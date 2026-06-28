package com.example.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("guest_login.php")
    suspend fun guestLogin(@Body req: GuestLoginRequest): LoginResponse

    @POST("google_login.php")
    suspend fun googleLogin(@Body req: GoogleLoginRequest): LoginResponse

    @GET("get_profile.php")
    suspend fun getProfile(@Header("Authorization") bearer: String): ProfileResponse

    @POST("save_profile.php")
    suspend fun saveProfile(
        @Header("Authorization") bearer: String,
        @Body req: SaveProfileRequest
    ): ProfileResponse
}
