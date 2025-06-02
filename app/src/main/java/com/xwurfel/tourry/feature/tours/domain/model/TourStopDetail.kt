package com.xwurfel.tourry.feature.tours.domain.model

data class TourStopDetail(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int
)