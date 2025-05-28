package com.xwurfel.tourry.ui.auth

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import com.xwurfel.tourry.feature.profile.domain.usecase.CreateAccountUseCase
import com.xwurfel.tourry.feature.profile.domain.usecase.SendPasswordResetUseCase
import com.xwurfel.tourry.feature.profile.domain.usecase.SignInWithEmailUseCase
import com.xwurfel.tourry.feature.profile.domain.usecase.SignInWithGoogleUseCase
import com.xwurfel.tourry.ui.auth.AuthPartialState.AuthSuccess
import com.xwurfel.tourry.ui.auth.AuthPartialState.Error
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val googleSignInClient: GoogleSignInClient,
    private val mockDataManager: MockDataManager,
) : MviViewModel<AuthUiState, AuthPartialState, AuthEvent, AuthIntent>(
    initialState = AuthUiState()
) {

    override fun mapIntents(intent: AuthIntent): Flow<AuthPartialState> = flow {
        when (intent) {
            AuthIntent.SignInWithGoogle -> {
                emit(AuthPartialState.Loading)
                // This will be handled by the Activity result
                emit(AuthPartialState.LaunchGoogleSignIn)
            }

            is AuthIntent.GoogleSignInResult -> {
                emit(AuthPartialState.Loading)
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(intent.data)
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken

                    if (idToken != null) {
                        signInWithGoogleUseCase(idToken)
                            .onSuccess { user ->
                                emit(AuthSuccess(user.id, user.name))
                                publishEvent(AuthEvent.NavigateToMain)
                            }
                            .onFailure { error ->
                                emit(Error(error.msg.toString()))
                            }
                    } else {
                        emit(Error("Google sign-in failed - no ID token"))
                    }
                } catch (e: ApiException) {
                    Timber.e(e, "Google sign-in failed")
                    emit(Error("Google sign-in failed: ${e.message}"))
                }
            }

            is AuthIntent.SignInWithEmail -> {
                emit(AuthPartialState.Loading)
                signInWithEmailUseCase(intent.email, intent.password)
                    .onSuccess { user ->
                        emit(AuthSuccess(user.id, user.name))
                        publishEvent(AuthEvent.NavigateToMain)
                    }
                    .onFailure { error ->
                        emit(Error(error.msg.toString()))
                    }
            }

            is AuthIntent.CreateAccount -> {
                emit(AuthPartialState.Loading)
                createAccountUseCase(intent.email, intent.password, intent.name)
                    .onSuccess { user ->
                        emit(AuthSuccess(user.id, user.name))
                        publishEvent(AuthEvent.NavigateToMain)
                    }
                    .onFailure { error ->
                        emit(Error(error.msg.toString()))
                    }
            }

            is AuthIntent.SendPasswordReset -> {
                emit(AuthPartialState.Loading)
                sendPasswordResetUseCase(intent.email)
                    .onSuccess {
                        emit(AuthPartialState.PasswordResetSent)
                    }
                    .onFailure { error ->
                        emit(Error(error.msg.toString()))
                    }
            }

            AuthIntent.ContinueAsGuest -> {
                emit(AuthPartialState.Loading)
                delay(500)
                val userId = "guest_${System.currentTimeMillis()}"
                mockDataManager.signIn(userId)
                emit(AuthSuccess(userId, "Guest User"))
                publishEvent(AuthEvent.NavigateToMain)
            }

            AuthIntent.ClearError -> {
                emit(AuthPartialState.ErrorCleared)
            }

            AuthIntent.GoogleSignInLaunched -> {
                // TODO: react if needed
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

            AuthPartialState.LaunchGoogleSignIn -> previousState.copy(
                isLoading = false,
                shouldLaunchGoogleSignIn = true
            )

            is AuthPartialState.AuthSuccess -> previousState.copy(
                isLoading = false,
                isAuthenticated = true,
                userId = partialState.userId,
                userName = partialState.userName,
                error = null,
                shouldLaunchGoogleSignIn = false
            )

            AuthPartialState.PasswordResetSent -> previousState.copy(
                isLoading = false,
                passwordResetSent = true,
                error = null
            )

            is AuthPartialState.Error -> previousState.copy(
                isLoading = false,
                error = partialState.message,
                shouldLaunchGoogleSignIn = false
            )

            AuthPartialState.ErrorCleared -> previousState.copy(
                error = null,
                passwordResetSent = false
            )
        }
    }

    fun onGoogleSignInLaunched() {
        // Google Sign-In launched, handled by UI state
    }
}

// Updated States
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userId: String? = null,
    val userName: String? = null,
    val shouldLaunchGoogleSignIn: Boolean = false,
    val passwordResetSent: Boolean = false,
    val error: String? = null
)

sealed interface AuthPartialState {
    object Loading : AuthPartialState
    object LaunchGoogleSignIn : AuthPartialState
    data class AuthSuccess(val userId: String, val userName: String) : AuthPartialState
    object PasswordResetSent : AuthPartialState
    data class Error(val message: String) : AuthPartialState
    object ErrorCleared : AuthPartialState
}

sealed interface AuthIntent {
    object SignInWithGoogle : AuthIntent
    data class GoogleSignInResult(val data: android.content.Intent?) : AuthIntent
    data object GoogleSignInLaunched : AuthIntent
    data class SignInWithEmail(val email: String, val password: String) : AuthIntent
    data class CreateAccount(val email: String, val password: String, val name: String) : AuthIntent
    data class SendPasswordReset(val email: String) : AuthIntent
    object ContinueAsGuest : AuthIntent
    object ClearError : AuthIntent
}

sealed interface AuthEvent {
    object NavigateToMain : AuthEvent
}