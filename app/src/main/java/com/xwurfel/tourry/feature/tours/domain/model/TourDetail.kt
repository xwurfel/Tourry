package com.xwurfel.tourry.feature.tours.domain.model

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