package com.xwurfel.tourry.feature.tours.domain.model

data class CreateTourStop(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int,
    val geofenceRadius: Float = 50f,
    val content: CreateStopContent? = null
)