package com.xwurfel.tourry.feature.auth.api

import com.xwurfel.tourry.feature.auth.api.model.AuthResponse
import com.xwurfel.tourry.feature.auth.api.model.LoginRequest
import com.xwurfel.tourry.feature.auth.api.model.RegisterRequest
import com.xwurfel.tourry.feature.auth.api.model.UserDto
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthApi2 @Inject constructor() {
    suspend fun login(request: LoginRequest): Response<AuthResponse> {
        return Response.success(
            AuthResponse(
                token = "token",
                user = UserDto(
                    id = "id",
                    email = "email",
                    name = "name",
                    profilePictureUrl = null
                )
            )
        )
    }

    suspend fun register(request: RegisterRequest): Response<AuthResponse> = Response.success(
        AuthResponse(
            token = "token",
            user = UserDto(
                id = "id",
                email = "email",
                name = "name",
                profilePictureUrl = null
            )
        )
    )

    suspend fun logout(): Response<Unit> =
        Response.success(Unit)

}