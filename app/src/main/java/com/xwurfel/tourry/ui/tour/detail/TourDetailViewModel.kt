package com.xwurfel.tourry.ui.tour.detail

import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.ui.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class TourDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
    // TODO: Inject use cases
) : MviViewModel<TourDetailUiState, TourDetailPartialState, TourDetailEvent, TourDetailIntent>(
    initialState = TourDetailUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""

    init {
        observeContinuousChanges(
            loadTourDetail()
        )
    }

    override fun mapIntents(intent: TourDetailIntent): Flow<TourDetailPartialState> = flow {
        when (intent) {
            TourDetailIntent.JoinTour -> {
                // TODO: Implement join tour
                publishEvent(TourDetailEvent.NavigateToBooking)
            }

            TourDetailIntent.StartTour -> {
                publishEvent(TourDetailEvent.NavigateToLiveTour)
            }
        }
    }

    override fun reduceUiState(
        previousState: TourDetailUiState,
        partialState: TourDetailPartialState
    ): TourDetailUiState {
        return when (partialState) {
            is TourDetailPartialState.Loading -> previousState.copy(isLoading = true)
            is TourDetailPartialState.TourLoaded -> previousState.copy(
                tour = partialState.tour,
                isLoading = false
            )

            is TourDetailPartialState.Error -> previousState.copy(
                isLoading = false,
                error = partialState.message
            )
        }
    }

    private fun loadTourDetail(): Flow<TourDetailPartialState> = flow {
        emit(TourDetailPartialState.Loading)
        // TODO: Load tour from repository
        // For now, emit mock data
        emit(TourDetailPartialState.TourLoaded(mockTourDetail()))
    }

    private fun mockTourDetail() = TourDetail(
        id = tourId,
        title = "Paris Hidden Gems Tour",
        description = "Discover the secret spots of Paris that most tourists never see. This walking tour takes you through charming neighborhoods, hidden courtyards, and local favorites.",
        coverImageUrl = null,
        theme = "CULTURAL",
        rating = 4.8f,
        reviewsCount = 127,
        duration = 120,
        distance = 3.5f,
        price = 25.0,
        isFree = false,
        startTime = System.currentTimeMillis() + 86400000, // Tomorrow
        isLive = false,
        isJoined = false,
        spotsLeft = 8,
        guide = TourGuide(
            id = "1",
            name = "Marie Dubois",
            avatarUrl = null,
            rating = 4.9f,
            toursCount = 45
        ),
        stops = listOf(
            TourStopDetail(
                id = "1",
                name = "Secret Garden of Palais Royal",
                description = "A hidden oasis in the heart of Paris",
                latitude = 48.8634,
                longitude = 2.3375,
                order = 1
            ),
            TourStopDetail(
                id = "2",
                name = "Passage des Panoramas",
                description = "Historic covered passage with vintage shops",
                latitude = 48.8714,
                longitude = 2.3417,
                order = 2
            ),
            TourStopDetail(
                id = "3",
                name = "Square Suzanne Buisson",
                description = "Romantic hidden square in Montmartre",
                latitude = 48.8867,
                longitude = 2.3339,
                order = 3
            )
        )
    )
}

// States
data class TourDetailUiState(
    val tour: TourDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface TourDetailPartialState {
    object Loading : TourDetailPartialState
    data class TourLoaded(val tour: TourDetail) : TourDetailPartialState
    data class Error(val message: String) : TourDetailPartialState
}

sealed interface TourDetailIntent {
    object JoinTour : TourDetailIntent
    object StartTour : TourDetailIntent
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