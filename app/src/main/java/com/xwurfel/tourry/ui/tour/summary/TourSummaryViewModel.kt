package com.xwurfel.tourry.ui.tour.summary

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.domain.util.getOrNull
import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.analytics.TourAnalytics
import com.xwurfel.tourry.feature.tours.domain.model.TourStats
import com.xwurfel.tourry.feature.tours.domain.usecase.GetTourByIdUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.GetTourStatsUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.SubmitTourReviewUseCase
import com.xwurfel.tourry.ui.tour.summary.mapper.TourSummaryMapper.toSummaryData
import com.xwurfel.tourry.ui.tour.summary.mapper.TourSummaryMapper.toUiTourStats
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class TourSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTourByIdUseCase: GetTourByIdUseCase,
    private val getTourStatsUseCase: GetTourStatsUseCase,
    private val submitTourReviewUseCase: SubmitTourReviewUseCase,
    private val tourAnalytics: TourAnalytics,
    @ApplicationContext private val context: Context,
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

                // Auto-submit rating to Firebase
                val currentState = uiStateSnapshot.value
                val completionPercentage = currentState.tourStats?.completionPercentage ?: 1.0f

                submitTourReviewUseCase(
                    tourId = tourId,
                    rating = intent.rating,
                    review = currentState.userFeedback,
                    completionPercentage = completionPercentage
                ).onSuccess {
                    tourAnalytics.trackUserFeedback(
                        tourId,
                        intent.rating,
                        currentState.userFeedback
                    )
                    emit(TourSummaryPartialState.RatingSubmitted)
                }.onFailure { error ->
                    emit(
                        TourSummaryPartialState.Error(
                            "Failed to submit rating: ${
                                error.msg.asString(
                                    context.resources
                                )
                            }"
                        )
                    )
                }
            }

            is TourSummaryIntent.UpdateFeedback -> {
                emit(TourSummaryPartialState.FeedbackUpdated(intent.feedback))
            }

            is TourSummaryIntent.SubmitFeedback -> {
                emit(TourSummaryPartialState.SubmittingFeedback)

                val currentState = uiStateSnapshot.value
                val rating = currentState.userRating
                val completionPercentage = currentState.tourStats?.completionPercentage ?: 1.0f

                if (rating == 0) {
                    emit(TourSummaryPartialState.Error("Please provide a rating before submitting feedback"))
                    return@flow
                }

                submitTourReviewUseCase(
                    tourId = tourId,
                    rating = rating,
                    review = intent.feedback,
                    completionPercentage = completionPercentage
                ).onSuccess {
                    tourAnalytics.trackUserFeedback(tourId, rating, intent.feedback)
                    emit(TourSummaryPartialState.FeedbackSubmitted)
                }.onFailure { error ->
                    emit(
                        TourSummaryPartialState.Error(
                            "Failed to submit feedback: ${
                                error.msg.asString(
                                    context.resources
                                )
                            }"
                        )
                    )
                }
            }

            TourSummaryIntent.ShareTour -> {
                tourAnalytics.trackEvent(
                    "tour_shared",
                    mapOf(
                        "tour_id" to tourId,
                        "share_source" to "summary_page"
                    )
                )
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
            TourSummaryPartialState.Loading -> previousState.copy(
                isLoading = true,
                error = null
            )

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
            // Load tour details
            val tourResult = getTourByIdUseCase(tourId)
            val tour = tourResult.getOrNull()

            if (tour == null) {
                emit(TourSummaryPartialState.Error("Tour not found"))
                return@flow
            }

            // Load tour statistics
            val statsResult = getTourStatsUseCase(tourId)
            val stats = statsResult.getOrNull()

            if (stats == null) {
                emit(TourSummaryPartialState.Error("Failed to load tour statistics"))
                return@flow
            }

            val summaryData = tour.toSummaryData()
            val uiStats = stats.toUiTourStats()

            emit(
                TourSummaryPartialState.SummaryLoaded(
                    tourTitle = summaryData.tourTitle,
                    coverImageUrl = summaryData.coverImageUrl,
                    stats = uiStats
                )
            )

        } catch (e: Exception) {
            emit(TourSummaryPartialState.Error("Failed to load tour summary: ${e.message}"))
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
    val isRatingSubmitted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface TourSummaryPartialState {
    data object Loading : TourSummaryPartialState
    data class SummaryLoaded(
        val tourTitle: String,
        val coverImageUrl: String?,
        val stats: TourStats
    ) : TourSummaryPartialState

    data class RatingUpdated(val rating: Int) : TourSummaryPartialState
    data object RatingSubmitted : TourSummaryPartialState
    data class FeedbackUpdated(val feedback: String) : TourSummaryPartialState
    data object SubmittingFeedback : TourSummaryPartialState
    data object FeedbackSubmitted : TourSummaryPartialState
    data object TourShared : TourSummaryPartialState
    data class Error(val message: String) : TourSummaryPartialState
}

sealed interface TourSummaryIntent {
    data class SubmitRating(val rating: Int) : TourSummaryIntent
    data class UpdateFeedback(val feedback: String) : TourSummaryIntent
    data class SubmitFeedback(val feedback: String) : TourSummaryIntent
    data object NavigateHome : TourSummaryIntent
    data object ShareTour : TourSummaryIntent
}

sealed interface TourSummaryEvent {
    data object NavigateHome : TourSummaryEvent
}

