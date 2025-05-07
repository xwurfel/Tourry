package com.xwurfel.tourry.domain.auth.service

import android.net.Uri
import com.xwurfel.tourry.data.auth.TokenManager
import com.xwurfel.tourry.domain.auth.model.AuthState
import com.xwurfel.tourry.domain.auth.repository.AuthRepository
import com.xwurfel.tourry.domain.user.model.User
import com.xwurfel.tourry.domain.user.model.UserRole
import com.xwurfel.tourry.domain.user.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (tokenManager.getToken() != null) {
                val userId = tokenManager.getUserId()
                if (userId > 0) {
                    val user = userRepository.getUserById(userId).first()
                    user?.let {
                        _authState.value = AuthState(isAuthenticated = true, currentUser = it)
                    }
                }
            }
        }
    }

    suspend fun registerUser(
        email: String, password: String, name: String, role: UserRole = UserRole.TOURIST
    ): Result<User> {
        return try {
            val result = authRepository.register(email, password, name, role)

            if (result.isSuccess) {
                val user = result.getOrNull()
                _authState.value = AuthState(isAuthenticated = true, currentUser = user)
                Result.success(user!!)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Registration failed")
                _authState.value = AuthState(error = error.message)
                Result.failure(error)
            }
        } catch (e: Exception) {
            _authState.value = AuthState(error = e.message)
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = authRepository.login(email, password)

            if (result.isSuccess) {
                val user = result.getOrNull()
                _authState.value = AuthState(isAuthenticated = true, currentUser = user)
                Result.success(user!!)
            } else {
                val error = result.exceptionOrNull() ?: Exception("Login failed")
                _authState.value = AuthState(error = error.message)
                Result.failure(error)
            }
        } catch (e: Exception) {
            _authState.value = AuthState(error = e.message)
            Result.failure(e)
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = AuthState()
    }

    suspend fun getCurrentUser(): User? {
        // If we have a current user in the auth state, return it
        _authState.value.currentUser?.let { return it }

        // Otherwise, check if we have a saved token and try to get the user
        if (authRepository.isAuthenticated()) {
            val userId = tokenManager.getUserId()
            if (userId > 0) {
                val user = userRepository.getUserById(userId).first()
                user?.let {
                    _authState.value = AuthState(isAuthenticated = true, currentUser = it)
                }
                return user
            }
        }

        return null
    }

    fun observeCurrentUser(userId: Long): Flow<User?> {
        return userRepository.getUserById(userId)
    }

    suspend fun refreshAuthState() {
        val user = getCurrentUser()
        _authState.value = AuthState(
            isAuthenticated = user != null,
            currentUser = user,
            error = null
        )
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

    suspend fun checkAndRefreshTokenIfNeeded(): Boolean {
        if (tokenManager.isTokenExpired()) {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                // Implement token refresh logic here
                // This would call the authRepository.refreshToken() method
                // For now, just return false to indicate refresh failed
                return false
            }
            return false
        }
        return true
    }
}