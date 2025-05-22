package com.xwurfel.tourry.ui.explore

import com.xwurfel.tourry.core.ui.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    // TODO: Inject use cases here
) : MviViewModel<ExploreUiState, ExplorePartialState, ExploreEvent, ExploreIntent>(
    initialState = ExploreUiState()
) {

    init {
        observeContinuousChanges(
            loadTours()
        )
    }

    override fun mapIntents(intent: ExploreIntent): Flow<ExplorePartialState> = flow {
        when (intent) {
            is ExploreIntent.SearchQueryChanged -> {
                emit(ExplorePartialState.SearchQueryChanged(intent.query))
                // TODO: Trigger search
            }

            is ExploreIntent.FilterChanged -> {
                emit(ExplorePartialState.FiltersChanged(intent.filters))
                // TODO: Apply filters
            }

            is ExploreIntent.TourClicked -> {
                publishEvent(ExploreEvent.NavigateToTourDetail(intent.tourId))
            }

            is ExploreIntent.CreateTourClicked -> {
                publishEvent(ExploreEvent.NavigateToTourCreation)
            }

            is ExploreIntent.ToggleViewMode -> {
                emit(ExplorePartialState.ViewModeChanged(!uiStateSnapshot.value.isMapMode))
            }

            is ExploreIntent.RefreshTours -> {
                emit(ExplorePartialState.Loading)
                // TODO: Refresh tours
            }
        }
    }

    override fun reduceUiState(
        previousState: ExploreUiState,
        partialState: ExplorePartialState
    ): ExploreUiState {
        return when (partialState) {
            is ExplorePartialState.Loading -> previousState.copy(isLoading = true)
            is ExplorePartialState.ToursLoaded -> previousState.copy(
                tours = partialState.tours,
                isLoading = false
            )

            is ExplorePartialState.SearchQueryChanged -> previousState.copy(
                searchQuery = partialState.query
            )

            is ExplorePartialState.FiltersChanged -> previousState.copy(
                activeFilters = partialState.filters
            )

            is ExplorePartialState.ViewModeChanged -> previousState.copy(
                isMapMode = partialState.isMapMode
            )

            is ExplorePartialState.Error -> previousState.copy(
                isLoading = false,
                error = partialState.message
            )
        }
    }

    private fun loadTours(): Flow<ExplorePartialState> = flow {
        emit(ExplorePartialState.Loading)
        // TODO: Load tours from repository
        // For now, emit mock data
        emit(ExplorePartialState.ToursLoaded(emptyList()))
    }
}

// States and Intents
data class ExploreUiState(
    val tours: List<TourPreview> = emptyList(),
    val searchQuery: String = "",
    val activeFilters: ExploreFilters = ExploreFilters(),
    val isLoading: Boolean = false,
    val isMapMode: Boolean = false,
    val error: String? = null
)

sealed interface ExplorePartialState {
    object Loading : ExplorePartialState
    data class ToursLoaded(val tours: List<TourPreview>) : ExplorePartialState
    data class SearchQueryChanged(val query: String) : ExplorePartialState
    data class FiltersChanged(val filters: ExploreFilters) : ExplorePartialState
    data class ViewModeChanged(val isMapMode: Boolean) : ExplorePartialState
    data class Error(val message: String) : ExplorePartialState
}

sealed interface ExploreIntent {
    data class SearchQueryChanged(val query: String) : ExploreIntent
    data class FilterChanged(val filters: ExploreFilters) : ExploreIntent
    data class TourClicked(val tourId: String) : ExploreIntent
    object CreateTourClicked : ExploreIntent
    object ToggleViewMode : ExploreIntent
    object RefreshTours : ExploreIntent
}

sealed interface ExploreEvent {
    data class NavigateToTourDetail(val tourId: String) : ExploreEvent
    object NavigateToTourCreation : ExploreEvent
}

// Data models
data class TourPreview(
    val id: String,
    val title: String,
    val description: String,
    val coverImageUrl: String?,
    val rating: Float,
    val price: Double,
    val isFree: Boolean,
    val isLiveSoon: Boolean,
    val startTime: Long,
    val duration: Int, // in minutes
    val distance: Float? = null // in km
)

data class ExploreFilters(
    val dateRange: Pair<Long, Long>? = null,
    val maxDuration: Int? = null,
    val maxPrice: Double? = null,
    val maxDistance: Float? = null
)