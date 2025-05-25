package com.xwurfel.tourry.ui.tour.detail

import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class TourDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mockDataManager: MockDataManager
) : MviViewModel<TourDetailUiState, TourDetailPartialState, TourDetailEvent, TourDetailIntent>(
    initialState = TourDetailUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""

    init {
        observeContinuousChanges(
            loadTourDetail(),
            observeJoinedStatus()
        )
    }

    override fun mapIntents(intent: TourDetailIntent): Flow<TourDetailPartialState> = flow {
        when (intent) {
            TourDetailIntent.JoinTour -> {
                emit(TourDetailPartialState.JoiningTour)
                try {
                    val success = mockDataManager.joinTour(tourId)
                    if (success) {
                        emit(TourDetailPartialState.TourJoined)
                        // If the tour is live, navigate directly to live tour
                        val tour = uiStateSnapshot.value.tour
                        if (tour?.isLive == true) {
                            publishEvent(TourDetailEvent.NavigateToLiveTour)
                        } else {
                            publishEvent(TourDetailEvent.NavigateToBooking)
                        }
                    } else {
                        emit(TourDetailPartialState.Error("Failed to join tour"))
                    }
                } catch (e: Exception) {
                    emit(TourDetailPartialState.Error("Failed to join tour: ${e.message}"))
                }
            }

            TourDetailIntent.StartTour -> {
                // Check if user has joined the tour
                val joinedIds = mockDataManager.joinedTourIds.value
                if (tourId in joinedIds) {
                    publishEvent(TourDetailEvent.NavigateToLiveTour)
                } else {
                    emit(TourDetailPartialState.Error("You must join the tour first"))
                }
            }

            TourDetailIntent.RefreshTour -> {
                emit(TourDetailPartialState.Loading)
                // Refresh will be handled by loadTourDetail
            }

            TourDetailIntent.ShareTour -> {
                // TODO: Implement sharing functionality
                // For now, just show a success message
                emit(TourDetailPartialState.TourShared)
            }
        }
    }

    override fun reduceUiState(
        previousState: TourDetailUiState,
        partialState: TourDetailPartialState
    ): TourDetailUiState {
        return when (partialState) {
            is TourDetailPartialState.Loading -> previousState.copy(isLoading = true, error = null)

            is TourDetailPartialState.TourLoaded -> previousState.copy(
                tour = partialState.tour,
                isLoading = false,
                error = null
            )

            is TourDetailPartialState.JoiningTour -> previousState.copy(
                isJoining = true,
                error = null
            )

            is TourDetailPartialState.TourJoined -> {
                val updatedTour = previousState.tour?.copy(isJoined = true)
                previousState.copy(
                    tour = updatedTour,
                    isJoining = false
                )
            }

            is TourDetailPartialState.JoinedStatusUpdated -> {
                val updatedTour = previousState.tour?.copy(isJoined = partialState.isJoined)
                previousState.copy(tour = updatedTour)
            }

            is TourDetailPartialState.TourShared -> previousState.copy(
                error = null // Could show a success message here
            )

            is TourDetailPartialState.Error -> previousState.copy(
                isLoading = false,
                isJoining = false,
                error = partialState.message
            )
        }
    }

    private fun loadTourDetail(): Flow<TourDetailPartialState> = flow {
        emit(TourDetailPartialState.Loading)
        try {
            // Get tour detail from MockDataManager
            val tourDetail = mockDataManager.getTourDetail(tourId)
            if (tourDetail != null) {
                emit(TourDetailPartialState.TourLoaded(tourDetail))
            } else {
                emit(TourDetailPartialState.Error("Tour not found"))
            }
        } catch (e: Exception) {
            emit(TourDetailPartialState.Error("Failed to load tour: ${e.message}"))
        }
    }

    private fun observeJoinedStatus(): Flow<TourDetailPartialState> = flow {
        mockDataManager.joinedTourIds.collect { joinedIds ->
            val isJoined = tourId in joinedIds
            emit(TourDetailPartialState.JoinedStatusUpdated(isJoined))
        }
    }
}

// States
data class TourDetailUiState(
    val tour: TourDetail? = null,
    val isLoading: Boolean = false,
    val isJoining: Boolean = false,
    val error: String? = null
)

sealed interface TourDetailPartialState {
    object Loading : TourDetailPartialState
    data class TourLoaded(val tour: TourDetail) : TourDetailPartialState
    object JoiningTour : TourDetailPartialState
    object TourJoined : TourDetailPartialState
    data class JoinedStatusUpdated(val isJoined: Boolean) : TourDetailPartialState
    object TourShared : TourDetailPartialState
    data class Error(val message: String) : TourDetailPartialState
}

sealed interface TourDetailIntent {
    object JoinTour : TourDetailIntent
    object StartTour : TourDetailIntent
    object RefreshTour : TourDetailIntent
    object ShareTour : TourDetailIntent
}

sealed interface TourDetailEvent {
    object NavigateToLiveTour : TourDetailEvent
    object NavigateToBooking : TourDetailEvent
}

// Data models
data class TourDetail(
    val id: String,
    val title: String,
    val description: String,
    val coverImageUrl: String?,
    val theme: String,
    val rating: Float,
    val reviewsCount: Int,
    val duration: Int, // minutes
    val distance: Float, // km
    val price: Double,
    val isFree: Boolean,
    val startTime: Long,
    val isLive: Boolean,
    val isJoined: Boolean,
    val spotsLeft: Int?,
    val guide: TourGuide,
    val stops: List<TourStopDetail>
)

data class TourGuide(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val rating: Float,
    val toursCount: Int
)

data class TourStopDetail(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int
)