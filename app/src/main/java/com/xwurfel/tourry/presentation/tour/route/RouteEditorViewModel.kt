package com.xwurfel.tourry.presentation.tour.route

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.route.repository.RouteRepository
import com.xwurfel.tourry.domain.tour.service.TourService
import com.xwurfel.tourry.domain.route.model.RoutePoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.toList
import kotlin.collections.toMutableList

data class RouteEditorUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val tour: Tour? = null,
    val routePoints: List<RoutePoint> = emptyList(),
    val originalRoutePoints: List<RoutePoint> = emptyList(),
    val selectedRoutePointIndex: Int = -1,
    val hasUnsavedChanges: Boolean = false,
    val showDiscardChangesDialog: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class RouteEditorViewModel @Inject constructor(
    private val tourService: TourService,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteEditorUiState())
    val uiState: StateFlow<RouteEditorUiState> = _uiState.asStateFlow()

    fun loadTourData(tourId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

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

                // Load route points
                val routePoints = routeRepository.getRoutePointsForTour(tourId)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tour = tour,
                        routePoints = routePoints,
                        originalRoutePoints = routePoints.toList()
                    )
                }
            } catch (e: Exception) {
                ensureActive()
                Log.e("RouteEditorViewModel", "Error loading tour data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load tour data"
                    )
                }
            }
        }
    }

    fun addRoutePoint(location: LatLng) {
        val newPoint = RoutePoint(
            tourId = _uiState.value.tour?.id ?: return,
            location = location,
            title = "Stop ${_uiState.value.routePoints.size + 1}",
            description = "",
            order = _uiState.value.routePoints.size,
            durationMinutes = 15 // Default duration
        )

        _uiState.update {
            it.copy(
                routePoints = it.routePoints + newPoint,
                selectedRoutePointIndex = it.routePoints.size,
                hasUnsavedChanges = true
            )
        }
    }

    fun removeRoutePoint(index: Int) {
        if (index < 0 || index >= _uiState.value.routePoints.size) return

        val updatedRoutePoints = _uiState.value.routePoints.toMutableList().apply {
            removeAt(index)
            // Update the order of remaining points
            forEachIndexed { i, point ->
                this[i] = point.copy(order = i)
            }
        }

        _uiState.update {
            it.copy(
                routePoints = updatedRoutePoints,
                selectedRoutePointIndex = -1,
                hasUnsavedChanges = true
            )
        }
    }

    fun updateRoutePoint(index: Int, updatedPoint: RoutePoint) {
        if (index < 0 || index >= _uiState.value.routePoints.size) return

        val updatedRoutePoints = _uiState.value.routePoints.toMutableList().apply {
            this[index] = updatedPoint.copy(order = index)
        }

        _uiState.update {
            it.copy(
                routePoints = updatedRoutePoints,
                hasUnsavedChanges = true
            )
        }
    }

    fun selectRoutePoint(index: Int) {
        _uiState.update { it.copy(selectedRoutePointIndex = index) }
    }

    fun saveRoute() {
        val tourId = _uiState.value.tour?.id ?: return
        val routePoints = _uiState.value.routePoints

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                routeRepository.saveRoutePointsForTour(tourId, routePoints)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasUnsavedChanges = false,
                        isSaved = true,
                        originalRoutePoints = routePoints.toList()
                    )
                }
            } catch (e: Exception) {
                ensureActive()
                Log.e("RouteEditorViewModel", "Error saving route", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to save route"
                    )
                }
            }
        }
    }

    fun showDiscardChangesDialog() {
        _uiState.update { it.copy(showDiscardChangesDialog = true) }
    }

    fun hideDiscardChangesDialog() {
        _uiState.update { it.copy(showDiscardChangesDialog = false) }
    }
}