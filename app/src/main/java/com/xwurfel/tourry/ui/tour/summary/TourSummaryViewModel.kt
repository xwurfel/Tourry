package com.xwurfel.tourry.ui.tour.summary

import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class TourSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mockDataManager: MockDataManager
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
                // TODO: Submit rating to repository via MockDataManager
                try {
                    kotlinx.coroutines.delay(500) // Simulate API call
                    // Could add this to MockDataManager
                    emit(TourSummaryPartialState.RatingSubmitted)
                } catch (e: Exception) {
                    emit(TourSummaryPartialState.Error("Failed to submit rating"))
                }
            }

            is TourSummaryIntent.UpdateFeedback -> {
                emit(TourSummaryPartialState.FeedbackUpdated(intent.feedback))
            }

            is TourSummaryIntent.SubmitFeedback -> {
                emit(TourSummaryPartialState.SubmittingFeedback)
                try {
                    kotlinx.coroutines.delay(1000) // Simulate API call
                    // Could add feedback submission to MockDataManager
                    emit(TourSummaryPartialState.FeedbackSubmitted)
                } catch (e: Exception) {
                    emit(TourSummaryPartialState.Error("Failed to submit feedback"))
                }
            }

            TourSummaryIntent.ShareTour -> {
                // TODO: Implement sharing logic
                // This could trigger a system share intent
                emit(TourSummaryPartialState.TourShared)
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
            TourSummaryPartialState.Loading -> previousState.copy(isLoading = true, error = null)

            is TourSummaryPartialState.SummaryLoaded -> previousState.copy(
                tourTitle = partialState.tourTitle,
                tourCoverImage = partialState.coverImageUrl,
                tourStats = partialState.stats,
                isLoading = false,
                error = null
            )

            is TourSummaryPartialState.RatingUpdated -> previousState.copy(
                userRating = partialState.rating
            )

            TourSummaryPartialState.RatingSubmitted -> previousState.copy(
                isRatingSubmitted = true
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

            TourSummaryPartialState.TourShared -> previousState.copy(
                error = null // Could show a success message
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
            kotlinx.coroutines.delay(1000) // Simulate loading

            // Get tour details from MockDataManager
            val tourDetail = mockDataManager.getTourDetail(tourId)

            if (tourDetail != null) {
                // Generate realistic tour stats based on the tour
                val mockStats = generateTourStats(tourDetail)

                emit(
                    TourSummaryPartialState.SummaryLoaded(
                        tourTitle = tourDetail.title,
                        coverImageUrl = tourDetail.coverImageUrl,
                        stats = mockStats
                    )
                )
            } else {
                emit(TourSummaryPartialState.Error("Tour not found"))
            }
        } catch (e: Exception) {
            emit(TourSummaryPartialState.Error("Failed to load tour summary: ${e.message}"))
        }
    }

    private fun generateTourStats(tourDetail: com.xwurfel.tourry.ui.tour.detail.TourDetail): TourStats {
        // Generate realistic stats based on the tour data
        val totalStops = tourDetail.stops.size
        val stopsVisited =
            Random.nextInt(totalStops - 1, totalStops + 1) // Most or all stops visited
        val completionPercentage = stopsVisited.toFloat() / totalStops

        // Duration with some variance (80-120% of planned duration)
        val baseDuration = tourDetail.duration
        val actualDuration = (baseDuration * (0.8f + Random.nextFloat() * 0.4f)).toInt()

        // Distance with some variance
        val actualDistance = tourDetail.distance * (0.9f + Random.nextFloat() * 0.2f)

        return TourStats(
            durationMinutes = actualDuration,
            distanceKm = (actualDistance * 10).toInt() / 10.0f, // Round to 1 decimal
            stopsVisited = stopsVisited,
            totalStops = totalStops,
            completionPercentage = completionPercentage
        )
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
    val isRatingSubmitted: Boolean = false,
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
    object RatingSubmitted : TourSummaryPartialState
    data class FeedbackUpdated(val feedback: String) : TourSummaryPartialState
    object SubmittingFeedback : TourSummaryPartialState
    object FeedbackSubmitted : TourSummaryPartialState
    object TourShared : TourSummaryPartialState
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