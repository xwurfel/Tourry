package com.xwurfel.tourry.feature.auth.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xwurfel.tourry.core.network.interceptor.NoConnectivityException
import com.xwurfel.tourry.feature.auth.api.AuthApi
import com.xwurfel.tourry.feature.auth.api.model.LoginRequest
import com.xwurfel.tourry.feature.auth.api.model.RegisterRequest
import com.xwurfel.tourry.feature.auth.domain.model.User
import com.xwurfel.tourry.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val dataStore: DataStore<Preferences>
) : AuthRepository {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_PICTURE_KEY = stringPreferencesKey("user_picture")
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = authApi.login(LoginRequest(email, password))

            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    // Save auth token and user data
                    dataStore.edit { preferences ->
                        preferences[TOKEN_KEY] = authResponse.token
                        preferences[USER_ID_KEY] = authResponse.user.id
                        preferences[USER_EMAIL_KEY] = authResponse.user.email
                        preferences[USER_NAME_KEY] = authResponse.user.name
                        authResponse.user.profilePictureUrl?.let {
                            preferences[USER_PICTURE_KEY] = it
                        }
                    }

                    Result.success(
                        User(
                            id = authResponse.user.id,
                            email = authResponse.user.email,
                            name = authResponse.user.name,
                            profilePictureUrl = authResponse.user.profilePictureUrl
                        )
                    )
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("Login failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String): Result<User> {
        return try {
            val response = authApi.register(RegisterRequest(name, email, password))

            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    // Save auth token and user data
                    dataStore.edit { preferences ->
                        preferences[TOKEN_KEY] = authResponse.token
                        preferences[USER_ID_KEY] = authResponse.user.id
                        preferences[USER_EMAIL_KEY] = authResponse.user.email
                        preferences[USER_NAME_KEY] = authResponse.user.name
                        authResponse.user.profilePictureUrl?.let {
                            preferences[USER_PICTURE_KEY] = it
                        }
                    }

                    Result.success(
                        User(
                            id = authResponse.user.id,
                            email = authResponse.user.email,
                            name = authResponse.user.name,
                            profilePictureUrl = authResponse.user.profilePictureUrl
                        )
                    )
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("Registration failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        try {
            authApi.logout()
        } catch (e: Exception) {
            // Even if the API call fails, clear local data
        } finally {
            dataStore.edit { preferences ->
                preferences.remove(TOKEN_KEY)
                preferences.remove(USER_ID_KEY)
                preferences.remove(USER_EMAIL_KEY)
                preferences.remove(USER_NAME_KEY)
                preferences.remove(USER_PICTURE_KEY)
            }
        }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return dataStore.data.map { preferences ->
            val userId = preferences[USER_ID_KEY] ?: return@map null
            val email = preferences[USER_EMAIL_KEY] ?: return@map null
            val name = preferences[USER_NAME_KEY] ?: return@map null
            val profilePictureUrl = preferences[USER_PICTURE_KEY]

            User(
                id = userId, email = email, name = name, profilePictureUrl = profilePictureUrl
            )
        }
    }

    override suspend fun getCurrentUser(): User? {
        return observeCurrentUser().firstOrNull()
    }

    override suspend fun isLoggedIn(): Boolean {
        val preferences = dataStore.data.firstOrNull()
        return preferences?.get(TOKEN_KEY) != null
    }
}