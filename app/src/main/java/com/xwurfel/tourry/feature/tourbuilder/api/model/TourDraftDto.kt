package com.xwurfel.tourry.feature.tourbuilder.api.model

import com.google.gson.annotations.SerializedName

data class TourDraftDto(
    val id: String?,
    val title: String,
    val description: String,
    val location: String,
    val category: String,
    val difficulty: String,
    val price: Double?,
    @SerializedName("is_public")
    val isPublic: Boolean,
    val waypoints: List<WaypointDraftDto>,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String?,
    @SerializedName("image_urls")
    val imageUrls: List<String>,
    val highlights: List<String>,
    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int
)

data class WaypointDraftDto(
    val id: String?,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int,
    @SerializedName("duration_minutes")
    val durationMinutes: Int,
    @SerializedName("geofence_radius")
    val geofenceRadius: Float,
    val contents: List<ContentDraftDto>
)

data class ContentDraftDto(
    val id: String?,
    val title: String,
    val description: String,
    val type: String,
    @SerializedName("media_url")
    val mediaUrl: String?,
    val order: Int
)

data class WaypointOrderDto(
    @SerializedName("waypoint_ids")
    val waypointIds: List<String>
)

data class RouteInfoDto(
    val distance: Double,
    @SerializedName("duration_minutes")
    val durationMinutes: Int,
    val path: List<LatLngDto>
)

data class LatLngDto(
    val latitude: Double,
    val longitude: Double
)

data class MediaUploadResponseDto(
    val url: String
)