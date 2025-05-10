package com.xwurfel.tourry.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.feature.discovery.domain.model.TourDetails
import com.xwurfel.tourry.feature.discovery.domain.repository.TourDiscoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TourDetailsState(
    val isLoading: Boolean = false,
    val tourDetails: TourDetails? = null,
    val error: String? = null
)

@HiltViewModel
class TourDetailsViewModel @Inject constructor(
    private val repository: TourDiscoveryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TourDetailsState(isLoading = true))
    val state: StateFlow<TourDetailsState> = _state.asStateFlow()

    fun loadTourDetails(tourId: String) {
        viewModelScope.launch {
            _state.value = TourDetailsState(isLoading = true)

            repository.getTourDetails(tourId).fold(
                onSuccess = { tourDetails ->
                    _state.value = TourDetailsState(
                        isLoading = false,
                        tourDetails = tourDetails
                    )
                },
                onFailure = { error ->
                    _state.value = TourDetailsState(
                        isLoading = false,
                        error = "Failed to load tour details: ${error.message}"
                    )
                }
            )
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val currentDetails = _state.value.tourDetails ?: return@launch

            // Optimistically update UI
            _state.value = _state.value.copy(
                tourDetails = currentDetails.copy(isBookmarked = !currentDetails.isBookmarked)
            )

            val result = if (currentDetails.isBookmarked) {
                repository.removeBookmark(currentDetails.id)
            } else {
                repository.bookmarkTour(currentDetails.id)
            }

            result.onFailure { error ->
                // Revert on failure
                _state.value = _state.value.copy(
                    tourDetails = currentDetails,
                    error = "Failed to ${if (currentDetails.isBookmarked) "remove" else "add"} bookmark: ${error.message}"
                )
            }
        }
    }
}