package com.xwurfel.tourry.feature.auth.api

import com.xwurfel.tourry.feature.auth.api.model.AuthResponse
import com.xwurfel.tourry.feature.auth.api.model.LoginRequest
import com.xwurfel.tourry.feature.auth.api.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}