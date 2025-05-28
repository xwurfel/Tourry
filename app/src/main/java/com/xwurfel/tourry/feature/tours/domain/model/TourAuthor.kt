package com.xwurfel.tourry.feature.tours.domain.model

data class TourAuthor(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val rating: Float = 0f,
    val toursCount: Int = 0
)