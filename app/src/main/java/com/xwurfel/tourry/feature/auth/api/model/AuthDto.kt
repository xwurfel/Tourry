package com.xwurfel.tourry.feature.auth.api.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val profilePictureUrl: String?
)

data class AuthResponse(
    val user: UserDto,
    val token: String
)