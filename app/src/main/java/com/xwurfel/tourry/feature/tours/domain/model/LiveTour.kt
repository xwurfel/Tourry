package com.xwurfel.tourry.feature.tours.domain.model


data class LiveTour(
    val id: String,
    val title: String,
    val description: String,
    val stops: List<LiveTourStop>,
    val authorId: String,
    val authorName: String
)