package com.xwurfel.tourry.ui.profile

import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val mockDataManager: MockDataManager
) : MviViewModel<ProfileUiState, ProfilePartialState, ProfileEvent, ProfileIntent>(
    initialState = ProfileUiState()
) {

    init {
        observeContinuousChanges(
            loadUserProfile()
        )
    }

    override fun mapIntents(intent: ProfileIntent): Flow<ProfilePartialState> = flow {
        when (intent) {
            ProfileIntent.SignIn -> {
                publishEvent(ProfileEvent.NavigateToAuth)
            }

            ProfileIntent.SignOut -> {
                emit(ProfilePartialState.Loading)
                try {
                    // TODO: Implement sign out
                    kotlinx.coroutines.delay(500)
                    emit(ProfilePartialState.SignedOut)
                    publishEvent(ProfileEvent.NavigateToAuth)
                } catch (e: Exception) {
                    emit(ProfilePartialState.Error("Sign out failed"))
                }
            }

            ProfileIntent.EditProfile -> {
                // TODO: Navigate to edit profile screen
            }

            ProfileIntent.LoadProfile -> {
                emit(ProfilePartialState.Loading)
                // TODO: Reload profile from repository
            }
        }
    }

    override fun reduceUiState(
        previousState: ProfileUiState,
        partialState: ProfilePartialState
    ): ProfileUiState {
        return when (partialState) {
            ProfilePartialState.Loading -> previousState.copy(isLoading = true)

            is ProfilePartialState.ProfileLoaded -> previousState.copy(
                user = partialState.user,
                userStats = partialState.stats,
                isLoading = false
            )

            ProfilePartialState.SignedOut -> ProfileUiState()

            is ProfilePartialState.Error -> previousState.copy(
                isLoading = false,
                error = partialState.message
            )
        }
    }

    private fun loadUserProfile(): Flow<ProfilePartialState> = flow {
        emit(ProfilePartialState.Loading)
        try {
            // TODO: Load from repository
            // For now, emit mock data or null (guest)
            kotlinx.coroutines.delay(1000)

            // Simulate guest user
            emit(ProfilePartialState.ProfileLoaded(null, null))

            // Uncomment for mock signed-in user:
            /*
            val mockUser = UserProfile(
                id = "1",
                name = "John Doe",
                email = "john@example.com",
                avatarUrl = null,
                bio = "Tour enthusiast and local guide",
                rating = 4.8f,
                reviewsCount = 23
            )
            val mockStats = UserStats(
                toursCreated = 5,
                toursJoined = 12,
                totalParticipants = 67
            )
            emit(ProfilePartialState.ProfileLoaded(mockUser, mockStats))
            */
        } catch (e: Exception) {
            emit(ProfilePartialState.Error("Failed to load profile"))
        }
    }
}

// States
data class ProfileUiState(
    val user: UserProfile? = null,
    val userStats: UserStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface ProfilePartialState {
    object Loading : ProfilePartialState
    data class ProfileLoaded(
        val user: UserProfile?,
        val stats: UserStats?
    ) : ProfilePartialState

    object SignedOut : ProfilePartialState
    data class Error(val message: String) : ProfilePartialState
}

sealed interface ProfileIntent {
    object SignIn : ProfileIntent
    object SignOut : ProfileIntent
    object EditProfile : ProfileIntent
    object LoadProfile : ProfileIntent
}

sealed interface ProfileEvent {
    object NavigateToAuth : ProfileEvent
}

// Data models
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val bio: String = "",
    val rating: Float = 0f,
    val reviewsCount: Int = 0
)

data class UserStats(
    val toursCreated: Int,
    val toursJoined: Int,
    val totalParticipants: Int
)