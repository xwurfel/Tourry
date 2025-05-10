package com.xwurfel.tourry.feature.tracking.domain.model

import com.google.android.gms.maps.model.LatLng
import java.time.Instant

enum class GroupStatus {
    CREATED, ACTIVE, PAUSED, COMPLETED, CANCELLED
}

enum class GroupRole {
    GUIDE, PARTICIPANT
}

enum class MemberStatus {
    INVITED, JOINED, ACTIVE, INACTIVE, LEFT
}

data class TourGroup(
    val id: String,
    val tourId: String,
    val guideId: String,
    val name: String,
    val joinCode: String,
    val status: GroupStatus,
    val createdAt: Instant,
    val startedAt: Instant? = null,
    val endedAt: Instant? = null,
    val currentWaypointIndex: Int = 0,
    val members: List<GroupMember> = emptyList()
)

data class GroupMember(
    val userId: String,
    val name: String,
    val profilePictureUrl: String?,
    val role: GroupRole,
    val status: MemberStatus,
    val lastLocation: MemberLocation? = null,
    val joinedAt: Instant? = null
)

data class MemberLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Instant,
    val isInGeofence: Boolean = true
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}

data class GeofenceSettings(
    val centerOnGuide: Boolean = true,
    val radiusMeters: Float = 50f,
    val notifyOnExit: Boolean = true
)