package com.xwurfel.tourry.feature.tours.domain.model

data class TourGuide(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val rating: Float,
    val toursCount: Int
)