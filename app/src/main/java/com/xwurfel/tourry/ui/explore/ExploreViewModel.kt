package com.xwurfel.tourry.ui.explore

import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.analytics.TourAnalytics
import com.xwurfel.tourry.feature.profile.domain.usecase.GetCurrentUserIdUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.JoinTourUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.ObserveAvailableToursUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.ObserveUserParticipationsUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.SearchToursUseCase
import com.xwurfel.tourry.ui.explore.mapper.TourPreviewMapper.toTourPreviews
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val observeAvailableToursUseCase: ObserveAvailableToursUseCase,
    private val searchToursUseCase: SearchToursUseCase,
    private val joinTourUseCase: JoinTourUseCase,
    private val observeUserParticipationsUseCase: ObserveUserParticipationsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val tourAnalytics: TourAnalytics
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

                if (intent.query.isNotBlank()) {
                    // Perform search using Firebase
                    searchToursUseCase(
                        query = intent.query,
                        themes = uiStateSnapshot.value.activeFilters.themes,
                        maxPrice = uiStateSnapshot.value.activeFilters.maxPrice,
                        maxDistance = uiStateSnapshot.value.activeFilters.maxDistance
                    ).onSuccess { tours ->
                        val tourPreviews = tours.toTourPreviews()

                        tourAnalytics.trackSearch(
                            query = intent.query,
                            resultsCount = tourPreviews.size
                        )

                        emit(ExplorePartialState.ToursFiltered(tourPreviews))
                    }.onFailure { error ->
                        emit(ExplorePartialState.Error("Search failed: ${error.msg}"))
                    }
                } else {
                    // Reset to all tours when search is cleared
                    emit(ExplorePartialState.ToursFiltered(uiStateSnapshot.value.allTours))
                }
            }

            is ExploreIntent.FilterChanged -> {
                emit(ExplorePartialState.FiltersChanged(intent.filters))

                val filterUsed = when {
                    intent.filters.dateRange != null -> "date"
                    intent.filters.maxDuration != null -> "duration"
                    intent.filters.maxPrice != null -> "price"
                    intent.filters.maxDistance != null -> "distance"
                    intent.filters.themes.isNotEmpty() -> "theme"
                    else -> null
                }

                if (filterUsed != null) {
                    tourAnalytics.trackSearch(
                        query = uiStateSnapshot.value.searchQuery,
                        resultsCount = 0, // Will be updated after filtering
                        filterUsed = filterUsed
                    )
                }

                applyFilters(intent.filters)
            }

            is ExploreIntent.TourClicked -> {
                tourAnalytics.trackEvent(
                    "tour_detail_viewed",
                    mapOf("tour_id" to intent.tourId)
                )
                publishEvent(ExploreEvent.NavigateToTourDetail(intent.tourId))
            }

            is ExploreIntent.CreateTourClicked -> {
                tourAnalytics.trackEvent(
                    "tour_creation_started",
                    mapOf("source" to "explore_fab")
                )
                publishEvent(ExploreEvent.NavigateToTourCreation)
            }

            is ExploreIntent.ToggleViewMode -> {
                emit(ExplorePartialState.ViewModeChanged(!uiStateSnapshot.value.isMapMode))
            }

            is ExploreIntent.RefreshTours -> {
                emit(ExplorePartialState.Loading)
                // Tours will be refreshed through the continuous flow
            }

            is ExploreIntent.JoinTour -> {
                emit(ExplorePartialState.JoiningTour(intent.tourId))

                joinTourUseCase(intent.tourId)
                    .onSuccess {
                        tourAnalytics.trackEvent(
                            "tour_joined_quick",
                            mapOf(
                                "tour_id" to intent.tourId,
                                "join_method" to "quick_join"
                            )
                        )
                        emit(ExplorePartialState.TourJoined(intent.tourId))
                    }
                    .onFailure { error ->
                        emit(ExplorePartialState.Error("Failed to join tour: ${error.msg}"))
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
                allTours = partialState.tours,
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

        observeAvailableToursUseCase().collect { tours ->
            val tourPreviews = tours.toTourPreviews()
            emit(ExplorePartialState.ToursLoaded(tourPreviews))
        }
    }

    private fun observeJoinedTours(): Flow<ExplorePartialState> = flow {
        getCurrentUserIdUseCase()
            .filterNotNull()
            .collect { userId ->
                observeUserParticipationsUseCase(userId)
                    .map { participations ->
                        participations
                            .filter { it.status == com.xwurfel.tourry.feature.tours.domain.model.ParticipationStatus.JOINED }
                            .map { it.tourId }
                            .toSet()
                    }
                    .collect { joinedIds ->
                        emit(ExplorePartialState.JoinedToursUpdated(joinedIds))
                    }
            }
    }

    private suspend fun FlowCollector<ExplorePartialState>.applyFilters(filters: ExploreFilters) {
        if (filters.isEmpty()) {
            // No filters, show all tours
            emit(ExplorePartialState.ToursFiltered(uiStateSnapshot.value.allTours))
            return
        }

        // For complex filtering that involves multiple criteria, we'll use search
        searchToursUseCase(
            query = uiStateSnapshot.value.searchQuery,
            themes = filters.themes,
            maxPrice = filters.maxPrice,
            maxDistance = filters.maxDistance
        ).onSuccess { tours ->
            val tourPreviews = tours.toTourPreviews()

            // Apply additional client-side filters that might not be handled by backend
            val filteredTours = tourPreviews.filter { tour ->
                applyClientSideFilters(tour, filters)
            }

            emit(ExplorePartialState.ToursFiltered(filteredTours))
        }.onFailure { error ->
            emit(ExplorePartialState.Error("Filtering failed: ${error.msg}"))
        }
    }

    private fun applyClientSideFilters(tour: TourPreview, filters: ExploreFilters): Boolean {
        // Apply date range filter
        if (filters.dateRange != null) {
            val (startDate, endDate) = filters.dateRange
            if (tour.startTime < startDate || tour.startTime > endDate) {
                return false
            }
        }

        // Apply duration filter
        if (filters.maxDuration != null) {
            val maxDurationMinutes = filters.maxDuration * 60 // Convert hours to minutes
            if (tour.duration > maxDurationMinutes) {
                return false
            }
        }

        return true
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
    val themes: List<String> = emptyList(),
    val dateRange: Pair<Long, Long>? = null,
    val maxDuration: Int? = null, // in hours
    val maxPrice: Double? = null,
    val maxDistance: Float? = null // in km
) {
    fun isEmpty(): Boolean {
        return themes.isEmpty() &&
                dateRange == null &&
                maxDuration == null &&
                maxPrice == null &&
                maxDistance == null
    }
}
