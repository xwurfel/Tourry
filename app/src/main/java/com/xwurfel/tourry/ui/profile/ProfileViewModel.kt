package com.xwurfel.tourry.ui.profile

import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
            observeUserProfile(),
            observeUserStats(),
            observeSettings()
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
                    kotlinx.coroutines.delay(500)
                    mockDataManager.signOut()
                    emit(ProfilePartialState.SignedOut)
                    publishEvent(ProfileEvent.NavigateToAuth)
                } catch (e: Exception) {
                    emit(ProfilePartialState.Error("Sign out failed"))
                }
            }

            ProfileIntent.EditProfile -> {
                publishEvent(ProfileEvent.NavigateToEditProfile)
            }

            ProfileIntent.LoadProfile -> {
                emit(ProfilePartialState.Loading)
                loadUserProfileFromSource()
            }

            is ProfileIntent.UpdateProfile -> {
                emit(ProfilePartialState.UpdatingProfile)
                try {
                    val currentUserId = mockDataManager.currentUserId.value
                    if (currentUserId != null) {
                        val success =
                            mockDataManager.updateUserProfile(currentUserId, intent.profile)
                        if (success) {
                            emit(ProfilePartialState.ProfileUpdated)
                            // Track analytics
                            val changedFields =
                                getChangedFields(uiStateSnapshot.value.user, intent.profile)
                            mockDataManager.trackProfileEdit(currentUserId, changedFields)
                        } else {
                            emit(ProfilePartialState.Error("Failed to update profile"))
                        }
                    } else {
                        emit(ProfilePartialState.Error("User not signed in"))
                    }
                } catch (e: Exception) {
                    emit(ProfilePartialState.Error("Failed to update profile: ${e.message}"))
                }
            }

            is ProfileIntent.UploadAvatar -> {
                emit(ProfilePartialState.UploadingAvatar)
                try {
                    val currentUserId = mockDataManager.currentUserId.value
                    if (currentUserId != null) {
                        val avatarUrl =
                            mockDataManager.uploadProfileAvatar(currentUserId, intent.imageUri)
                        if (avatarUrl != null) {
                            emit(ProfilePartialState.AvatarUploaded(avatarUrl))
                        } else {
                            emit(ProfilePartialState.Error("Failed to upload avatar"))
                        }
                    } else {
                        emit(ProfilePartialState.Error("User not signed in"))
                    }
                } catch (e: Exception) {
                    emit(ProfilePartialState.Error("Failed to upload avatar: ${e.message}"))
                }
            }

            is ProfileIntent.UpdateNotificationSettings -> {
                try {
                    val success = mockDataManager.updateNotificationSettings(intent.enabled)
                    if (success) {
                        emit(ProfilePartialState.NotificationSettingsUpdated(intent.enabled))
                    } else {
                        emit(ProfilePartialState.Error("Failed to update notification settings"))
                    }
                } catch (e: Exception) {
                    emit(ProfilePartialState.Error("Failed to update notification settings"))
                }
            }

            is ProfileIntent.UpdateLocationSharing -> {
                try {
                    val success = mockDataManager.updateLocationSharingSettings(intent.enabled)
                    if (success) {
                        emit(ProfilePartialState.LocationSharingUpdated(intent.enabled))
                    } else {
                        emit(ProfilePartialState.Error("Failed to update location settings"))
                    }
                } catch (e: Exception) {
                    emit(ProfilePartialState.Error("Failed to update location settings"))
                }
            }

            ProfileIntent.DeleteAccount -> {
                emit(ProfilePartialState.DeletingAccount)
                try {
                    val currentUserId = mockDataManager.currentUserId.value
                    if (currentUserId != null) {
                        val success = mockDataManager.deleteAccount(currentUserId)
                        if (success) {
                            emit(ProfilePartialState.AccountDeleted)
                            publishEvent(ProfileEvent.NavigateToAuth)
                        } else {
                            emit(ProfilePartialState.Error("Failed to delete account"))
                        }
                    } else {
                        emit(ProfilePartialState.Error("User not signed in"))
                    }
                } catch (e: Exception) {
                    emit(ProfilePartialState.Error("Failed to delete account: ${e.message}"))
                }
            }

            ProfileIntent.ViewAnalytics -> {
                publishEvent(ProfileEvent.NavigateToAnalytics)
            }

            ProfileIntent.ViewHelp -> {
                publishEvent(ProfileEvent.NavigateToHelp)
            }

            ProfileIntent.ShareProfile -> {
                val currentUser = uiStateSnapshot.value.user
                if (currentUser != null) {
                    publishEvent(ProfileEvent.ShareProfile(currentUser))
                }
            }

            ProfileIntent.RefreshProfile -> {
                emit(ProfilePartialState.Loading)
                loadUserProfileFromSource()
            }
        }
    }

    override fun reduceUiState(
        previousState: ProfileUiState,
        partialState: ProfilePartialState
    ): ProfileUiState {
        return when (partialState) {
            ProfilePartialState.Loading -> previousState.copy(
                isLoading = true,
                error = null
            )

            is ProfilePartialState.ProfileLoaded -> previousState.copy(
                user = partialState.user,
                userStats = partialState.stats,
                isLoading = false,
                error = null
            )

            ProfilePartialState.SignedOut -> ProfileUiState()

            ProfilePartialState.UpdatingProfile -> previousState.copy(
                isUpdatingProfile = true,
                error = null
            )

            ProfilePartialState.ProfileUpdated -> previousState.copy(
                isUpdatingProfile = false,
                error = null
            )

            ProfilePartialState.UploadingAvatar -> previousState.copy(
                isUploadingAvatar = true,
                error = null
            )

            is ProfilePartialState.AvatarUploaded -> {
                val updatedUser = previousState.user?.copy(avatarUrl = partialState.avatarUrl)
                previousState.copy(
                    user = updatedUser,
                    isUploadingAvatar = false,
                    error = null
                )
            }

            is ProfilePartialState.NotificationSettingsUpdated -> previousState.copy(
                notificationsEnabled = partialState.enabled,
                error = null
            )

            is ProfilePartialState.LocationSharingUpdated -> previousState.copy(
                locationSharingEnabled = partialState.enabled,
                error = null
            )

            ProfilePartialState.DeletingAccount -> previousState.copy(
                isDeletingAccount = true,
                error = null
            )

            ProfilePartialState.AccountDeleted -> ProfileUiState()

            is ProfilePartialState.SettingsLoaded -> previousState.copy(
                notificationsEnabled = partialState.notificationsEnabled,
                locationSharingEnabled = partialState.locationSharingEnabled
            )

            is ProfilePartialState.Error -> previousState.copy(
                isLoading = false,
                isUpdatingProfile = false,
                isUploadingAvatar = false,
                isDeletingAccount = false,
                error = partialState.message
            )
        }
    }

    private fun observeUserProfile(): Flow<ProfilePartialState> = flow {
        combine(
            mockDataManager.isAuthenticated,
            mockDataManager.currentUserProfile,
            mockDataManager.userStats
        ) { isAuthenticated, profile, stats ->
            if (isAuthenticated && profile != null) {
                ProfilePartialState.ProfileLoaded(profile, stats)
            } else {
                ProfilePartialState.ProfileLoaded(null, null)
            }
        }.collect { partialState ->
            emit(partialState)
        }
    }

    private fun observeUserStats(): Flow<ProfilePartialState> = flow {
        mockDataManager.userStats.collect { stats ->
            // This is handled in observeUserProfile to avoid duplicate emissions
        }
    }

    private fun observeSettings(): Flow<ProfilePartialState> = flow {
        combine(
            mockDataManager.notificationsEnabled,
            mockDataManager.locationSharingEnabled
        ) { notifications, locationSharing ->
            ProfilePartialState.SettingsLoaded(notifications, locationSharing)
        }.collect { partialState ->
            emit(partialState)
        }
    }

    private suspend fun loadUserProfileFromSource() {
        try {
            val currentUserId = mockDataManager.currentUserId.value
            if (currentUserId != null) {
                val profile = mockDataManager.loadUserProfile(currentUserId)
                val stats = mockDataManager.loadUserStats(currentUserId)
                // This will trigger through the observeUserProfile flow
            }
        } catch (e: Exception) {
            // Error handling is done in the flow
        }
    }

    private fun getChangedFields(oldProfile: UserProfile?, newProfile: UserProfile): List<String> {
        if (oldProfile == null) return listOf("name", "email", "bio")

        val changedFields = mutableListOf<String>()

        if (oldProfile.name != newProfile.name) changedFields.add("name")
        if (oldProfile.email != newProfile.email) changedFields.add("email")
        if (oldProfile.bio != newProfile.bio) changedFields.add("bio")
        if (oldProfile.avatarUrl != newProfile.avatarUrl) changedFields.add("avatar")

        return changedFields
    }
}

// Enhanced States
data class ProfileUiState(
    val user: UserProfile? = null,
    val userStats: UserStats? = null,
    val isLoading: Boolean = false,
    val isUpdatingProfile: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val locationSharingEnabled: Boolean = true,
    val error: String? = null
)

sealed interface ProfilePartialState {
    object Loading : ProfilePartialState

    data class ProfileLoaded(
        val user: UserProfile?,
        val stats: UserStats?
    ) : ProfilePartialState

    object SignedOut : ProfilePartialState

    object UpdatingProfile : ProfilePartialState
    object ProfileUpdated : ProfilePartialState

    object UploadingAvatar : ProfilePartialState
    data class AvatarUploaded(val avatarUrl: String) : ProfilePartialState

    data class NotificationSettingsUpdated(val enabled: Boolean) : ProfilePartialState
    data class LocationSharingUpdated(val enabled: Boolean) : ProfilePartialState

    object DeletingAccount : ProfilePartialState
    object AccountDeleted : ProfilePartialState

    data class SettingsLoaded(
        val notificationsEnabled: Boolean,
        val locationSharingEnabled: Boolean
    ) : ProfilePartialState

    data class Error(val message: String) : ProfilePartialState
}

sealed interface ProfileIntent {
    object SignIn : ProfileIntent
    object SignOut : ProfileIntent
    object EditProfile : ProfileIntent
    object LoadProfile : ProfileIntent
    object RefreshProfile : ProfileIntent

    data class UpdateProfile(val profile: UserProfile) : ProfileIntent
    data class UploadAvatar(val imageUri: String) : ProfileIntent

    data class UpdateNotificationSettings(val enabled: Boolean) : ProfileIntent
    data class UpdateLocationSharing(val enabled: Boolean) : ProfileIntent

    object DeleteAccount : ProfileIntent
    object ViewAnalytics : ProfileIntent
    object ViewHelp : ProfileIntent
    object ShareProfile : ProfileIntent
}

sealed interface ProfileEvent {
    object NavigateToAuth : ProfileEvent
    object NavigateToEditProfile : ProfileEvent
    object NavigateToAnalytics : ProfileEvent
    object NavigateToHelp : ProfileEvent
    data class ShareProfile(val user: UserProfile) : ProfileEvent
}

// Enhanced Data models
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