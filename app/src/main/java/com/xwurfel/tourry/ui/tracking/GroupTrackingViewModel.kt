package com.xwurfel.tourry.ui.tracking

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.feature.auth.domain.repository.AuthRepository
import com.xwurfel.tourry.feature.service.geofence.GeofenceManager
import com.xwurfel.tourry.feature.tracking.domain.model.GeofenceSettings
import com.xwurfel.tourry.feature.tracking.domain.model.GroupStatus
import com.xwurfel.tourry.feature.tracking.domain.model.MemberLocation
import com.xwurfel.tourry.feature.tracking.domain.model.MemberStatus
import com.xwurfel.tourry.feature.tracking.domain.model.TourGroup
import com.xwurfel.tourry.feature.tracking.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class GroupTrackingState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val group: TourGroup? = null,
    val membersLocations: Map<String, MemberLocation> = emptyMap(),
    val guideLocation: MemberLocation? = null,
    val currentUserLocation: MemberLocation? = null,
    val currentUserId: String? = null,
    val isUserGuide: Boolean = false,
    val isUserInGeofence: Boolean = true,
    val geofenceSettings: GeofenceSettings = GeofenceSettings(),
    val isTracking: Boolean = false
)

@HiltViewModel
class GroupTrackingViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authRepository: AuthRepository,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    private val _state = MutableStateFlow(GroupTrackingState())
    val state: StateFlow<GroupTrackingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Get current user ID
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _state.update { it.copy(currentUserId = user.id) }
            }
        }
    }

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            groupRepository.getGroup(groupId).fold(
                onSuccess = { group ->
                    val currentUserId = _state.value.currentUserId
                    val isUserGuide = currentUserId == group.guideId
                    val userMember = group.members.find { it.userId == currentUserId }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            group = group,
                            isUserGuide = isUserGuide,
                            error = null
                        )
                    }

                    // Start observing locations
                    observeGroupLocations(groupId)

                    // Update geofence if user is a guide
                    if (isUserGuide) {
                        updateGeofenceFromGuideLocation()
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load group"
                        )
                    }
                }
            )
        }
    }

    private fun observeGroupLocations(groupId: String) {
        viewModelScope.launch {
            groupRepository.observeGroupLocations(groupId).collectLatest { locations ->
                val guideId = _state.value.group?.guideId
                val currentUserId = _state.value.currentUserId

                val guideLocation = guideId?.let { locations[it] }
                val currentUserLocation = currentUserId?.let { locations[it] }

                _state.update {
                    it.copy(
                        membersLocations = locations,
                        guideLocation = guideLocation,
                        currentUserLocation = currentUserLocation,
                        isUserInGeofence = currentUserLocation?.isInGeofence ?: true
                    )
                }

                // If guide location changes and user is not guide, update geofence check
                if (guideLocation != null && currentUserId != guideId) {
                    checkIfInGeofence(guideLocation.toLatLng(), currentUserLocation)
                }
            }
        }
    }

    fun startTracking() {
        val group = _state.value.group ?: return
        val currentUserId = _state.value.currentUserId ?: return

        viewModelScope.launch {
            // Update member status to ACTIVE
            groupRepository.updateMemberStatus(
                groupId = group.id,
                userId = currentUserId,
                status = MemberStatus.ACTIVE
            )

            _state.update { it.copy(isTracking = true) }
        }
    }

    fun stopTracking() {
        val group = _state.value.group ?: return
        val currentUserId = _state.value.currentUserId ?: return

        viewModelScope.launch {
            // Update member status to INACTIVE
            groupRepository.updateMemberStatus(
                groupId = group.id,
                userId = currentUserId,
                status = MemberStatus.INACTIVE
            )

            _state.update { it.copy(isTracking = false) }
        }
    }

    fun updateCurrentLocation(location: Location) {
        val group = _state.value.group ?: return
        val currentUserId = _state.value.currentUserId ?: return
        val isUserGuide = _state.value.isUserGuide

        groupRepository.updateLocation(
            groupId = group.id,
            userId = currentUserId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy
        )

        val currentLocation = MemberLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = Instant.now(),
            isInGeofence = true // Will be updated by geofence check
        )

        _state.update {
            it.copy(
                currentUserLocation = currentLocation
            )
        }

        // If user is guide, update geofence
        if (isUserGuide) {
            updateGeofenceFromGuideLocation()
        } else {
            // Check if in geofence based on guide's location
            val guideLocation = _state.value.guideLocation
            if (guideLocation != null) {
                checkIfInGeofence(guideLocation.toLatLng(), currentLocation)
            }
        }
    }

    fun requestRegroup() {
        val group = _state.value.group ?: return

        viewModelScope.launch {
            groupRepository.requestRegroup(group.id).fold(
                onSuccess = {
                    // Success, notification will be sent to all members
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            error = "Failed to request regroup: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun startTour() {
        val group = _state.value.group ?: return
        if (!_state.value.isUserGuide) return

        viewModelScope.launch {
            groupRepository.updateGroupStatus(
                groupId = group.id,
                status = GroupStatus.ACTIVE,
                currentWaypointIndex = 0
            ).fold(
                onSuccess = { updatedGroup ->
                    _state.update {
                        it.copy(
                            group = updatedGroup
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            error = "Failed to start tour: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun pauseTour() {
        val group = _state.value.group ?: return
        if (!_state.value.isUserGuide) return

        viewModelScope.launch {
            groupRepository.updateGroupStatus(
                groupId = group.id,
                status = GroupStatus.PAUSED
            ).fold(
                onSuccess = { updatedGroup ->
                    _state.update {
                        it.copy(
                            group = updatedGroup
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            error = "Failed to pause tour: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun endTour() {
        val group = _state.value.group ?: return
        if (!_state.value.isUserGuide) return

        viewModelScope.launch {
            groupRepository.updateGroupStatus(
                groupId = group.id,
                status = GroupStatus.COMPLETED
            ).fold(
                onSuccess = { updatedGroup ->
                    _state.update {
                        it.copy(
                            group = updatedGroup
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            error = "Failed to end tour: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    fun updateGeofenceSettings(settings: GeofenceSettings) {
        _state.update {
            it.copy(
                geofenceSettings = settings
            )
        }

        updateGeofenceFromGuideLocation()
    }

    private fun updateGeofenceFromGuideLocation() {
        val group = _state.value.group ?: return
        val guideLocation = _state.value.currentUserLocation ?: return
        val geofenceSettings = _state.value.geofenceSettings

        val center = LatLng(guideLocation.latitude, guideLocation.longitude)
        geofenceManager.updateGroupGeofence(
            groupId = group.id,
            center = center,
            radiusMeters = geofenceSettings.radiusMeters
        )
    }

    private fun checkIfInGeofence(guideLatLng: LatLng, userLocation: MemberLocation?) {
        if (userLocation == null) return

        val group = _state.value.group ?: return
        val currentUserId = _state.value.currentUserId ?: return
        val geofenceSettings = _state.value.geofenceSettings

        val userLatLng = LatLng(userLocation.latitude, userLocation.longitude)

        // Calculate distance between guide and user
        val results = FloatArray(1)
        Location.distanceBetween(
            guideLatLng.latitude, guideLatLng.longitude,
            userLatLng.latitude, userLatLng.longitude,
            results
        )

        val distance = results[0]
        val isInGeofence = distance <= geofenceSettings.radiusMeters

        // Update user's geofence status if it changed
        if (isInGeofence != userLocation.isInGeofence) {
            val updatedLocation = userLocation.copy(isInGeofence = isInGeofence)

            groupRepository.updateLocation(
                groupId = group.id,
                userId = currentUserId,
                latitude = userLocation.latitude,
                longitude = userLocation.longitude,
                accuracy = userLocation.accuracy
            )

            _state.update {
                it.copy(
                    isUserInGeofence = isInGeofence,
                    currentUserLocation = updatedLocation
                )
            }
        }
    }
}