package com.xwurfel.tourry.feature.discovery.api.model

import com.google.gson.annotations.SerializedName

data class TourPreviewDto(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String?,
    val location: String,
    val distance: Double?,
    @SerializedName("duration_minutes")
    val durationMinutes: Int,
    val difficulty: String,
    val category: String,
    val rating: Float,
    @SerializedName("review_count")
    val reviewCount: Int,
    val price: Double?,
    @SerializedName("is_bookmarked")
    val isBookmarked: Boolean
)

data class TourDetailsDto(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("creator_name")
    val creatorName: String,
    @SerializedName("creator_id")
    val creatorId: String,
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String?,
    @SerializedName("image_urls")
    val imageUrls: List<String>,
    val location: String,
    @SerializedName("start_latitude")
    val startLatitude: Double?,
    @SerializedName("start_longitude")
    val startLongitude: Double?,
    val distance: Double?,
    @SerializedName("duration_minutes")
    val durationMinutes: Int,
    val difficulty: String,
    val category: String,
    val rating: Float,
    @SerializedName("review_count")
    val reviewCount: Int,
    val price: Double?,
    @SerializedName("is_bookmarked")
    val isBookmarked: Boolean,
    @SerializedName("waypoint_count")
    val waypointCount: Int,
    val highlights: List<String>
)

data class BookmarkRequestDto(
    @SerializedName("tour_id")
    val tourId: String
)