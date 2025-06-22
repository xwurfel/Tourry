package com.xwurfel.tourry.feature.tours.domain.model

data class MyTour(
    val id: String,
    val title: String,
    val coverImageUrl: String?,
    val startTime: Long,
    val status: TourStatus,
    val participantsCount: Int = 0,
    val rating: Float? = null
)