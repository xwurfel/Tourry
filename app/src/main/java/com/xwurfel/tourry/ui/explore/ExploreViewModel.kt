package com.xwurfel.tourry.ui.explore

import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val mockDataManager: MockDataManager
) : MviViewModel<ExploreUiState, ExplorePartialState, ExploreEvent, ExploreIntent>(
    initialState = ExploreUiState()
) {

    init {
        observeContinuousChanges(
            loadTours(),
            observeJoinedTours()
        )
    }

    override fun mapIntents(intent: ExploreIntent): Flow<ExplorePartialState> = flow {
        when (intent) {
            is ExploreIntent.SearchQueryChanged -> {
                emit(ExplorePartialState.SearchQueryChanged(intent.query))
                // Filter tours based on search query
                val currentTours = uiStateSnapshot.value.tours
                val filteredTours = if (intent.query.isBlank()) {
                    currentTours
                } else {
                    currentTours.filter { tour ->
                        tour.title.contains(intent.query, ignoreCase = true) ||
                                tour.description.contains(intent.query, ignoreCase = true)
                    }
                }
                emit(ExplorePartialState.ToursFiltered(filteredTours))
            }

            is ExploreIntent.FilterChanged -> {
                emit(ExplorePartialState.FiltersChanged(intent.filters))
                // Apply filters to current tours
                applyFilters(intent.filters)
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
                // Reload tours from mock data manager
                loadToursFromSource()
            }

            is ExploreIntent.JoinTour -> {
                emit(ExplorePartialState.JoiningTour(intent.tourId))
                try {
                    val success = mockDataManager.joinTour(intent.tourId)
                    if (success) {
                        emit(ExplorePartialState.TourJoined(intent.tourId))
                    } else {
                        emit(ExplorePartialState.Error("Failed to join tour"))
                    }
                } catch (e: Exception) {
                    emit(ExplorePartialState.Error("Failed to join tour: ${e.message}"))
                }
            }
        }
    }

    override fun reduceUiState(
        previousState: ExploreUiState,
        partialState: ExplorePartialState
    ): ExploreUiState {
        return when (partialState) {
            is ExplorePartialState.Loading -> previousState.copy(isLoading = true, error = null)

            is ExplorePartialState.ToursLoaded -> previousState.copy(
                tours = partialState.tours,
                allTours = partialState.tours, // Keep original list for filtering
                isLoading = false,
                error = null
            )

            is ExplorePartialState.ToursFiltered -> previousState.copy(
                tours = partialState.tours
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

            is ExplorePartialState.JoiningTour -> previousState.copy(
                isLoading = true,
                error = null
            )

            is ExplorePartialState.TourJoined -> previousState.copy(
                isLoading = false,
                joinedTourIds = previousState.joinedTourIds + partialState.tourId
            )

            is ExplorePartialState.JoinedToursUpdated -> previousState.copy(
                joinedTourIds = partialState.joinedTourIds
            )

            is ExplorePartialState.Error -> previousState.copy(
                isLoading = false,
                error = partialState.message
            )
        }
    }

    private fun loadTours(): Flow<ExplorePartialState> = flow {
        emit(ExplorePartialState.Loading)
        loadToursFromSource()
    }

    private suspend fun FlowCollector<ExplorePartialState>.loadToursFromSource() {
        mockDataManager.availableTours.collect { tours ->
            emit(ExplorePartialState.ToursLoaded(tours))
        }
    }

    private fun observeJoinedTours(): Flow<ExplorePartialState> = flow {
        mockDataManager.joinedTourIds.collect { joinedIds ->
            emit(ExplorePartialState.JoinedToursUpdated(joinedIds))
        }
    }

    private suspend fun FlowCollector<ExplorePartialState>.applyFilters(filters: ExploreFilters) {
        val allTours = uiStateSnapshot.value.allTours
        val currentTime = System.currentTimeMillis()

        val filteredTours = allTours.filter { tour ->
            // Date filter
            if (filters.dateRange != null) {
                val (startDate, endDate) = filters.dateRange
                if (tour.startTime < startDate || tour.startTime > endDate) {
                    return@filter false
                }
            }

            // Duration filter
            if (filters.maxDuration != null) {
                if (tour.duration > filters.maxDuration * 60) { // Convert hours to minutes
                    return@filter false
                }
            }

            // Price filter
            if (filters.maxPrice != null) {
                if (tour.price > filters.maxPrice) {
                    return@filter false
                }
            }

            // Distance filter (mock implementation)
            if (filters.maxDistance != null && tour.distance != null) {
                if (tour.distance > filters.maxDistance) {
                    return@filter false
                }
            }

            true
        }

        emit(ExplorePartialState.ToursFiltered(filteredTours))
    }
}

// Updated states and intents
data class ExploreUiState(
    val tours: List<TourPreview> = emptyList(),
    val allTours: List<TourPreview> = emptyList(), // Keep original list for filtering
    val searchQuery: String = "",
    val activeFilters: ExploreFilters = ExploreFilters(),
    val isLoading: Boolean = false,
    val isMapMode: Boolean = false,
    val joinedTourIds: Set<String> = emptySet(),
    val error: String? = null
)

sealed interface ExplorePartialState {
    object Loading : ExplorePartialState
    data class ToursLoaded(val tours: List<TourPreview>) : ExplorePartialState
    data class ToursFiltered(val tours: List<TourPreview>) : ExplorePartialState
    data class SearchQueryChanged(val query: String) : ExplorePartialState
    data class FiltersChanged(val filters: ExploreFilters) : ExplorePartialState
    data class ViewModeChanged(val isMapMode: Boolean) : ExplorePartialState
    data class JoiningTour(val tourId: String) : ExplorePartialState
    data class TourJoined(val tourId: String) : ExplorePartialState
    data class JoinedToursUpdated(val joinedTourIds: Set<String>) : ExplorePartialState
    data class Error(val message: String) : ExplorePartialState
}

sealed interface ExploreIntent {
    data class SearchQueryChanged(val query: String) : ExploreIntent
    data class FilterChanged(val filters: ExploreFilters) : ExploreIntent
    data class TourClicked(val tourId: String) : ExploreIntent
    object CreateTourClicked : ExploreIntent
    object ToggleViewMode : ExploreIntent
    object RefreshTours : ExploreIntent
    data class JoinTour(val tourId: String) : ExploreIntent
}

sealed interface ExploreEvent {
    data class NavigateToTourDetail(val tourId: String) : ExploreEvent
    object NavigateToTourCreation : ExploreEvent
}

// Data models remain the same
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
    val maxDuration: Int? = null, // in hours
    val maxPrice: Double? = null,
    val maxDistance: Float? = null // in km
)