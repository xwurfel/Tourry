package com.xwurfel.tourry.data.network.dto

data class RoutePointDto(
    val id: Long,
    val tourId: Long,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val durationMinutes: Int?,
    val arrivalInstructions: String?,
    val imageUrl: String?
)

data class RoutePointCreateDto(
    val tourId: Long,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val description: String,
    val orderIndex: Int,
    val durationMinutes: Int? = null,
    val arrivalInstructions: String? = null,
    val imageUrl: String? = null
)

data class TourRouteUpdateDto(
    val tourId: Long,
    val routePoints: List<RoutePointCreateDto>
)