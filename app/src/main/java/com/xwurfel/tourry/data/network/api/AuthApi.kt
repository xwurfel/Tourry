package com.xwurfel.tourry.data.network.api

import com.xwurfel.tourry.data.network.dto.AuthResponseDto
import com.xwurfel.tourry.data.network.dto.LoginDto
import com.xwurfel.tourry.data.network.dto.RegisterDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body registerDto: RegisterDto): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(@Body loginDto: LoginDto): Response<AuthResponseDto>
}