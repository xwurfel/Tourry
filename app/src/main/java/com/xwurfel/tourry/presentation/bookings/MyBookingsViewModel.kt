package com.xwurfel.tourry.presentation.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.domain.auth.service.AuthService
import com.xwurfel.tourry.domain.booking.model.Booking
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import com.xwurfel.tourry.domain.booking.service.BookingService
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.tour.service.TourService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingWithTour(
    val booking: Booking,
    val tour: Tour
)

data class MyBookingsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pendingBookings: List<BookingWithTour> = emptyList(),
    val upcomingBookings: List<BookingWithTour> = emptyList(),
    val pastBookings: List<BookingWithTour> = emptyList(),
    val selectedTab: BookingsTab = BookingsTab.UPCOMING
)

enum class BookingsTab {
    PENDING, UPCOMING, PAST
}

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val authService: AuthService,
    private val bookingService: BookingService,
    private val tourService: TourService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyBookingsUiState())
    val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()

    init {
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val currentUser = authService.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "User not logged in"
                        )
                    }
                    return@launch
                }

                // Get all user bookings
                val allBookings = bookingService.getBookingsByUser(currentUser.id).first()

                // Create lists for each booking status
                val pendingBookings = mutableListOf<BookingWithTour>()
                val upcomingBookings = mutableListOf<BookingWithTour>()
                val pastBookings = mutableListOf<BookingWithTour>()

                // Process each booking
                for (booking in allBookings) {
                    val tour = tourService.getTourById(booking.tourId).first()
                    if (tour != null) {
                        val bookingWithTour = BookingWithTour(booking, tour)

                        when (booking.status) {
                            BookingStatus.PENDING -> pendingBookings.add(bookingWithTour)
                            BookingStatus.CONFIRMED -> upcomingBookings.add(bookingWithTour)
                            BookingStatus.COMPLETED, BookingStatus.CANCELLED -> pastBookings.add(bookingWithTour)
                        }
                    }
                }

                // Sort bookings by date
                pendingBookings.sortByDescending { it.booking.createdAt }
                upcomingBookings.sortBy { it.tour.startDateTime }
                pastBookings.sortByDescending { it.tour.endDateTime }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingBookings = pendingBookings,
                        upcomingBookings = upcomingBookings,
                        pastBookings = pastBookings
                    )
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load bookings"
                    )
                }
            }
        }
    }

    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val result = bookingService.cancelBooking(bookingId)

                if (result.isSuccess) {
                    // Reload bookings after cancellation
                    loadBookings()
                    _uiState.update {
                        it.copy(
                            successMessage = "Booking cancelled successfully"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to cancel booking"
                        )
                    }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to cancel booking"
                    )
                }
            }
        }
    }

    fun selectTab(tab: BookingsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}