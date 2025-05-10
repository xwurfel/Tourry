package com.xwurfel.tourry.ui.tourbuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.feature.discovery.domain.model.TourCategory
import com.xwurfel.tourry.feature.discovery.domain.model.TourDifficulty
import com.xwurfel.tourry.feature.tourbuilder.domain.model.ContentDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.model.ContentType
import com.xwurfel.tourry.feature.tourbuilder.domain.model.TourDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.model.WaypointDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.repository.RouteInfo
import com.xwurfel.tourry.feature.tourbuilder.domain.repository.TourBuilderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class TourBuilderState(
    val isLoading: Boolean = false,
    val tourDraft: TourDraft = TourDraft(),
    val selectedWaypointId: String? = null,
    val selectedContentId: String? = null,
    val routeInfo: RouteInfo? = null,
    val isEditingWaypoint: Boolean = false,
    val isEditingContent: Boolean = false,
    val error: String? = null,
    val operationSuccess: String? = null,
    val uploadInProgress: Boolean = false,
    val saveInProgress: Boolean = false,
    val isMapReady: Boolean = false,
    val currentLocation: LatLng? = null,
    val focusOnLocationRequested: Boolean = false
)

@HiltViewModel
class TourBuilderViewModel @Inject constructor(
    private val repository: TourBuilderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TourBuilderState())
    val state: StateFlow<TourBuilderState> = _state.asStateFlow()

    init {
        loadDraftInProgress()
    }

    private fun loadDraftInProgress() {
        viewModelScope.launch {
            repository.getDraftInProgress().collect { draft ->
                if (draft != null) {
                    _state.value = _state.value.copy(
                        tourDraft = draft,
                        error = null
                    )
                    calculateRoute()
                }
            }
        }
    }

    fun loadTourDraft(tourId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.getTourDraft(tourId).fold(
                onSuccess = { tourDraft ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        tourDraft = tourDraft,
                        error = null
                    )
                    calculateRoute()
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun updateTourBasicInfo(
        title: String? = null,
        description: String? = null,
        location: String? = null,
        category: TourCategory? = null,
        difficulty: TourDifficulty? = null,
        price: Double? = null,
        isPublic: Boolean? = null,
        highlights: List<String>? = null
    ) {
        val currentDraft = _state.value.tourDraft
        val updatedDraft = currentDraft.copy(
            title = title ?: currentDraft.title,
            description = description ?: currentDraft.description,
            location = location ?: currentDraft.location,
            category = category ?: currentDraft.category,
            difficulty = difficulty ?: currentDraft.difficulty,
            price = price ?: currentDraft.price,
            isPublic = isPublic ?: currentDraft.isPublic,
            highlights = highlights ?: currentDraft.highlights
        )

        _state.value = _state.value.copy(tourDraft = updatedDraft)
        saveDraftLocally()
    }

    fun addWaypoint(position: LatLng) {
        val currentDraft = _state.value.tourDraft
        val newOrder = currentDraft.waypoints.size

        val newWaypoint = WaypointDraft(
            id = UUID.randomUUID().toString(),
            position = position,
            order = newOrder,
            title = "Waypoint ${newOrder + 1}"
        )

        val updatedWaypoints = currentDraft.waypoints + newWaypoint
        val updatedDraft = currentDraft.copy(waypoints = updatedWaypoints)

        _state.value = _state.value.copy(
            tourDraft = updatedDraft,
            selectedWaypointId = newWaypoint.id
        )

        calculateRoute()
        saveDraftLocally()
    }

    fun updateWaypoint(
        waypointId: String,
        title: String? = null,
        description: String? = null,
        position: LatLng? = null,
        durationMinutes: Int? = null,
        geofenceRadius: Float? = null
    ) {
        val currentDraft = _state.value.tourDraft
        val waypointIndex = currentDraft.waypoints.indexOfFirst { it.id == waypointId }

        if (waypointIndex == -1) return

        val currentWaypoint = currentDraft.waypoints[waypointIndex]
        val updatedWaypoint = currentWaypoint.copy(
            title = title ?: currentWaypoint.title,
            description = description ?: currentWaypoint.description,
            position = position ?: currentWaypoint.position,
            durationMinutes = durationMinutes ?: currentWaypoint.durationMinutes,
            geofenceRadius = geofenceRadius ?: currentWaypoint.geofenceRadius
        )

        val updatedWaypoints = currentDraft.waypoints.toMutableList().apply {
            set(waypointIndex, updatedWaypoint)
        }

        val updatedDraft = currentDraft.copy(waypoints = updatedWaypoints)

        _state.value = _state.value.copy(
            tourDraft = updatedDraft,
            isEditingWaypoint = false
        )

        if (position != null) {
            calculateRoute()
        }

        saveDraftLocally()
    }

    fun deleteWaypoint(waypointId: String) {
        val currentDraft = _state.value.tourDraft
        val updatedWaypoints = currentDraft.waypoints.filter { it.id != waypointId }
            .mapIndexed { index, waypoint -> waypoint.copy(order = index) }

        val updatedDraft = currentDraft.copy(waypoints = updatedWaypoints)

        _state.value = _state.value.copy(
            tourDraft = updatedDraft,
            selectedWaypointId = null
        )

        calculateRoute()
        saveDraftLocally()
    }

    fun reorderWaypoints(fromIndex: Int, toIndex: Int) {
        val currentDraft = _state.value.tourDraft
        val waypoints = currentDraft.waypoints.toMutableList()

        if (fromIndex < 0 || fromIndex >= waypoints.size || toIndex < 0 || toIndex >= waypoints.size) {
            return
        }

        val waypointToMove = waypoints.removeAt(fromIndex)
        waypoints.add(toIndex, waypointToMove)

        val reorderedWaypoints = waypoints.mapIndexed { index, waypoint ->
            waypoint.copy(order = index)
        }

        val updatedDraft = currentDraft.copy(waypoints = reorderedWaypoints)

        _state.value = _state.value.copy(tourDraft = updatedDraft)

        calculateRoute()
        saveDraftLocally()
    }

    fun addContent(
        waypointId: String,
        title: String,
        description: String,
        type: ContentType,
        mediaUrl: String? = null
    ) {
        val currentDraft = _state.value.tourDraft
        val waypointIndex = currentDraft.waypoints.indexOfFirst { it.id == waypointId }

        if (waypointIndex == -1) return

        val waypoint = currentDraft.waypoints[waypointIndex]
        val newOrder = waypoint.contents.size

        val newContent = ContentDraft(
            id = UUID.randomUUID().toString(), // Temporary ID until saved on the server
            title = title,
            description = description,
            type = type,
            mediaUrl = mediaUrl,
            order = newOrder
        )

        val updatedContents = waypoint.contents + newContent
        val updatedWaypoint = waypoint.copy(contents = updatedContents)

        val updatedWaypoints = currentDraft.waypoints.toMutableList().apply {
            set(waypointIndex, updatedWaypoint)
        }

        val updatedDraft = currentDraft.copy(waypoints = updatedWaypoints)

        _state.value = _state.value.copy(
            tourDraft = updatedDraft,
            isEditingContent = false
        )

        saveDraftLocally()
    }

    fun updateContent(
        waypointId: String,
        contentId: String,
        title: String? = null,
        description: String? = null,
        type: ContentType? = null,
        mediaUrl: String? = null
    ) {
        val currentDraft = _state.value.tourDraft
        val waypointIndex = currentDraft.waypoints.indexOfFirst { it.id == waypointId }

        if (waypointIndex == -1) return

        val waypoint = currentDraft.waypoints[waypointIndex]
        val contentIndex = waypoint.contents.indexOfFirst { it.id == contentId }

        if (contentIndex == -1) return

        val content = waypoint.contents[contentIndex]
        val updatedContent = content.copy(
            title = title ?: content.title,
            description = description ?: content.description,
            type = type ?: content.type,
            mediaUrl = mediaUrl ?: content.mediaUrl
        )

        val updatedContents = waypoint.contents.toMutableList().apply {
            set(contentIndex, updatedContent)
        }

        val updatedWaypoint = waypoint.copy(contents = updatedContents)
        val updatedWaypoints = currentDraft.waypoints.toMutableList().apply {
            set(waypointIndex, updatedWaypoint)
        }

        val updatedDraft = currentDraft.copy(waypoints = updatedWaypoints)

        _state.value = _state.value.copy(
            tourDraft = updatedDraft,
            isEditingContent = false
        )

        saveDraftLocally()
    }

    fun deleteContent(waypointId: String, contentId: String) {
        val currentDraft = _state.value.tourDraft
        val waypointIndex = currentDraft.waypoints.indexOfFirst { it.id == waypointId }

        if (waypointIndex == -1) return

        val waypoint = currentDraft.waypoints[waypointIndex]
        val updatedContents = waypoint.contents.filter { it.id != contentId }
            .mapIndexed { index, content -> content.copy(order = index) }

        val updatedWaypoint = waypoint.copy(contents = updatedContents)
        val updatedWaypoints = currentDraft.waypoints.toMutableList().apply {
            set(waypointIndex, updatedWaypoint)
        }

        val updatedDraft = currentDraft.copy(waypoints = updatedWaypoints)

        _state.value = _state.value.copy(tourDraft = updatedDraft)

        saveDraftLocally()
    }

    fun selectContent(contentId: String) {
        val selectedWaypointId = _state.value.selectedWaypointId ?: return
        val waypoint =
            _state.value.tourDraft.waypoints.find { it.id == selectedWaypointId } ?: return
        waypoint.contents.find { it.id == contentId } ?: return

        _state.value = _state.value.copy(
            selectedContentId = contentId,
            isEditingContent = true
        )
    }

    fun uploadImage(file: File) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                uploadInProgress = true,
                error = null
            )

            repository.uploadImage(file).fold(
                onSuccess = { url ->
                    _state.value = _state.value.copy(
                        uploadInProgress = false,
                        operationSuccess = "Image uploaded successfully"
                    )

                    // Add the image URL to the tour or selected waypoint
                    val selectedWaypointId = _state.value.selectedWaypointId
                    if (selectedWaypointId != null && _state.value.isEditingContent) {
                        // Add as new content to the selected waypoint
                        addContent(
                            waypointId = selectedWaypointId,
                            title = "Image",
                            description = "",
                            type = ContentType.IMAGE,
                            mediaUrl = url
                        )
                    } else {
                        // Add to tour images
                        val currentDraft = _state.value.tourDraft
                        val updatedImageUrls = currentDraft.imageUrls + url
                        val updatedDraft = currentDraft.copy(
                            imageUrls = updatedImageUrls,
                            thumbnailUrl = currentDraft.thumbnailUrl ?: url
                        )

                        _state.value = _state.value.copy(tourDraft = updatedDraft)
                        saveDraftLocally()
                    }
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        uploadInProgress = false,
                        error = "Failed to upload image: ${e.message}"
                    )
                }
            )
        }
    }

    fun uploadAudio(file: File) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                uploadInProgress = true,
                error = null
            )

            repository.uploadAudio(file).fold(
                onSuccess = { url ->
                    _state.value = _state.value.copy(
                        uploadInProgress = false,
                        operationSuccess = "Audio uploaded successfully"
                    )

                    // We only add audio to waypoint content
                    val selectedWaypointId = _state.value.selectedWaypointId
                    if (selectedWaypointId != null) {
                        addContent(
                            waypointId = selectedWaypointId,
                            title = "Audio Guide",
                            description = "",
                            type = ContentType.AUDIO,
                            mediaUrl = url
                        )
                    }
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        uploadInProgress = false,
                        error = "Failed to upload audio: ${e.message}"
                    )
                }
            )
        }
    }

    fun saveTour() {
        viewModelScope.launch {
            val currentDraft = _state.value.tourDraft

            // Validate tour data before saving
            if (currentDraft.title.isBlank()) {
                _state.value = _state.value.copy(
                    error = "Tour title cannot be empty"
                )
                return@launch
            }

            if (currentDraft.waypoints.isEmpty()) {
                _state.value = _state.value.copy(
                    error = "Tour must have at least one waypoint"
                )
                return@launch
            }

            _state.value = _state.value.copy(
                saveInProgress = true,
                error = null
            )

            val result = if (currentDraft.id == null) {
                repository.createTour(currentDraft)
            } else {
                repository.updateTour(currentDraft)
            }

            result.fold(
                onSuccess = { savedTour ->
                    _state.value = _state.value.copy(
                        tourDraft = savedTour,
                        saveInProgress = false,
                        operationSuccess = "Tour saved successfully"
                    )
                    repository.clearLocalDraft()
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        saveInProgress = false,
                        error = "Failed to save tour: ${e.message}"
                    )
                    // Keep local draft if save failed
                    saveDraftLocally()
                }
            )
        }
    }

    fun publishTour() {
        viewModelScope.launch {
            val currentDraft = _state.value.tourDraft

            // Validate tour data before publishing
            if (currentDraft.title.isBlank()) {
                _state.value = _state.value.copy(
                    error = "Tour title cannot be empty"
                )
                return@launch
            }

            if (currentDraft.description.isBlank()) {
                _state.value = _state.value.copy(
                    error = "Tour description cannot be empty"
                )
                return@launch
            }

            if (currentDraft.waypoints.isEmpty()) {
                _state.value = _state.value.copy(
                    error = "Tour must have at least one waypoint"
                )
                return@launch
            }

            if (currentDraft.thumbnailUrl == null) {
                _state.value = _state.value.copy(
                    error = "Tour must have a thumbnail image"
                )
                return@launch
            }

            val updatedDraft = currentDraft.copy(isPublic = true)

            _state.value = _state.value.copy(
                saveInProgress = true,
                error = null
            )

            val result = if (updatedDraft.id == null) {
                repository.createTour(updatedDraft)
            } else {
                repository.updateTour(updatedDraft)
            }

            result.fold(
                onSuccess = { savedTour ->
                    _state.value = _state.value.copy(
                        tourDraft = savedTour,
                        saveInProgress = false,
                        operationSuccess = "Tour published successfully"
                    )
                    repository.clearLocalDraft()
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        saveInProgress = false,
                        error = "Failed to publish tour: ${e.message}"
                    )
                    // Keep local draft if publish failed
                    saveDraftLocally()
                }
            )
        }
    }

    fun deleteTour() {
        viewModelScope.launch {
            val currentDraft = _state.value.tourDraft

            if (currentDraft.id == null) {
                // Just clear the local draft
                repository.clearLocalDraft()
                _state.value = TourBuilderState()
                return@launch
            }

            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

            repository.deleteTour(currentDraft.id).fold(
                onSuccess = {
                    _state.value = TourBuilderState(operationSuccess = "Tour deleted successfully")
                    repository.clearLocalDraft()
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to delete tour: ${e.message}"
                    )
                }
            )
        }
    }

    fun calculateRoute() {
        viewModelScope.launch {
            val currentDraft = _state.value.tourDraft
            val waypoints = currentDraft.waypoints

            if (waypoints.size < 2) {
                _state.update { it.copy(routeInfo = null) }
                return@launch
            }

            val positions = waypoints.sortedBy { it.order }.map { it.position }

            repository.calculateRoute(positions).fold(
                onSuccess = { routeInfo ->
                    _state.update {
                        it.copy(
                            routeInfo = routeInfo,
                            tourDraft = it.tourDraft.copy(estimatedDuration = routeInfo.duration)
                        )
                    }
                },
                onFailure = { error ->
                    // Don't show error, just leave routeInfo as null or previous value
                    // This is a non-critical operation
                }
            )
        }
    }

    fun selectWaypoint(waypointId: String?) {
        _state.value = _state.value.copy(
            selectedWaypointId = waypointId,
            isEditingWaypoint = false,
            isEditingContent = false
        )
    }

    fun startEditingWaypoint() {
        _state.value = _state.value.copy(
            isEditingWaypoint = true,
            isEditingContent = false
        )
    }

    fun cancelEditing() {
        _state.value = _state.value.copy(
            isEditingWaypoint = false,
            isEditingContent = false,
            selectedContentId = null
        )
    }

    fun startEditingContent() {
        _state.value = _state.value.copy(
            isEditingContent = true,
            isEditingWaypoint = false
        )
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(
            error = null,
            operationSuccess = null
        )
    }

    fun setMapReady(ready: Boolean) {
        _state.value = _state.value.copy(isMapReady = ready)
    }

    private fun saveDraftLocally() {
        viewModelScope.launch {
            repository.saveDraftLocally(_state.value.tourDraft)
        }
    }

    fun updateCurrentLocation(location: LatLng) {
        _state.value = _state.value.copy(
            currentLocation = location
        )
    }

    fun focusOnCurrentLocation() {
        val currentLocation = _state.value.currentLocation
        if (currentLocation != null) {
            // Just update the state to trigger focus in the UI
            _state.value = _state.value.copy(
                selectedWaypointId = null,
                isEditingWaypoint = false,
                isEditingContent = false
            )
            // Use a special flag to indicate we want to focus on current location
            // rather than creating a new boolean state property
            viewModelScope.launch {
                _state.value = _state.value.copy(
                    focusOnLocationRequested = true
                )
                // Reset the flag after a short delay
                delay(100)
                _state.value = _state.value.copy(
                    focusOnLocationRequested = false
                )
            }
        }
    }
}