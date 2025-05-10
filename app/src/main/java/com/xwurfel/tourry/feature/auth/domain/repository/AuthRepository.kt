package com.xwurfel.tourry.feature.auth.domain.repository

import com.xwurfel.tourry.feature.auth.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String): Result<User>
    suspend fun logout()
    fun observeCurrentUser(): Flow<User?>
    suspend fun getCurrentUser(): User?
    suspend fun isLoggedIn(): Boolean
}