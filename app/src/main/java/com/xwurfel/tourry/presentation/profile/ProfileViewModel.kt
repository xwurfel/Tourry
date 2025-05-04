package com.xwurfel.tourry.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.domain.auth.service.AuthService
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import com.xwurfel.tourry.domain.booking.service.BookingService
import com.xwurfel.tourry.domain.tour.service.TourService
import com.xwurfel.tourry.domain.user.model.User
import com.xwurfel.tourry.domain.user.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val user: User? = null,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editBio: String = "",
    val editPhoneNumber: String = "",
    val editProfileImage: Uri? = null,
    val isPendingBookingsExpanded: Boolean = false,
    val isUpcomingBookingsExpanded: Boolean = false,
    val isPastBookingsExpanded: Boolean = false,
    val pendingBookingsCount: Int = 0,
    val upcomingBookingsCount: Int = 0,
    val pastBookingsCount: Int = 0,
    val totalToursCreated: Int = 0,
    val totalBookingsCount: Int = 0,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authService: AuthService,
    private val bookingService: BookingService,
    private val tourService: TourService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val currentUser = authService.getCurrentUser()

                if (currentUser != null) {
                    authService.observeCurrentUser(currentUser.id).collectLatest { user ->
                        if (user != null) {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    user = user,
                                    editName = user.name,
                                    editBio = user.bio ?: "",
                                    editPhoneNumber = user.phoneNumber ?: "",
                                    editProfileImage = user.profileImageUri
                                )
                            }

                            loadBookingStatistics(user.id)

                            if (user.role == UserRole.GUIDE) {
                                loadTourStatistics(user.id)
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false, errorMessage = "User not found"
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false, errorMessage = "Not logged in"
                        )
                    }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    private fun loadBookingStatistics(userId: Long) {
        viewModelScope.launch {
            try {
                val allUserBookings = bookingService.getBookingsByUser(userId).first()

                val pendingBookings = allUserBookings.filter { it.status == BookingStatus.PENDING }
                val upcomingBookings =
                    allUserBookings.filter { it.status == BookingStatus.CONFIRMED }
                val pastBookings = allUserBookings.filter {
                    it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CANCELLED
                }

                _uiState.update {
                    it.copy(
                        pendingBookingsCount = pendingBookings.size,
                        upcomingBookingsCount = upcomingBookings.size,
                        pastBookingsCount = pastBookings.size,
                        totalBookingsCount = allUserBookings.size
                    )
                }
            } catch (_: Exception) {
                ensureActive()
            }
        }
    }

    private fun loadTourStatistics(userId: Long) {
        viewModelScope.launch {
            try {
                val userTours = tourService.getToursByOrganizer(userId).first()

                _uiState.update {
                    it.copy(
                        totalToursCreated = userTours.size
                    )
                }
            } catch (_: Exception) {
                ensureActive()
            }
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun cancelEditing() {
        val currentUser = _uiState.value.user ?: return

        _uiState.update {
            it.copy(
                isEditing = false,
                editName = currentUser.name,
                editBio = currentUser.bio ?: "",
                editPhoneNumber = currentUser.phoneNumber ?: "",
                editProfileImage = currentUser.profileImageUri
            )
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun onBioChange(bio: String) {
        _uiState.update { it.copy(editBio = bio) }
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        _uiState.update { it.copy(editPhoneNumber = phoneNumber) }
    }

    fun onProfileImageSelected(uri: Uri) {
        _uiState.update { it.copy(editProfileImage = uri) }
    }

    fun saveProfile() {
        val currentState = _uiState.value
        val currentUser = currentState.user ?: return

        if (currentState.editName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val result = authService.updateUserProfile(
                    userId = currentUser.id,
                    name = currentState.editName,
                    bio = currentState.editBio.takeIf { it.isNotBlank() },
                    phoneNumber = currentState.editPhoneNumber.takeIf { it.isNotBlank() },
                    profileImageUri = currentState.editProfileImage
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = false,
                            successMessage = "Profile updated successfully"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull()?.message
                                ?: "Failed to update profile"
                        )
                    }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    fun togglePendingBookingsExpanded() {
        _uiState.update { it.copy(isPendingBookingsExpanded = !it.isPendingBookingsExpanded) }
    }

    fun toggleUpcomingBookingsExpanded() {
        _uiState.update { it.copy(isUpcomingBookingsExpanded = !it.isUpcomingBookingsExpanded) }
    }

    fun togglePastBookingsExpanded() {
        _uiState.update { it.copy(isPastBookingsExpanded = !it.isPastBookingsExpanded) }
    }

    fun logout() {
        authService.logout()
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}