package com.xwurfel.tourry.ui.tourbuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.feature.tourbuilder.domain.model.TourDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.repository.TourBuilderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedToursState(
    val isLoading: Boolean = false,
    val tours: List<TourDraft> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SavedToursViewModel @Inject constructor(
    private val repository: TourBuilderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SavedToursState(isLoading = true))
    val state: StateFlow<SavedToursState> = _state.asStateFlow()

    init {
        loadSavedTours()
    }

    fun loadSavedTours() {
        viewModelScope.launch {
            _state.value = SavedToursState(isLoading = true)

            repository.getMySavedTours().fold(
                onSuccess = { tours ->
                    _state.value = SavedToursState(tours = tours)
                },
                onFailure = { error ->
                    _state.value = SavedToursState(error = error.message)
                }
            )
        }
    }

    fun deleteTour(tourId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            repository.deleteTour(tourId).fold(
                onSuccess = {
                    // Remove the deleted tour from the list
                    val updatedTours = _state.value.tours.filter { it.id != tourId }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        tours = updatedTours
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to delete tour: ${error.message}"
                    )
                }
            )
        }
    }
}