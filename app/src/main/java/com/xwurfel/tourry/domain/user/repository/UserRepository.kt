package com.xwurfel.tourry.domain.user.repository

import com.xwurfel.tourry.domain.user.model.User
import com.xwurfel.tourry.domain.user.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    fun getUserById(id: Long): Flow<User?>

    fun getUserByEmail(email: String): Flow<User?>

    fun getAllUsers(): Flow<List<User>>

    fun getUsersByRole(role: UserRole): Flow<List<User>>

    suspend fun updateUserRole(userId: Long, role: UserRole)

    suspend fun updateUser(user: User, newPassword: String? = null)

    suspend fun deleteUser(id: Long)
}