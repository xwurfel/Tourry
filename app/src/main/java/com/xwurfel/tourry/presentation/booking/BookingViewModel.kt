package com.xwurfel.tourry.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.domain.auth.service.AuthService
import com.xwurfel.tourry.domain.booking.service.BookingService
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.tour.service.TourService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val bookingError: String? = null,
    val tour: Tour? = null,
    val numberOfParticipants: Int = 1,
    val notes: String = "",
    val totalPrice: Double = 0.0,
    val maxParticipants: Int = 10,
    val isBookingCompleted: Boolean = false
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val tourService: TourService,
    private val bookingService: BookingService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState(isLoading = true))
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    fun loadTourInfo(tourId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val tour = tourService.getTourById(tourId).first()
                if (tour == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false, errorMessage = "Tour not found"
                        )
                    }
                    return@launch
                }

                val remainingCapacity = tourService.getRemainingCapacity(tourId).getOrThrow()
                if (remainingCapacity <= 0) {
                    _uiState.update {
                        it.copy(
                            isLoading = false, errorMessage = "This tour is fully booked"
                        )
                    }
                    return@launch
                }

                val totalPrice = tour.price

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tour = tour,
                        totalPrice = totalPrice,
                        maxParticipants = remainingCapacity,
                        numberOfParticipants = 1
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load tour information"
                    )
                }
            }
        }
    }

    fun onParticipantsChanged(participants: Int) {
        val currentState = _uiState.value
        val tour = currentState.tour ?: return

        val updatedParticipants = participants.coerceIn(1, currentState.maxParticipants)
        val updatedTotalPrice = updatedParticipants * tour.price

        _uiState.update {
            it.copy(
                numberOfParticipants = updatedParticipants, totalPrice = updatedTotalPrice
            )
        }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun bookTour() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val tour = currentState.tour ?: return@launch

            val currentUser = authService.getCurrentUser()
            if (currentUser == null) {
                _uiState.update {
                    it.copy(bookingError = "Please log in to book a tour")
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, bookingError = null) }

            try {
                val result = bookingService.createBooking(
                    tourId = tour.id,
                    userId = currentUser.id,
                    numberOfParticipants = currentState.numberOfParticipants,
                    notes = if (currentState.notes.isBlank()) null else currentState.notes
                )

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false, isBookingCompleted = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            bookingError = result.exceptionOrNull()?.message
                                ?: "Failed to book tour"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false, bookingError = e.message ?: "Failed to book tour"
                    )
                }
            }
        }
    }
}