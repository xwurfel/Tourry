package com.xwurfel.tourry.presentation.tour.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.domain.auth.service.AuthService
import com.xwurfel.tourry.domain.category.model.TourCategory
import com.xwurfel.tourry.domain.category.repository.TourCategoryRepository
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.tour.service.TourService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

private const val DEFAULT_HOURS = 2L

data class EditTourUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isNewTour: Boolean = true,
    val tour: Tour? = null,
    val title: String = "",
    val description: String = "",
    val imageUri: Uri? = null,
    val meetingPoint: LatLng = LatLng(0.0, 0.0),
    val meetingPointAddress: String = "",
    val startDateTime: LocalDateTime = LocalDateTime.now().plusDays(1),
    val endDateTime: LocalDateTime = LocalDateTime.now().plusDays(1).plusHours(2),
    val price: Double = 0.0,
    val capacity: Int = 1,
    val selectedCategoryId: Long = 1,
    val categories: List<TourCategory> = emptyList(),
    val isSaved: Boolean = false
)

@HiltViewModel
class EditTourViewModel @Inject constructor(
    private val authService: AuthService,
    private val tourService: TourService,
    private val categoryRepository: TourCategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditTourUiState())
    val uiState: StateFlow<EditTourUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadTour(tourId: Long?) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                if (tourId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNewTour = true
                        )
                    }
                } else {
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

                    val currentUser = authService.getCurrentUser()
                    if (currentUser == null || currentUser.id != tour.organizerId) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "You don't have permission to edit this tour"
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNewTour = false,
                            tour = tour,
                            title = tour.title,
                            description = tour.description,
                            imageUri = tour.imageUri,
                            meetingPoint = tour.meetingPoint,
                            meetingPointAddress = tour.meetingPointAddress,
                            startDateTime = tour.startDateTime,
                            endDateTime = tour.endDateTime,
                            price = tour.price,
                            capacity = tour.capacity,
                            selectedCategoryId = tour.categoryId
                        )
                    }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load tour"
                    )
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().collectLatest { categories ->
                    _uiState.update {
                        it.copy(
                            categories = categories,
                            selectedCategoryId = if (categories.isNotEmpty()) categories.first().id else 1
                        )
                    }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to load categories"
                    )
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onImageSelected(imageUri: Uri) {
        _uiState.update { it.copy(imageUri = imageUri) }
    }

    fun onMeetingPointChanged(meetingPoint: LatLng) {
        _uiState.update { it.copy(meetingPoint = meetingPoint) }
    }

    fun onMeetingPointAddressChanged(address: String) {
        _uiState.update { it.copy(meetingPointAddress = address) }
    }

    fun onStartDateTimeChanged(dateTime: LocalDateTime) {
        val currentEndDateTime = _uiState.value.endDateTime
        val updatedEndDateTime = if (dateTime.isAfter(currentEndDateTime)) {
            dateTime.plusHours(DEFAULT_HOURS)
        } else {
            currentEndDateTime
        }

        _uiState.update {
            it.copy(
                startDateTime = dateTime,
                endDateTime = updatedEndDateTime
            )
        }
    }

    fun onEndDateTimeChanged(dateTime: LocalDateTime) {
        _uiState.update { it.copy(endDateTime = dateTime) }
    }

    fun onPriceChanged(price: Double) {
        _uiState.update { it.copy(price = price) }
    }

    fun onCapacityChanged(capacity: Int) {
        _uiState.update { it.copy(capacity = capacity) }
    }

    fun onCategorySelected(categoryId: Long) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun saveTour() {
        val currentState = _uiState.value

        // Validate inputs
        if (currentState.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be empty") }
            return
        }

        if (currentState.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Description cannot be empty") }
            return
        }

        if (currentState.meetingPointAddress.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Meeting point address cannot be empty") }
            return
        }

        if (currentState.price < 0) {
            _uiState.update { it.copy(errorMessage = "Price cannot be negative") }
            return
        }

        if (currentState.capacity <= 0) {
            _uiState.update { it.copy(errorMessage = "Capacity must be greater than zero") }
            return
        }

        if (currentState.startDateTime.isBefore(LocalDateTime.now())) {
            _uiState.update { it.copy(errorMessage = "Start date cannot be in the past") }
            return
        }

        if (currentState.endDateTime.isBefore(currentState.startDateTime)) {
            _uiState.update { it.copy(errorMessage = "End date cannot be before start date") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val currentUser = authService.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "You must be logged in to create a tour"
                        )
                    }
                    return@launch
                }

                val result = if (currentState.isNewTour) {
                    tourService.createTour(
                        title = currentState.title,
                        description = currentState.description,
                        meetingPoint = currentState.meetingPoint,
                        meetingPointAddress = currentState.meetingPointAddress,
                        startDateTime = currentState.startDateTime,
                        endDateTime = currentState.endDateTime,
                        price = currentState.price,
                        capacity = currentState.capacity,
                        categoryId = currentState.selectedCategoryId,
                        organizerId = currentUser.id
                    )
                } else {
                    val tourId = currentState.tour?.id ?: return@launch

                    tourService.updateTour(
                        tourId = tourId,
                        title = currentState.title,
                        description = currentState.description,
                        imageUri = currentState.imageUri,
                        meetingPoint = currentState.meetingPoint,
                        meetingPointAddress = currentState.meetingPointAddress,
                        startDateTime = currentState.startDateTime,
                        endDateTime = currentState.endDateTime,
                        price = currentState.price,
                        capacity = currentState.capacity,
                        categoryId = currentState.selectedCategoryId
                    )
                }

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            successMessage = if (currentState.isNewTour) "Tour created successfully" else "Tour updated successfully"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to save tour"
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

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}