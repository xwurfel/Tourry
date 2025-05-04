package com.xwurfel.tourry.data.network.dto

import java.time.LocalDateTime

data class LoginDto(
    val email: String,
    val password: String
)

data class RegisterDto(
    val email: String,
    val password: String,
    val name: String,
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val phoneNumber: String? = null,
    val role: String? = null
)

data class AuthResponseDto(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
    val bio: String?,
    val profileImageUrl: String?,
    val phoneNumber: String?,
    val role: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class UpdateUserDto(
    val name: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val phoneNumber: String?,
    val password: String?
)