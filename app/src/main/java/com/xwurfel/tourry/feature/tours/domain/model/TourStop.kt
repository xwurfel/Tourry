package com.xwurfel.tourry.feature.tours.domain.model

data class TourStop(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int,
    val content: StopContent? = null
)
