package com.xwurfel.tourry.ui.profile

import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import com.xwurfel.tourry.feature.profile.domain.model.User
import com.xwurfel.tourry.feature.profile.domain.model.UserSettings
import com.xwurfel.tourry.feature.profile.domain.model.UserStats
import com.xwurfel.tourry.feature.profile.domain.usecase.ObserveAuthenticationStateUseCase
import com.xwurfel.tourry.feature.profile.domain.usecase.ObserveCurrentUserUseCase
import com.xwurfel.tourry.feature.profile.domain.usecase.ObserveUserSettingsUseCase
import com.xwurfel.tourry.feature.profile.domain.usecase.ObserveUserStatsUseCase
import com.xwurfel.tourry.feature.profile.domain.usecase.SignOutUseCase
import com.xwurfel.tourry.feature.profile.domain.usecase.UpdateUserSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    private val observeAuthenticationStateUseCase: ObserveAuthenticationStateUseCase,
    private val observeUserStatsUseCase: ObserveUserStatsUseCase,
    private val observeUserSettingsUseCase: ObserveUserSettingsUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val mockDataManager: MockDataManager // Keep for now during transition
) : MviViewModel<ProfileUiState, ProfilePartialState, ProfileEvent, ProfileIntent>(
    initialState = ProfileUiState()
) {

    init {
        observeContinuousChanges(
            observeUserData()
        )
    }

    override fun mapIntents(intent: ProfileIntent): Flow<ProfilePartialState> = flow {
        when (intent) {
            ProfileIntent.SignIn -> {
                publishEvent(ProfileEvent.NavigateToSignIn)
//                emit(ProfilePartialState.Loading(true))
//                try {
//                    val userId = "demo_user_${System.currentTimeMillis()}"
//                    mockDataManager.signIn(userId)
//
//                    emit(ProfilePartialState.Loading(false))
//                } catch (e: Exception) {
//                    emit(ProfilePartialState.Loading(false))
//                    emit(ProfilePartialState.Error("Failed to sign in: ${e.message}"))
//                }
            }

            ProfileIntent.SignOut -> {
                emit(ProfilePartialState.Loading(true))

                signOutUseCase().onSuccess {
                    mockDataManager.signOut()
                    emit(ProfilePartialState.Loading(false))
                }.onFailure { error ->
                    mockDataManager.signOut()
                    emit(ProfilePartialState.Loading(false))
                    emit(ProfilePartialState.Error(error.msg.toString()))
                }
            }

            is ProfileIntent.UpdateNotificationSettings -> {
                val currentSettings = uiStateSnapshot.value.userSettings
                val updatedSettings = currentSettings.copy(notificationsEnabled = intent.enabled)

                updateUserSettingsUseCase(updatedSettings).onFailure { error ->
                    emit(ProfilePartialState.Error(error.msg.toString()))
                }
            }

            is ProfileIntent.UpdateLocationPermission -> {
                val currentSettings = uiStateSnapshot.value.userSettings
                val updatedSettings =
                    currentSettings.copy(locationPermissionGranted = intent.granted)

                updateUserSettingsUseCase(updatedSettings).onFailure { error ->
                    emit(ProfilePartialState.Error(error.msg.toString()))
                }
            }

            ProfileIntent.NavigateToCreateTour -> {
                publishEvent(ProfileEvent.NavigateToCreateTour)
            }

            ProfileIntent.NavigateToMyTours -> {
                publishEvent(ProfileEvent.NavigateToMyTours)
            }
        }
    }

    override fun reduceUiState(
        previousState: ProfileUiState, partialState: ProfilePartialState
    ): ProfileUiState {
        return when (partialState) {
            is ProfilePartialState.Loading -> previousState.copy(
                isLoading = partialState.isLoading, error = null
            )

            is ProfilePartialState.UserDataLoaded -> previousState.copy(
                user = partialState.user,
                userStats = partialState.stats,
                isAuthenticated = partialState.isAuthenticated,
                userSettings = partialState.settings,
                isLoading = false,
                error = null
            )

            is ProfilePartialState.Error -> previousState.copy(
                isLoading = false, error = partialState.message
            )
        }
    }

    private fun observeUserData(): Flow<ProfilePartialState> = combine(
        observeCurrentUserUseCase(),
        observeAuthenticationStateUseCase(),
        observeUserStatsUseCase(),
        observeUserSettingsUseCase(),
        mockDataManager.isAuthenticated,
        mockDataManager.currentUserProfile
    ) { values: Array<Any?> ->
        val firebaseUser = values[0] as User?
        val isFirebaseAuth = values[1] as Boolean
        val firebaseStats = values[2] as UserStats?
        val firebaseSettings = values[3] as UserSettings
        val isMockAuth = values[4] as Boolean
        val mockUser = values[5] as User?

        val user = firebaseUser ?: mockUser
        val isAuthenticated = isFirebaseAuth || isMockAuth
        val stats = firebaseStats ?: user?.stats
        val settings = firebaseSettings

        ProfilePartialState.UserDataLoaded(
            user = user, isAuthenticated = isAuthenticated, stats = stats, settings = settings
        )
    }
}

// States remain the same
data class ProfileUiState(
    val user: User? = null,
    val userStats: UserStats? = null,
    val userSettings: UserSettings = UserSettings(),
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ProfilePartialState {
    data class Loading(val isLoading: Boolean) : ProfilePartialState

    data class UserDataLoaded(
        val user: User?,
        val isAuthenticated: Boolean,
        val stats: UserStats?,
        val settings: UserSettings
    ) : ProfilePartialState

    data class Error(val message: String) : ProfilePartialState
}

sealed interface ProfileIntent {
    object SignIn : ProfileIntent
    object SignOut : ProfileIntent
    data class UpdateNotificationSettings(val enabled: Boolean) : ProfileIntent
    data class UpdateLocationPermission(val granted: Boolean) : ProfileIntent
    object NavigateToCreateTour : ProfileIntent
    object NavigateToMyTours : ProfileIntent
}

sealed interface ProfileEvent {
    data object NavigateToCreateTour : ProfileEvent
    data object NavigateToMyTours : ProfileEvent
    data object NavigateToSignIn : ProfileEvent
}