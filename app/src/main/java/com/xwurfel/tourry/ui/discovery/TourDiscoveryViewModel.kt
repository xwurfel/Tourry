package com.xwurfel.tourry.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.core.di.IoDispatcher
import com.xwurfel.tourry.feature.discovery.domain.model.TourFilter
import com.xwurfel.tourry.feature.discovery.domain.model.TourPreview
import com.xwurfel.tourry.feature.discovery.domain.repository.TourDiscoveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TourDiscoveryState(
    val isLoading: Boolean = false,
    val featuredTours: List<TourPreview> = emptyList(),
    val popularTours: List<TourPreview> = emptyList(),
    val nearbyTours: List<TourPreview> = emptyList(),
    val bookmarkedTours: List<TourPreview> = emptyList(),
    val searchResults: List<TourPreview> = emptyList(),
    val filter: TourFilter = TourFilter(),
    val currentSearchQuery: String = "",
    val error: String? = null
)

@HiltViewModel
class TourDiscoveryViewModel @Inject constructor(
    private val repository: TourDiscoveryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(TourDiscoveryState())
    val state: StateFlow<TourDiscoveryState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            launch { loadFeaturedTours() }
            launch { loadPopularTours() }
            launch { loadBookmarkedTours() }
        }
    }

    private suspend fun loadFeaturedTours() {
        repository.getFeaturedTours().fold(
            onSuccess = { tours ->
                _state.value = _state.value.copy(
                    featuredTours = tours,
                    isLoading = isStillLoading()
                )
            },
            onFailure = { error ->
                _state.value = _state.value.copy(
                    error = "Failed to load featured tours: ${error.message}",
                    isLoading = isStillLoading()
                )
            }
        )
    }

    private suspend fun loadPopularTours() {
        repository.getPopularTours().fold(
            onSuccess = { tours ->
                _state.value = _state.value.copy(
                    popularTours = tours,
                    isLoading = isStillLoading()
                )
            },
            onFailure = { error ->
                _state.value = _state.value.copy(
                    error = "Failed to load popular tours: ${error.message}",
                    isLoading = isStillLoading()
                )
            }
        )
    }

    private suspend fun loadBookmarkedTours() {
        repository.getBookmarkedTours().fold(
            onSuccess = { tours ->
                _state.value = _state.value.copy(
                    bookmarkedTours = tours,
                    isLoading = isStillLoading()
                )
            },
            onFailure = { error ->
                _state.value = _state.value.copy(
                    isLoading = isStillLoading()
                )
            }
        )
    }

    fun loadNearbyTours(latitude: Double, longitude: Double, radiusKm: Double = 10.0) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.getNearbyTours(latitude, longitude, radiusKm).fold(
                onSuccess = { tours ->
                    _state.value = _state.value.copy(
                        nearbyTours = tours,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        error = "Failed to load nearby tours: ${error.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun searchTours(query: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                currentSearchQuery = query
            )

            val filter = _state.value.filter.copy(query = query)

            repository.searchTours(filter).fold(
                onSuccess = { tours ->
                    _state.value = _state.value.copy(
                        searchResults = tours,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        error = "Search failed: ${error.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun updateFilter(filter: TourFilter) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                filter = filter,
                isLoading = true,
                error = null
            )

            // Apply the filter (preserve the current search query)
            val filterWithQuery = filter.copy(query = _state.value.currentSearchQuery)

            repository.searchTours(filterWithQuery).fold(
                onSuccess = { tours ->
                    _state.value = _state.value.copy(
                        searchResults = tours,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _state.value = _state.value.copy(
                        error = "Filter application failed: ${error.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun toggleBookmark(tourId: String, isCurrentlyBookmarked: Boolean) {
        viewModelScope.launch {
            // Optimistically update UI first
            updateLocalBookmarkState(tourId, !isCurrentlyBookmarked)

            val result = if (isCurrentlyBookmarked) {
                repository.removeBookmark(tourId)
            } else {
                repository.bookmarkTour(tourId)
            }

            result.onFailure { error ->
                // Revert optimistic update on failure
                updateLocalBookmarkState(tourId, isCurrentlyBookmarked)
                _state.value = _state.value.copy(
                    error = "Failed to ${if (isCurrentlyBookmarked) "remove" else "add"} bookmark: ${error.message}"
                )
            }
        }
    }

    private fun updateLocalBookmarkState(tourId: String, isBookmarked: Boolean) {
        val current = _state.value

        // Update all tour lists in our state
        _state.value = current.copy(
            featuredTours = updateTourListBookmarkState(
                current.featuredTours,
                tourId,
                isBookmarked
            ),
            popularTours = updateTourListBookmarkState(current.popularTours, tourId, isBookmarked),
            nearbyTours = updateTourListBookmarkState(current.nearbyTours, tourId, isBookmarked),
            searchResults = updateTourListBookmarkState(
                current.searchResults,
                tourId,
                isBookmarked
            ),
            bookmarkedTours = if (isBookmarked) {
                // Add to bookmarked list if it exists in another list
                val tour = findTourInAllLists(tourId)
                if (tour != null) {
                    val updatedTour = tour.copy(isBookmarked = true)
                    current.bookmarkedTours + updatedTour
                } else {
                    current.bookmarkedTours
                }
            } else {
                // Remove from bookmarked list
                current.bookmarkedTours.filter { it.id != tourId }
            }
        )
    }

    private fun updateTourListBookmarkState(
        tours: List<TourPreview>,
        tourId: String,
        isBookmarked: Boolean
    ): List<TourPreview> {
        return tours.map { tour ->
            if (tour.id == tourId) {
                tour.copy(isBookmarked = isBookmarked)
            } else {
                tour
            }
        }
    }

    private fun findTourInAllLists(tourId: String): TourPreview? {
        val current = _state.value
        return current.featuredTours.find { it.id == tourId }
            ?: current.popularTours.find { it.id == tourId }
            ?: current.nearbyTours.find { it.id == tourId }
            ?: current.searchResults.find { it.id == tourId }
    }

    // Helper to check if we're still loading any data
    private fun isStillLoading(): Boolean {
        return _state.value.run {
            featuredTours.isEmpty() || popularTours.isEmpty()
        }
    }

    fun refresh() {
        loadInitialData()
    }
}