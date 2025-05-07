package com.xwurfel.tourry.data.network.api

import com.xwurfel.tourry.data.network.dto.UpdateRoleDto
import com.xwurfel.tourry.data.network.dto.UpdateUserDto
import com.xwurfel.tourry.data.network.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface UserApi {
    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserDto>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<UserDto>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: Long,
        @Body updateUserDto: UpdateUserDto
    ): Response<UserDto>

    @PUT("users/{id}/role")
    suspend fun updateUserRole(
        @Path("id") id: Long,
        @Body updateRoleDto: UpdateRoleDto
    ): Response<UserDto>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Long): Response<Unit>

    @Multipart
    @POST("users/{id}/profile-image")
    suspend fun uploadProfileImage(
        @Path("id") userId: Long,
        @Part image: MultipartBody.Part
    ): Response<UserDto>
}