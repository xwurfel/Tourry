package com.xwurfel.tourry.ui.tour.creation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.core.ui.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TourCreationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
    // TODO: Inject use cases
) : MviViewModel<TourCreationUiState, TourCreationPartialState, TourCreationEvent, TourCreationIntent>(
    initialState = TourCreationUiState()
) {

    private val tourId: String? = savedStateHandle.get<String>("tourId")

    init {
        if (tourId != null) {
            // Load existing tour for editing
            loadTourForEditing(tourId)
        }
    }

    override fun mapIntents(intent: TourCreationIntent): Flow<TourCreationPartialState> = flow {
        when (intent) {
            is TourCreationIntent.UpdateBasicInfo -> {
                emit(
                    TourCreationPartialState.BasicInfoUpdated(
                        title = intent.title,
                        theme = intent.theme,
                        description = intent.description,
                        coverImageUri = intent.coverImageUri
                    )
                )
            }

            is TourCreationIntent.AddStop -> {
                emit(TourCreationPartialState.StopAdded(intent.stop))
            }

            is TourCreationIntent.UpdateStop -> {
                emit(TourCreationPartialState.StopUpdated(intent.index, intent.stop))
            }

            is TourCreationIntent.RemoveStop -> {
                emit(TourCreationPartialState.StopRemoved(intent.index))
            }

            is TourCreationIntent.ReorderStops -> {
                emit(TourCreationPartialState.StopsReordered(intent.fromIndex, intent.toIndex))
            }

            is TourCreationIntent.UpdateSchedule -> {
                emit(
                    TourCreationPartialState.ScheduleUpdated(
                        startDateTime = intent.startDateTime,
                        price = intent.price,
                        recurrenceRule = intent.recurrenceRule
                    )
                )
            }

            is TourCreationIntent.NextStep -> {
                if (validateCurrentStep()) {
                    emit(TourCreationPartialState.StepChanged(uiStateSnapshot.value.currentStep + 1))
                }
            }

            is TourCreationIntent.PreviousStep -> {
                emit(TourCreationPartialState.StepChanged(uiStateSnapshot.value.currentStep - 1))
            }

            is TourCreationIntent.PublishTour -> {
                publishTour()
            }
        }
    }

    override fun reduceUiState(
        previousState: TourCreationUiState,
        partialState: TourCreationPartialState
    ): TourCreationUiState {
        return when (partialState) {
            is TourCreationPartialState.BasicInfoUpdated -> previousState.copy(
                title = partialState.title,
                theme = partialState.theme,
                description = partialState.description,
                coverImageUri = partialState.coverImageUri
            )

            is TourCreationPartialState.StopAdded -> previousState.copy(
                stops = previousState.stops + partialState.stop
            )

            is TourCreationPartialState.StopUpdated -> previousState.copy(
                stops = previousState.stops.toMutableList().apply {
                    set(partialState.index, partialState.stop)
                }
            )

            is TourCreationPartialState.StopRemoved -> previousState.copy(
                stops = previousState.stops.filterIndexed { index, _ -> index != partialState.index }
            )

            is TourCreationPartialState.StopsReordered -> {
                val mutableStops = previousState.stops.toMutableList()
                val stop = mutableStops.removeAt(partialState.fromIndex)
                mutableStops.add(partialState.toIndex, stop)
                previousState.copy(stops = mutableStops)
            }

            is TourCreationPartialState.ScheduleUpdated -> previousState.copy(
                startDateTime = partialState.startDateTime,
                price = partialState.price,
                recurrenceRule = partialState.recurrenceRule
            )

            is TourCreationPartialState.StepChanged -> previousState.copy(
                currentStep = partialState.step
            )

            is TourCreationPartialState.Publishing -> previousState.copy(
                isPublishing = true
            )

            is TourCreationPartialState.Published -> {
                publishEvent(TourCreationEvent.NavigateToTourDetail(partialState.tourId))
                previousState.copy(isPublishing = false)
            }

            is TourCreationPartialState.Error -> previousState.copy(
                isPublishing = false,
                error = partialState.message
            )
        }
    }

    private fun validateCurrentStep(): Boolean {
        val state = uiStateSnapshot.value
        return when (state.currentStep) {
            0 -> state.title.isNotBlank() && state.description.isNotBlank()
            1 -> state.stops.isNotEmpty()
            2 -> state.startDateTime != null
            else -> true
        }
    }

    private fun publishTour() {
        viewModelScope.launch {
            // TODO: Implement tour publishing
        }
    }

    private fun loadTourForEditing(tourId: String) {
        // TODO: Load tour data
    }
}

// States
data class TourCreationUiState(
    val currentStep: Int = 0,
    val title: String = "",
    val theme: TourTheme? = null,
    val description: String = "",
    val coverImageUri: String? = null,
    val stops: List<TourStop> = emptyList(),
    val startDateTime: Long? = null,
    val price: Double = 0.0,
    val recurrenceRule: String? = null,
    val isPublishing: Boolean = false,
    val error: String? = null
)

sealed interface TourCreationPartialState {
    data class BasicInfoUpdated(
        val title: String,
        val theme: TourTheme?,
        val description: String,
        val coverImageUri: String?
    ) : TourCreationPartialState

    data class StopAdded(val stop: TourStop) : TourCreationPartialState
    data class StopUpdated(val index: Int, val stop: TourStop) : TourCreationPartialState
    data class StopRemoved(val index: Int) : TourCreationPartialState
    data class StopsReordered(val fromIndex: Int, val toIndex: Int) : TourCreationPartialState

    data class ScheduleUpdated(
        val startDateTime: Long,
        val price: Double,
        val recurrenceRule: String?
    ) : TourCreationPartialState

    data class StepChanged(val step: Int) : TourCreationPartialState
    object Publishing : TourCreationPartialState
    data class Published(val tourId: String) : TourCreationPartialState
    data class Error(val message: String) : TourCreationPartialState
}

sealed interface TourCreationIntent {
    data class UpdateBasicInfo(
        val title: String,
        val theme: TourTheme?,
        val description: String,
        val coverImageUri: String?
    ) : TourCreationIntent

    data class AddStop(val stop: TourStop) : TourCreationIntent
    data class UpdateStop(val index: Int, val stop: TourStop) : TourCreationIntent
    data class RemoveStop(val index: Int) : TourCreationIntent
    data class ReorderStops(val fromIndex: Int, val toIndex: Int) : TourCreationIntent

    data class UpdateSchedule(
        val startDateTime: Long,
        val price: Double,
        val recurrenceRule: String?
    ) : TourCreationIntent

    object NextStep : TourCreationIntent
    object PreviousStep : TourCreationIntent
    object PublishTour : TourCreationIntent
}

sealed interface TourCreationEvent {
    data class NavigateToTourDetail(val tourId: String) : TourCreationEvent
}

// Data models
enum class TourTheme {
    HISTORICAL,
    CULTURAL,
    FOOD,
    ARCHITECTURE,
    NATURE,
    ADVENTURE,
    PHOTOGRAPHY,
    OTHER
}

data class TourStop(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val mediaUrls: List<String> = emptyList(),
    val audioUrl: String? = null,
    val order: Int = 0
)