package com.xwurfel.tourry.presentation.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.domain.poi.model.PointOfInterest
import com.xwurfel.tourry.domain.poi.repository.PoiRepository
import com.xwurfel.tourry.domain.user_location.repository.UserLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val locationRepository: UserLocationRepository,
    private val poiRepository: PoiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeScreenUiState(isLoading = true))
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isLoading = false) }
        fetchAllPois()
    }

    fun onMapLoaded() {
        _uiState.update { it.copy(isMapLoaded = true) }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(isLocationPermissionGranted = true) }
        viewModelScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                _uiState.update { it.copy(userLocation = location) }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update { it.copy(errorMessage = "Unable to get location: ${e.message}") }
            }
        }
    }

    private fun fetchAllPois() {
        viewModelScope.launch {
            try {
                poiRepository.getPois().collect { pois ->
                    _uiState.update { it.copy(pois = pois) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    data class HomeScreenUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val isMapLoaded: Boolean = false,
        val isLocationPermissionGranted: Boolean = false,
        val userLocation: Location? = null,
        val pois: List<PointOfInterest> = emptyList(),
    )
}
