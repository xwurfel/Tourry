package com.xwurfel.tourry.ui.tour.mine

import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class MyToursViewModel @Inject constructor(
    private val mockDataManager: MockDataManager
) : MviViewModel<MyToursUiState, MyToursPartialState, MyToursEvent, MyToursIntent>(
    initialState = MyToursUiState()
) {

    init {
        observeContinuousChanges(
            loadMyTours()
        )
    }

    override fun mapIntents(intent: MyToursIntent): Flow<MyToursPartialState> = flow {
        when (intent) {
            is MyToursIntent.TabChanged -> {
                emit(MyToursPartialState.TabChanged(intent.tab))
            }

            is MyToursIntent.TourClicked -> {
                when (intent.tourStatus) {
                    TourStatus.LIVE -> publishEvent(MyToursEvent.NavigateToLiveTour(intent.tourId))
                    TourStatus.UPCOMING -> publishEvent(MyToursEvent.NavigateToTourDetail(intent.tourId))
                    TourStatus.COMPLETED -> publishEvent(MyToursEvent.NavigateToTourSummary(intent.tourId))
                }
            }

            is MyToursIntent.EditTour -> {
                publishEvent(MyToursEvent.NavigateToTourEdit(intent.tourId))
            }

            is MyToursIntent.CancelTour -> {
                emit(MyToursPartialState.Loading)
                try {
                    // TODO: Implement actual cancellation logic in MockDataManager
                    kotlinx.coroutines.delay(500) // Simulate API call
                    emit(MyToursPartialState.TourCancelled(intent.tourId))
                } catch (_: Exception) {
                    emit(MyToursPartialState.Error("Failed to cancel tour"))
                }
            }

            MyToursIntent.RefreshTours -> {
                emit(MyToursPartialState.Loading)
                // Refresh will be handled by the continuous flow
            }
        }
    }

    override fun reduceUiState(
        previousState: MyToursUiState, partialState: MyToursPartialState
    ): MyToursUiState {
        return when (partialState) {
            is MyToursPartialState.Loading -> previousState.copy(isLoading = true, error = null)

            is MyToursPartialState.ToursLoaded -> previousState.copy(
                joinedTours = partialState.joinedTours,
                createdTours = partialState.createdTours,
                isLoading = false,
                error = null
            )

            is MyToursPartialState.TabChanged -> previousState.copy(
                selectedTab = partialState.tab
            )

            is MyToursPartialState.TourCancelled -> {
                val updatedCreatedTours =
                    previousState.createdTours.filterNot { it.id == partialState.tourId }
                previousState.copy(
                    createdTours = updatedCreatedTours, isLoading = false
                )
            }

            is MyToursPartialState.Error -> previousState.copy(
                isLoading = false, error = partialState.message
            )
        }
    }

    private fun loadMyTours(): Flow<MyToursPartialState> = flow {
        emit(MyToursPartialState.Loading)

        // Combine joined tours and created tours
        combine(
            mockDataManager.joinedTourIds, mockDataManager.createdTours
        ) { joinedIds, createdTours ->
            val joinedTours = mockDataManager.getJoinedTours()
            MyToursPartialState.ToursLoaded(joinedTours, createdTours)
        }.collect { partialState ->
            emit(partialState)
        }
    }
}

// States
data class MyToursUiState(
    val selectedTab: MyToursTab = MyToursTab.JOINED,
    val joinedTours: List<MyTour> = emptyList(),
    val createdTours: List<MyTour> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface MyToursPartialState {
    object Loading : MyToursPartialState
    data class ToursLoaded(
        val joinedTours: List<MyTour>, val createdTours: List<MyTour>
    ) : MyToursPartialState

    data class TabChanged(val tab: MyToursTab) : MyToursPartialState
    data class TourCancelled(val tourId: String) : MyToursPartialState
    data class Error(val message: String) : MyToursPartialState
}

sealed interface MyToursIntent {
    data class TabChanged(val tab: MyToursTab) : MyToursIntent
    data class TourClicked(val tourId: String, val tourStatus: TourStatus) : MyToursIntent
    data class EditTour(val tourId: String) : MyToursIntent
    data class CancelTour(val tourId: String) : MyToursIntent
    object RefreshTours : MyToursIntent
}

sealed interface MyToursEvent {
    data class NavigateToTourDetail(val tourId: String) : MyToursEvent
    data class NavigateToLiveTour(val tourId: String) : MyToursEvent
    data class NavigateToTourSummary(val tourId: String) : MyToursEvent
    data class NavigateToTourEdit(val tourId: String) : MyToursEvent
}

// Data models
enum class MyToursTab {
    JOINED, CREATED
}

data class MyTour(
    val id: String,
    val title: String,
    val coverImageUrl: String?,
    val startTime: Long,
    val status: TourStatus,
    val participantsCount: Int = 0,
    val rating: Float? = null
)

enum class TourStatus {
    UPCOMING, LIVE, COMPLETED
}