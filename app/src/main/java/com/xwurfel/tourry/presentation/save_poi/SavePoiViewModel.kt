package com.xwurfel.tourry.presentation.save_poi

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.domain.poi.model.PointOfInterest
import com.xwurfel.tourry.domain.poi.repository.PoiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavePoiViewModel @Inject constructor(
    private val poiRepository: PoiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoiSettingsUiState())
    val uiState: StateFlow<PoiSettingsUiState> = _uiState.asStateFlow()

    fun setInitialLocation(initialLatitude: Double, initialLongitude: Double) {
        val initialLocation = LatLng(initialLatitude, initialLongitude)
        _uiState.update { it.copy(initialLocation = initialLocation) }
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

    fun savePoi() {
        val currentState = _uiState.value
        val poi = currentState.toPointOfInterest()
        viewModelScope.launch {
            try {
                poiRepository.savePoi(poi)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                ensureActive()
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    data class PoiSettingsUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val initialLocation: LatLng? = null,
        val title: String = "",
        val description: String = "",
        val imageUri: Uri? = null,
        val isSaved: Boolean = false
    ) {
        fun toPointOfInterest(): PointOfInterest {
            return PointOfInterest(
                title = title,
                description = description,
                imageUri = imageUri,
                location = initialLocation ?: LatLng(0.0, 0.0) // Should never happen
            )
        }
    }
}