package com.xwurfel.tourry.domain.auth.repository

import com.xwurfel.tourry.domain.auth.model.AuthState
import com.xwurfel.tourry.domain.user.model.User
import com.xwurfel.tourry.domain.user.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>

    suspend fun login(email: String, password: String): Result<User>

    suspend fun register(
        email: String,
        password: String,
        name: String,
        role: UserRole
    ): Result<User>

    fun logout()

    suspend fun isAuthenticated(): Boolean
}