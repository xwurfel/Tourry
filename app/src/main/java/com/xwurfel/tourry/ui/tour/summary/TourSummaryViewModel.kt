package com.xwurfel.tourry.ui.tour.summary

import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.ui.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class TourSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
    // TODO: Inject feedback and tour use cases
) : MviViewModel<TourSummaryUiState, TourSummaryPartialState, TourSummaryEvent, TourSummaryIntent>(
    initialState = TourSummaryUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""

    init {
        observeContinuousChanges(
            loadTourSummary()
        )
    }

    override fun mapIntents(intent: TourSummaryIntent): Flow<TourSummaryPartialState> = flow {
        when (intent) {
            is TourSummaryIntent.SubmitRating -> {
                emit(TourSummaryPartialState.RatingUpdated(intent.rating))
                // TODO: Submit rating to repository
            }

            is TourSummaryIntent.UpdateFeedback -> {
                emit(TourSummaryPartialState.FeedbackUpdated(intent.feedback))
            }

            is TourSummaryIntent.SubmitFeedback -> {
                emit(TourSummaryPartialState.SubmittingFeedback)
                try {
                    // TODO: Submit feedback to repository
                    kotlinx.coroutines.delay(1000) // Simulate network call
                    emit(TourSummaryPartialState.FeedbackSubmitted)
                } catch (e: Exception) {
                    emit(TourSummaryPartialState.Error("Failed to submit feedback"))
                }
            }

            TourSummaryIntent.ShareTour -> {
                // TODO: Implement sharing logic
                // This could trigger a system share intent
            }

            TourSummaryIntent.NavigateHome -> {
                publishEvent(TourSummaryEvent.NavigateHome)
            }
        }
    }

    override fun reduceUiState(
        previousState: TourSummaryUiState,
        partialState: TourSummaryPartialState
    ): TourSummaryUiState {
        return when (partialState) {
            TourSummaryPartialState.Loading -> previousState.copy(isLoading = true)

            is TourSummaryPartialState.SummaryLoaded -> previousState.copy(
                tourTitle = partialState.tourTitle,
                tourCoverImage = partialState.coverImageUrl,
                tourStats = partialState.stats,
                isLoading = false
            )

            is TourSummaryPartialState.RatingUpdated -> previousState.copy(
                userRating = partialState.rating
            )

            is TourSummaryPartialState.FeedbackUpdated -> previousState.copy(
                userFeedback = partialState.feedback
            )

            TourSummaryPartialState.SubmittingFeedback -> previousState.copy(
                isSubmittingFeedback = true
            )

            TourSummaryPartialState.FeedbackSubmitted -> previousState.copy(
                isSubmittingFeedback = false,
                isFeedbackSubmitted = true
            )

            is TourSummaryPartialState.Error -> previousState.copy(
                isLoading = false,
                isSubmittingFeedback = false,
                error = partialState.message
            )
        }
    }

    private fun loadTourSummary(): Flow<TourSummaryPartialState> = flow {
        emit(TourSummaryPartialState.Loading)
        try {
            // TODO: Load from repository
            kotlinx.coroutines.delay(1000)

            // Mock data
            val mockStats = TourStats(
                durationMinutes = 87,
                distanceKm = 2.3f,
                stopsVisited = 5,
                totalStops = 5,
                completionPercentage = 1.0f
            )

            emit(
                TourSummaryPartialState.SummaryLoaded(
                    tourTitle = "Amazing City Walking Tour",
                    coverImageUrl = null,
                    stats = mockStats
                )
            )
        } catch (e: Exception) {
            emit(TourSummaryPartialState.Error("Failed to load tour summary"))
        }
    }
}

// States
data class TourSummaryUiState(
    val tourTitle: String = "",
    val tourCoverImage: String? = null,
    val tourStats: TourStats? = null,
    val userRating: Int = 0,
    val userFeedback: String = "",
    val isSubmittingFeedback: Boolean = false,
    val isFeedbackSubmitted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface TourSummaryPartialState {
    object Loading : TourSummaryPartialState
    data class SummaryLoaded(
        val tourTitle: String,
        val coverImageUrl: String?,
        val stats: TourStats
    ) : TourSummaryPartialState

    data class RatingUpdated(val rating: Int) : TourSummaryPartialState
    data class FeedbackUpdated(val feedback: String) : TourSummaryPartialState
    object SubmittingFeedback : TourSummaryPartialState
    object FeedbackSubmitted : TourSummaryPartialState
    data class Error(val message: String) : TourSummaryPartialState
}

sealed interface TourSummaryIntent {
    data class SubmitRating(val rating: Int) : TourSummaryIntent
    data class UpdateFeedback(val feedback: String) : TourSummaryIntent
    data class SubmitFeedback(val feedback: String) : TourSummaryIntent
    object ShareTour : TourSummaryIntent
    object NavigateHome : TourSummaryIntent
}

sealed interface TourSummaryEvent {
    object NavigateHome : TourSummaryEvent
}

// Data models
data class TourStats(
    val durationMinutes: Int,
    val distanceKm: Float,
    val stopsVisited: Int,
    val totalStops: Int,
    val completionPercentage: Float
)