package com.xwurfel.tourry.ui.tour.creation

import androidx.lifecycle.SavedStateHandle
import com.xwurfel.tourry.core.ui.MviViewModel
import com.xwurfel.tourry.feature.mock.MockDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class TourCreationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mockDataManager: MockDataManager
) : MviViewModel<TourCreationUiState, TourCreationPartialState, TourCreationEvent, TourCreationIntent>(
    initialState = TourCreationUiState()
) {

    private val tourId: String? = savedStateHandle.get<String>("tourId")

    init {
        if (tourId != null) {
            observeContinuousChanges(
                flow {
                    try {
                        val tourDetail = mockDataManager.getTourDetail(tourId)
                        if (tourDetail != null) {
                            // Convert TourDetail to creation format
                            val stops = tourDetail.stops.map { stop ->
                                TourStop(
                                    id = stop.id,
                                    name = stop.name,
                                    description = stop.description,
                                    latitude = stop.latitude,
                                    longitude = stop.longitude,
                                    order = stop.order
                                )
                            }

                            emit(
                                TourCreationPartialState.TourLoaded(
                                    title = tourDetail.title,
                                    theme = TourTheme.valueOf(tourDetail.theme),
                                    description = tourDetail.description,
                                    coverImageUri = tourDetail.coverImageUrl,
                                    stops = stops,
                                    startDateTime = tourDetail.startTime,
                                    price = tourDetail.price,
                                    recurrenceRule = null // Mock doesn't store this yet
                                )
                            )
                        }
                    } catch (_: Exception) {
                        emit(TourCreationPartialState.Error("Failed to load tour for editing"))
                    }
                }
            )
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
                    val currentStep = uiStateSnapshot.value.currentStep
                    if (currentStep < 3) {
                        emit(TourCreationPartialState.StepChanged(currentStep + 1))
                    }
                } else {
                    emit(TourCreationPartialState.ValidationError(getValidationError()))
                }
            }

            is TourCreationIntent.PreviousStep -> {
                val currentStep = uiStateSnapshot.value.currentStep
                if (currentStep > 0) {
                    emit(TourCreationPartialState.StepChanged(currentStep - 1))
                }
            }

            is TourCreationIntent.PublishTour -> {
                val state = uiStateSnapshot.value

                // Validate all steps before publishing
                if (!validateAllSteps(state)) {
                    emit(TourCreationPartialState.ValidationError("Please complete all required information"))
                    return@flow
                }

                emit(TourCreationPartialState.Publishing)

                try {
                    kotlinx.coroutines.delay(1500) // Simulate API call

                    val newTourId = mockDataManager.createTour(
                        title = state.title,
                        description = state.description,
                        stops = state.stops,
                        startDateTime = state.startDateTime,
                        price = state.price
                    )

                    emit(TourCreationPartialState.Published(newTourId))
                } catch (e: Exception) {
                    emit(TourCreationPartialState.Error("Failed to publish tour: ${e.message}"))
                }
            }

            is TourCreationIntent.SaveDraft -> {
                // TODO: Implement draft saving
                emit(TourCreationPartialState.DraftSaved)
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
                coverImageUri = partialState.coverImageUri,
                validationError = null
            )

            is TourCreationPartialState.StopAdded -> previousState.copy(
                stops = previousState.stops + partialState.stop,
                validationError = null
            )

            is TourCreationPartialState.StopUpdated -> previousState.copy(
                stops = previousState.stops.toMutableList().apply {
                    set(partialState.index, partialState.stop)
                },
                validationError = null
            )

            is TourCreationPartialState.StopRemoved -> previousState.copy(
                stops = previousState.stops.filterIndexed { index, _ -> index != partialState.index },
                validationError = null
            )

            is TourCreationPartialState.StopsReordered -> {
                val mutableStops = previousState.stops.toMutableList()
                val stop = mutableStops.removeAt(partialState.fromIndex)
                mutableStops.add(partialState.toIndex, stop)
                previousState.copy(
                    stops = mutableStops,
                    validationError = null
                )
            }

            is TourCreationPartialState.ScheduleUpdated -> previousState.copy(
                startDateTime = partialState.startDateTime,
                price = partialState.price,
                recurrenceRule = partialState.recurrenceRule,
                validationError = null
            )

            is TourCreationPartialState.StepChanged -> previousState.copy(
                currentStep = partialState.step,
                validationError = null
            )

            is TourCreationPartialState.Publishing -> previousState.copy(
                isPublishing = true,
                validationError = null
            )

            is TourCreationPartialState.Published -> {
                publishEvent(TourCreationEvent.NavigateToTourDetail(partialState.tourId))
                previousState.copy(
                    isPublishing = false,
                    validationError = null
                )
            }

            is TourCreationPartialState.DraftSaved -> previousState.copy(
                validationError = null
            )

            is TourCreationPartialState.ValidationError -> previousState.copy(
                validationError = partialState.message
            )

            is TourCreationPartialState.Error -> previousState.copy(
                isPublishing = false,
                validationError = partialState.message
            )

            is TourCreationPartialState.TourLoaded -> previousState.copy(
                title = partialState.title,
                theme = partialState.theme,
                description = partialState.description,
                coverImageUri = partialState.coverImageUri,
                stops = partialState.stops,
                startDateTime = partialState.startDateTime,
                price = partialState.price,
                recurrenceRule = partialState.recurrenceRule,
                isEditing = true
            )
        }
    }

    private fun validateCurrentStep(): Boolean {
        val state = uiStateSnapshot.value
        return when (state.currentStep) {
            0 -> state.title.isNotBlank() && state.description.isNotBlank()
            1 -> state.stops.isNotEmpty() && state.stops.all { it.name.isNotBlank() && it.description.isNotBlank() }
            2 -> state.startDateTime != null// && state.startDateTime > System.currentTimeMillis()
            3 -> true // Preview step always valid
            else -> true
        }
    }

    private fun getValidationError(): String {
        val state = uiStateSnapshot.value
        return when (state.currentStep) {
            0 -> when {
                state.title.isBlank() -> "Please enter a tour title"
                state.description.isBlank() -> "Please enter a tour description"
                else -> "Please fill in all required fields"
            }

            1 -> when {
                state.stops.isEmpty() -> "Please add at least one stop to your tour"
                state.stops.any { it.name.isBlank() } -> "All stops must have a name"
                state.stops.any { it.description.isBlank() } -> "All stops must have a description"
                else -> "Please complete all stop information"
            }

            2 -> when {
                state.startDateTime == null -> "Please select a start date and time"
                state.startDateTime <= System.currentTimeMillis() -> "Start time must be in the future"
                else -> "Please complete the schedule information"
            }

            else -> "Unknown validation error"
        }
    }

    private fun validateAllSteps(state: TourCreationUiState): Boolean {
        return state.title.isNotBlank() &&
                state.description.isNotBlank() &&
                state.stops.isNotEmpty() &&
                state.stops.all { it.name.isNotBlank() && it.description.isNotBlank() } &&
                state.startDateTime != null &&
                state.startDateTime > System.currentTimeMillis()
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
    val isEditing: Boolean = false,
    val validationError: String? = null
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
    object DraftSaved : TourCreationPartialState
    data class ValidationError(val message: String) : TourCreationPartialState
    data class Error(val message: String) : TourCreationPartialState

    data class TourLoaded(
        val title: String,
        val theme: TourTheme?,
        val description: String,
        val coverImageUri: String?,
        val stops: List<TourStop>,
        val startDateTime: Long?,
        val price: Double,
        val recurrenceRule: String?
    ) : TourCreationPartialState
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
    object SaveDraft : TourCreationIntent
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