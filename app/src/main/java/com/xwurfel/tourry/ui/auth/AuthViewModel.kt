package com.xwurfel.tourry.ui.auth

import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val mockDataManager: MockDataManager
) : MviViewModel<AuthUiState, AuthPartialState, AuthEvent, AuthIntent>(
    initialState = AuthUiState()
) {

    override fun mapIntents(intent: AuthIntent): Flow<AuthPartialState> = flow {
        when (intent) {
            AuthIntent.SignInWithGoogle -> {
                emit(AuthPartialState.Loading)
                try {
                    kotlinx.coroutines.delay(1000)
                    val userId = "google_${System.currentTimeMillis()}"
                    mockDataManager.signIn(userId)
                    emit(AuthPartialState.AuthSuccess(userId))
                    publishEvent(AuthEvent.NavigateToMain)
                } catch (e: Exception) {
                    emit(AuthPartialState.Error("Google sign-in failed"))
                }
            }

            AuthIntent.SignInWithEmail -> {
                emit(AuthPartialState.Loading)
                try {
                    kotlinx.coroutines.delay(1000)
                    val userId = "email_${System.currentTimeMillis()}"
                    mockDataManager.signIn(userId)
                    emit(AuthPartialState.AuthSuccess(userId))
                    publishEvent(AuthEvent.NavigateToMain)
                } catch (e: Exception) {
                    emit(AuthPartialState.Error("Email sign-in failed"))
                }
            }

            AuthIntent.ContinueAsGuest -> {
                emit(AuthPartialState.Loading)
                kotlinx.coroutines.delay(500)
                val userId = "guest_${System.currentTimeMillis()}"
                mockDataManager.signIn(userId)
                emit(AuthPartialState.AuthSuccess(userId))
                publishEvent(AuthEvent.NavigateToMain)
            }

            AuthIntent.SignOut -> {
                emit(AuthPartialState.Loading)
                try {
                    kotlinx.coroutines.delay(500)
                    mockDataManager.signOut()
                    emit(AuthPartialState.SignedOut)
                } catch (e: Exception) {
                    emit(AuthPartialState.Error("Sign out failed"))
                }
            }
        }
    }

    override fun reduceUiState(
        previousState: AuthUiState,
        partialState: AuthPartialState
    ): AuthUiState {
        return when (partialState) {
            AuthPartialState.Loading -> previousState.copy(
                isLoading = true,
                error = null
            )

            is AuthPartialState.AuthSuccess -> previousState.copy(
                isLoading = false,
                isAuthenticated = true,
                userId = partialState.userId,
                error = null
            )

            AuthPartialState.SignedOut -> previousState.copy(
                isLoading = false,
                isAuthenticated = false,
                userId = null,
                error = null
            )

            is AuthPartialState.Error -> previousState.copy(
                isLoading = false,
                error = partialState.message
            )
        }
    }
}

// States
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val error: String? = null
)

sealed interface AuthPartialState {
    object Loading : AuthPartialState
    data class AuthSuccess(val userId: String) : AuthPartialState
    object SignedOut : AuthPartialState
    data class Error(val message: String) : AuthPartialState
}

sealed interface AuthIntent {
    object SignInWithGoogle : AuthIntent
    object SignInWithEmail : AuthIntent
    object ContinueAsGuest : AuthIntent
    object SignOut : AuthIntent
}

sealed interface AuthEvent {
    object NavigateToMain : AuthEvent
}