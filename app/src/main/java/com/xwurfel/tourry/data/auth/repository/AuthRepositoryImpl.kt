package com.xwurfel.tourry.data.auth.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.xwurfel.tourry.data.auth.TokenManager
import com.xwurfel.tourry.data.network.api.AuthApi
import com.xwurfel.tourry.data.network.dto.LoginDto
import com.xwurfel.tourry.data.network.dto.RegisterDto
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.auth.model.AuthState
import com.xwurfel.tourry.domain.auth.repository.AuthRepository
import com.xwurfel.tourry.domain.user.model.User
import com.xwurfel.tourry.domain.user.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    private val _authState = MutableStateFlow(AuthState())
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    override suspend fun register(
        email: String,
        password: String,
        name: String,
        role: UserRole
    ): Result<User> = withContext(ioDispatcher) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return@withContext Result.failure(Exception("No internet connection"))
        }

        val registerDto = RegisterDto(
            email = email,
            password = password,
            name = name,
            role = role.name
        )

        when (val response = NetworkUtils.safeApiCall {
            authApi.register(registerDto)
        }) {
            is ApiResponse.Success -> {
                val authResponse = response.data
                tokenManager.saveToken(authResponse.token)
                tokenManager.saveUserId(authResponse.user.id)

                // Convert UserDto to User domain model
                val user = User(
                    id = authResponse.user.id,
                    email = authResponse.user.email,
                    name = authResponse.user.name,
                    bio = authResponse.user.bio,
                    profileImageUri = authResponse.user.profileImageUrl?.let { Uri.parse(it) },
                    phoneNumber = authResponse.user.phoneNumber,
                    role = UserRole.valueOf(authResponse.user.role),
                    createdAt = authResponse.user.createdAt,
                    updatedAt = authResponse.user.updatedAt
                )

                Result.success(user)
            }

            is ApiResponse.Error -> {
                Result.failure(Exception(response.message ?: "Registration failed"))
            }

            ApiResponse.Loading -> {
                Result.failure(Exception("Request is still loading"))
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<User> =
        withContext(ioDispatcher) {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                return@withContext Result.failure(Exception("No internet connection"))
            }

            when (val response = NetworkUtils.safeApiCall {
                authApi.login(LoginDto(email, password))
            }) {
                is ApiResponse.Success -> {
                    val authResponse = response.data
                    tokenManager.saveToken(authResponse.token)
                    tokenManager.saveUserId(authResponse.user.id)

                    // Convert UserDto to User domain model
                    val user = User(
                        id = authResponse.user.id,
                        email = authResponse.user.email,
                        name = authResponse.user.name,
                        bio = authResponse.user.bio,
                        profileImageUri = authResponse.user.profileImageUrl?.toUri(),
                        phoneNumber = authResponse.user.phoneNumber,
                        role = UserRole.valueOf(authResponse.user.role),
                        createdAt = authResponse.user.createdAt,
                        updatedAt = authResponse.user.updatedAt
                    )

                    Result.success(user)
                }

                is ApiResponse.Error -> {
                    Result.failure(Exception(response.message ?: "Authentication failed"))
                }

                ApiResponse.Loading -> {
                    Result.failure(Exception("Request is still loading"))
                }
            }
        }

    override fun logout() {
        tokenManager.clearTokens()
    }

    override suspend fun isAuthenticated(): Boolean {
        return tokenManager.getToken() != null
    }

    override suspend fun refreshToken(refreshToken: String): Result<String> =
        withContext(ioDispatcher) {
            // This would be implemented to call the token refresh endpoint
            // For now, return a failure
            Result.failure(Exception("Token refresh not implemented"))
        }
}