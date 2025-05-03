package com.xwurfel.tourry.presentation.tour.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.domain.auth.service.AuthService
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import com.xwurfel.tourry.domain.booking.service.BookingService
import com.xwurfel.tourry.domain.category.repository.TourCategoryRepository
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.tour.service.TourService
import com.xwurfel.tourry.domain.user.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParticipantInfo(
    val userId: Long,
    val name: String,
    val numberOfParticipants: Int
)

data class TourDetailsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val tour: Tour? = null,
    val organizerName: String? = null,
    val categoryName: String? = null,
    val bookedParticipants: Int = 0,
    val isOrganizerView: Boolean = false,
    val isBooked: Boolean = false,
    val isTourFull: Boolean = false,
    val bookingStatus: String? = null,
    val participants: List<ParticipantInfo> = emptyList()
)

@HiltViewModel
class TourDetailsViewModel @Inject constructor(
    private val tourService: TourService,
    private val categoryRepository: TourCategoryRepository,
    private val userRepository: UserRepository,
    private val bookingService: BookingService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TourDetailsUiState(isLoading = true))
    val uiState: StateFlow<TourDetailsUiState> = _uiState.asStateFlow()

    fun loadTour(tourId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load tour details
                val tour = tourService.getTourById(tourId).first()
                if (tour == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Tour not found"
                        )
                    }
                    return@launch
                }

                // Load category details
                val category = categoryRepository.getCategoryById(tour.categoryId).first()

                // Load organizer details
                val organizer = userRepository.getUserById(tour.organizerId).first()

                // Check if current user is the organizer
                val currentUser = authService.getCurrentUser()
                val isOrganizerView = currentUser?.id == tour.organizerId

                // Load booking information
                val remainingCapacity = tourService.getRemainingCapacity(tourId).getOrThrow()
                val isTourFull = remainingCapacity <= 0
                val bookedParticipants = tour.capacity - remainingCapacity

                var isBooked = false
                var bookingStatus: String? = null
                if (currentUser != null) {
                    val userBookings = bookingService.getBookingsByUser(currentUser.id).first()
                    val tourBooking =
                        userBookings.find { it.tourId == tourId && it.status != BookingStatus.CANCELLED }

                    isBooked = tourBooking != null
                    bookingStatus = tourBooking?.status?.name
                }

                // Load participants (only for organizer view)
                val participants = if (isOrganizerView) {
                    loadParticipants(tourId)
                } else {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tour = tour,
                        categoryName = category?.name,
                        organizerName = organizer?.name,
                        bookedParticipants = bookedParticipants,
                        isOrganizerView = isOrganizerView,
                        isBooked = isBooked,
                        isTourFull = isTourFull,
                        bookingStatus = bookingStatus,
                        participants = participants
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load tour details"
                    )
                }
            }
        }
    }

    private suspend fun loadParticipants(tourId: Long): List<ParticipantInfo> {
        val bookings =
            bookingService.getBookingsByTourAndStatus(tourId, BookingStatus.CONFIRMED).first()
        val participants = mutableListOf<ParticipantInfo>()

        for (booking in bookings) {
            val user = userRepository.getUserById(booking.userId).first() ?: continue
            participants.add(
                ParticipantInfo(
                    userId = user.id,
                    name = user.name,
                    numberOfParticipants = booking.numberOfParticipants
                )
            )
        }

        return participants
    }
}