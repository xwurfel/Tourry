package com.xwurfel.tourry.feature.tours.domain.model

data class LiveTourStop(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int,
    val geofenceRadius: Float = 50f,
    val content: StopContent? = null,
    val isVisited: Boolean = false,
    val isActive: Boolean = false
)
