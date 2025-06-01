package com.xwurfel.tourry.ui.tour.detail

import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.analytics.TourAnalytics
import com.xwurfel.tourry.feature.profile.domain.usecase.GetCurrentUserIdUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.GetTourByIdUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.JoinTourUseCase
import com.xwurfel.tourry.feature.tours.domain.usecase.ObserveUserParticipationsUseCase
import com.xwurfel.tourry.ui.tour.detail.mapper.TourDetailMapper.toTourDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class TourDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTourByIdUseCase: GetTourByIdUseCase,
    private val joinTourUseCase: JoinTourUseCase,
    private val observeUserParticipationsUseCase: ObserveUserParticipationsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val tourAnalytics: TourAnalytics,
) : MviViewModel<TourDetailUiState, TourDetailPartialState, TourDetailEvent, TourDetailIntent>(
    initialState = TourDetailUiState()
) {

    private val tourId: String = savedStateHandle.get<String>("tourId") ?: ""

    init {
        tourAnalytics.trackEvent(
            "tour_detail_opened",
            mapOf("tour_id" to tourId)
        )

        observeContinuousChanges(
            loadTourDetail(),
            observeJoinedStatus()
        )
    }

    override fun mapIntents(intent: TourDetailIntent): Flow<TourDetailPartialState> = flow {
        when (intent) {
            TourDetailIntent.JoinTour -> {
                emit(TourDetailPartialState.JoiningTour)
                tourAnalytics.trackEvent(
                    "tour_join_attempted",
                    mapOf("tour_id" to tourId)
                )

                joinTourUseCase(tourId)
                    .onSuccess {
                        val tour = uiStateSnapshot.value.tour
                        tourAnalytics.trackEvent(
                            "tour_joined",
                            mapOf(
                                "tour_id" to tourId,
                                "tour_price" to (tour?.price?.toString() ?: "0"),
                                "join_method" to "detail_page"
                            )
                        )

                        emit(TourDetailPartialState.TourJoined)

                        if (tour?.isLive == true) {
                            publishEvent(TourDetailEvent.NavigateToLiveTour)
                        } else {
                            publishEvent(TourDetailEvent.NavigateToBooking)
                        }
                    }
                    .onFailure { error ->
                        emit(TourDetailPartialState.Error("Failed to join tour: ${error.msg}"))
                    }
            }

            TourDetailIntent.StartTour -> {
                tourAnalytics.trackEvent(
                    "tour_started_from_detail",
                    mapOf("tour_id" to tourId)
                )

                val tour = uiStateSnapshot.value.tour
                if (tour?.isJoined == true) {
                    publishEvent(TourDetailEvent.NavigateToLiveTour)
                } else {
                    emit(TourDetailPartialState.Error("You must join the tour first"))
                }
            }

            TourDetailIntent.ShareTour -> {
                tourAnalytics.trackEvent(
                    "tour_shared",
                    mapOf("tour_id" to tourId, "share_source" to "detail_page")
                )
                emit(TourDetailPartialState.TourShared)
            }

            TourDetailIntent.RefreshTour -> {
                emit(TourDetailPartialState.Loading)
                // Reload tour data
                getTourByIdUseCase(tourId)
                    .onSuccess { tour ->
                        val isJoined = uiStateSnapshot.value.tour?.isJoined ?: false
                        emit(TourDetailPartialState.TourLoaded(tour.toTourDetail(isJoined = isJoined)))
                    }
                    .onFailure { error ->
                        emit(TourDetailPartialState.Error("Failed to refresh tour: ${error.msg}"))
                    }
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

        getTourByIdUseCase(tourId)
            .onSuccess { tour ->
                val isJoined = checkIfUserJoinedTour()
                emit(TourDetailPartialState.TourLoaded(tour.toTourDetail(isJoined = isJoined)))
            }
            .onFailure { error ->
                emit(TourDetailPartialState.Error("Failed to load tour: ${error.msg}"))
            }
    }

    private fun observeJoinedStatus(): Flow<TourDetailPartialState> = flow {
        getCurrentUserIdUseCase()
            .filterNotNull()
            .collect { userId ->
                observeUserParticipationsUseCase(userId)
                    .map { participations ->
                        participations.any {
                            it.tourId == tourId &&
                                    it.status == com.xwurfel.tourry.feature.tours.domain.model.ParticipationStatus.JOINED
                        }
                    }
                    .collect { isJoined ->
                        emit(TourDetailPartialState.JoinedStatusUpdated(isJoined))
                    }
            }
    }

    private suspend fun checkIfUserJoinedTour(): Boolean {
        // This is a simple check that will be updated by the observeJoinedStatus flow
        return false
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