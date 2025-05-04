package com.xwurfel.tourry.presentation.tour.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.domain.category.model.TourCategory
import com.xwurfel.tourry.domain.category.repository.TourCategoryRepository
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.tour.service.TourService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

enum class TourFilterType(val displayName: String) {
    ALL("All"), UPCOMING("Upcoming"), TRENDING("Trending"), MY_TOURS("My Tours")
}

data class TourListUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allTours: List<Tour> = emptyList(),
    val filteredTours: List<Tour> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val filterType: TourFilterType = TourFilterType.ALL,
    val allCategories: List<TourCategory> = emptyList()
)

@HiltViewModel
class TourListViewModel @Inject constructor(
    private val tourService: TourService, private val categoryRepository: TourCategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TourListUiState(isLoading = true))
    val uiState: StateFlow<TourListUiState> = _uiState.asStateFlow()

    fun loadTours() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                tourService.getAllTours().collectLatest { tours ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false, allTours = tours, filteredTours = applyFilters(
                                tours,
                                state.searchQuery,
                                state.selectedCategoryIds,
                                state.filterType
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false, errorMessage = e.message ?: "Failed to load tours"
                    )
                }
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                categoryRepository.getAllCategories().collectLatest { categories ->
                    _uiState.update { it.copy(allCategories = categories, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query, filteredTours = applyFilters(
                    state.allTours, query, state.selectedCategoryIds, state.filterType
                )
            )
        }
    }

    fun onCategorySelected(categoryId: Long) {
        _uiState.update { state ->
            val updatedCategoryIds = if (state.selectedCategoryIds.contains(categoryId)) {
                state.selectedCategoryIds - categoryId
            } else {
                state.selectedCategoryIds + categoryId
            }

            state.copy(
                selectedCategoryIds = updatedCategoryIds,
                filteredTours = applyFilters(
                    state.allTours, state.searchQuery, updatedCategoryIds, state.filterType
                )
            )
        }
    }


    fun onFilterTypeChanged(filterType: TourFilterType) {
        _uiState.update { state ->
            state.copy(
                filterType = filterType, filteredTours = applyFilters(
                    state.allTours, state.searchQuery, state.selectedCategoryIds, filterType
                )
            )
        }
    }

    private fun applyFilters(
        tours: List<Tour>, searchQuery: String, categoryIds: Set<Long>, filterType: TourFilterType
    ): List<Tour> {
        var filteredTours = tours

        if (categoryIds.isNotEmpty()) {
            filteredTours = filteredTours.filter { tour ->
                tour.categoryId in categoryIds
            }
        }

        if (searchQuery.isNotEmpty()) {
            filteredTours = filteredTours.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(
                    searchQuery, ignoreCase = true
                ) || it.meetingPointAddress.contains(searchQuery, ignoreCase = true)
            }
        }

        // TODO: recheck filters
        filteredTours = when (filterType) {
            TourFilterType.ALL -> filteredTours
            TourFilterType.UPCOMING -> filteredTours.filter {
                it.startDateTime.isAfter(LocalDateTime.now())
            }

            TourFilterType.TRENDING -> {
                // In a real app, we would have a more sophisticated algorithm
                // For now, just return the upcoming tours sorted by start date
                filteredTours.filter {
                    it.startDateTime.isAfter(LocalDateTime.now())
                }.sortedBy { it.startDateTime }
            }

            TourFilterType.MY_TOURS -> {
                // In a real app, we would get the current user's ID
                // For now, just return an empty list
                emptyList()
            }
        }

        return filteredTours
    }
}