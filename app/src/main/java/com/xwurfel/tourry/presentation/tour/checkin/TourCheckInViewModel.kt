package com.xwurfel.tourry.presentation.tour.checkin

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.domain.auth.service.AuthService
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.domain.route.repository.RouteRepository
import com.xwurfel.tourry.domain.tour.model.CheckIn
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.tour.service.CheckInService
import com.xwurfel.tourry.domain.tour.service.TourService
import com.xwurfel.tourry.domain.user_location.repository.UserLocationRepository
import com.xwurfel.tourry.service.GeofencingService
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

data class TourCheckInUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val tour: Tour? = null,
    val routePoints: List<RoutePoint> = emptyList(),
    val checkedInPoints: List<Long> = emptyList(),
    val selectedRoutePointId: Long? = null,
    val userLocation: LatLng? = null,
    val isCheckingIn: Boolean = false,
    val checkInNote: String = "",
    val checkInImageUri: Uri? = null,
    val lastCheckIn: CheckIn? = null,
    val showSuccessMessage: Boolean = false,
    val progressPercentage: Float = 0f,
    val geofencingEnabled: Boolean = false
)

@HiltViewModel
class TourCheckInViewModel @Inject constructor(
    private val tourService: TourService,
    private val routeRepository: RouteRepository,
    private val checkInService: CheckInService,
    private val authService: AuthService,
    private val userLocationRepository: UserLocationRepository,
    private val geofencingService: GeofencingService
) : ViewModel() {

    companion object {
        private val TAG = this::class.java.simpleName
    }

    private val _uiState = MutableStateFlow(TourCheckInUiState())
    val uiState: StateFlow<TourCheckInUiState> = _uiState.asStateFlow()

    @SuppressLint("MissingPermission")
    fun loadTourCheckInData(tourId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val currentUser = authService.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "You must be logged in to check in to a tour"
                        )
                    }
                    return@launch
                }

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

                val routePointsWithStatus = checkInService.getTourRoutePointsWithCheckInStatus(
                    currentUser.id,
                    tourId
                )

                val routePoints = routePointsWithStatus.map { it.first }
                val checkedInPointIds = routePointsWithStatus
                    .filter { it.second }
                    .map { it.first.id }

                val progressPercentage = if (routePoints.isEmpty()) {
                    0f
                } else {
                    checkedInPointIds.size.toFloat() / routePoints.size.toFloat()
                }

                val location = userLocationRepository.getCurrentLocation()
                val userLocation = location?.let { LatLng(it.latitude, it.longitude) }

                val geofencingEnabled = geofencingService.hasRequiredPermissions()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tour = tour,
                        routePoints = routePoints,
                        checkedInPoints = checkedInPointIds,
                        userLocation = userLocation,
                        progressPercentage = progressPercentage,
                        geofencingEnabled = geofencingEnabled
                    )
                }

                observeCheckIns(currentUser.id, tourId)

                // Set up geofences if enabled
                if (geofencingEnabled && routePoints.isNotEmpty()) {
                    setupGeofences(tourId, routePoints)
                }

            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load tour data"
                    )
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private suspend fun setupGeofences(tourId: Long, routePoints: List<RoutePoint>) {
        try {
            geofencingService.removeGeofencesForTour(tourId)

            val result = geofencingService.addGeofencesForRoutePoints(routePoints, tourId)
            if (result.isFailure) {
                result.exceptionOrNull()?.let { exception ->
                    Log.e(TAG, exception.message ?: "Failed to add geofences")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message ?: "Failed to add geofences")
        }
    }

    override fun onCleared() {
        super.onCleared()
        val tourId = _uiState.value.tour?.id ?: return
        viewModelScope.launch {
            try {
                geofencingService.removeGeofencesForTour(tourId)
            } catch (e: Exception) {
                Log.e(TAG, e.message ?: "Failed to add geofences")
            }
        }
    }

    private fun observeCheckIns(userId: Long, tourId: Long) {
        viewModelScope.launch {
            checkInService.getUserTourCheckIns(userId, tourId).collectLatest { checkIns ->
                val checkedInPointIds = checkIns.map { it.routePointId }

                val totalPoints = _uiState.value.routePoints.size
                val progressPercentage = if (totalPoints == 0) {
                    0f
                } else {
                    checkedInPointIds.size.toFloat() / totalPoints.toFloat()
                }

                _uiState.update {
                    it.copy(
                        checkedInPoints = checkedInPointIds,
                        progressPercentage = progressPercentage
                    )
                }
            }
        }
    }

    fun updateUserLocation() {
        viewModelScope.launch {
            try {
                val location = userLocationRepository.getCurrentLocation()
                if (location != null) {
                    _uiState.update {
                        it.copy(userLocation = LatLng(location.latitude, location.longitude))
                    }
                }
            } catch (_: Exception) {
                ensureActive()
            }
        }
    }

    fun selectRoutePoint(routePointId: Long) {
        _uiState.update { it.copy(selectedRoutePointId = routePointId) }
    }

    fun onCheckInNoteChanged(note: String) {
        _uiState.update { it.copy(checkInNote = note) }
    }

    fun onCheckInImageSelected(imageUri: Uri) {
        _uiState.update { it.copy(checkInImageUri = imageUri) }
    }

    fun checkInToSelectedPoint(forceCheckIn: Boolean = false) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val userId = authService.getCurrentUser()?.id ?: return@launch
            val tourId = currentState.tour?.id ?: return@launch
            val routePointId = currentState.selectedRoutePointId ?: return@launch
            val userLocation = currentState.userLocation

            if (userLocation == null && !forceCheckIn) {
                _uiState.update {
                    it.copy(errorMessage = "Unable to get your location. Please try again.")
                }
                return@launch
            }

            if (currentState.checkedInPoints.contains(routePointId)) {
                _uiState.update {
                    it.copy(errorMessage = "You've already checked in to this location")
                }
                return@launch
            }

            try {
                _uiState.update { it.copy(isCheckingIn = true, errorMessage = null) }

                val result = checkInService.checkInToRoutePoint(
                    userId = userId,
                    tourId = tourId,
                    routePointId = routePointId,
                    userLocation = userLocation ?: LatLng(0.0, 0.0),
                    note = currentState.checkInNote.takeIf { it.isNotBlank() },
                    imageUri = currentState.checkInImageUri,
                    forceCheckIn = forceCheckIn
                )

                if (result.isSuccess) {
                    val checkIn = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            isCheckingIn = false,
                            lastCheckIn = checkIn,
                            checkInNote = "",
                            checkInImageUri = null,
                            selectedRoutePointId = null,
                            showSuccessMessage = true,
                            successMessage = "Successfully checked in!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCheckingIn = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to check in"
                        )
                    }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        isCheckingIn = false,
                        errorMessage = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun enableGeofencing() {
        val currentState = _uiState.value
        if (!geofencingService.hasRequiredPermissions()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Location permission is required for geofencing"
                )
            }
            return
        }

        val tourId = currentState.tour?.id ?: return
        val routePoints = currentState.routePoints

        if (routePoints.isEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = "No route points available for geofencing"
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val result = geofencingService.addGeofencesForRoutePoints(routePoints, tourId)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            geofencingEnabled = true,
                            successMessage = "Geofence alerts enabled for this tour"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to enable geofencing"
                        )
                    }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to enable geofencing"
                    )
                }
            }
        }
    }

    fun disableGeofencing() {
        val tourId = _uiState.value.tour?.id ?: return

        viewModelScope.launch {
            try {
                val result = geofencingService.removeGeofencesForTour(tourId)
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            geofencingEnabled = false,
                            successMessage = "Geofence alerts disabled"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to disable geofencing"
                        )
                    }
                }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Failed to disable geofencing"
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = false, successMessage = null) }
    }
}