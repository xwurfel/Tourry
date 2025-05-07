package com.xwurfel.tourry.domain.auth.service

import android.net.Uri
import com.xwurfel.tourry.domain.auth.model.AuthState
import com.xwurfel.tourry.domain.user.model.User
import com.xwurfel.tourry.domain.user.model.UserRole
import com.xwurfel.tourry.domain.user.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val userRepository: UserRepository,
) {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun registerUser(
        email: String, password: String, name: String, role: UserRole = UserRole.TOURIST
    ): Result<User> {
        return try {
            val existingUser = userRepository.getUserByEmail(email).first()
            if (existingUser != null) {
                Result.failure(IllegalArgumentException("User with this email already exists"))
            } else {
                val newUser = User(
                    email = email,
                    name = name,
                    bio = null,
                    profileImageUri = null,
                    phoneNumber = null,
                    role = role
                )
                val userId = userRepository.registerUser(newUser, password)
                val createdUser = userRepository.getUserById(userId).first()
                    ?: return Result.failure(Exception("Failed to create user"))

                Result.success(createdUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val user = userRepository.authenticateUser(email, password) ?: return Result.failure(
                IllegalArgumentException("Invalid credentials")
            )

            _authState.value = AuthState(isAuthenticated = true, currentUser = user)
            Result.success(user)
        } catch (e: Exception) {
            _authState.value = AuthState(error = e.message)
            Result.failure(e)
        }
    }

    fun logout() {
        _authState.value = AuthState()
    }

    suspend fun getCurrentUser(): User? {
        val currentUserId = _authState.value.currentUser?.id ?: return null
        return userRepository.getUserById(currentUserId).first()
    }

    fun observeCurrentUser(userId: Long): Flow<User?> {
        return userRepository.getUserById(userId)
    }

    suspend fun updateUserProfile(
        userId: Long,
        name: String? = null,
        bio: String? = null,
        phoneNumber: String? = null,
        newPassword: String? = null,
        profileImageUri: Uri? = null
    ): Result<User> {
        return try {
            val currentUser = userRepository.getUserById(userId).first() ?: return Result.failure(
                IllegalArgumentException("User not found")
            )

            val updatedUser = currentUser.copy(
                name = name ?: currentUser.name,
                bio = bio ?: currentUser.bio,
                phoneNumber = phoneNumber ?: currentUser.phoneNumber,
                profileImageUri = profileImageUri ?: currentUser.profileImageUri
            )

            userRepository.updateUser(updatedUser, newPassword)
            val refreshedUser = userRepository.getUserById(userId).first() ?: return Result.failure(
                Exception("Failed to update user")
            )

            // Update auth state if the updated user is the current user
            if (_authState.value.currentUser?.id == userId) {
                _authState.value = _authState.value.copy(currentUser = refreshedUser)
            }

            Result.success(refreshedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: Long, newRole: UserRole): Result<Unit> {
        return try {
            userRepository.updateUserRole(userId, newRole)

            // Update auth state if the updated user is the current user
            if (_authState.value.currentUser?.id == userId) {
                val refreshedUser = userRepository.getUserById(userId).first()
                refreshedUser?.let {
                    _authState.value = _authState.value.copy(currentUser = it)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}