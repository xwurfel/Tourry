package com.xwurfel.tourry.feature.tours.domain.model

// Data models
data class TourStats(
    val durationMinutes: Int,
    val distanceKm: Float,
    val stopsVisited: Int,
    val totalStops: Int,
    val completionPercentage: Float
)