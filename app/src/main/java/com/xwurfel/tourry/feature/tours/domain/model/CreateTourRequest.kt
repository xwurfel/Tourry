package com.xwurfel.tourry.feature.tours.domain.model

data class CreateTourRequest(
    val title: String,
    val description: String,
    val theme: String,
    val coverImageUrl: String? = null,
    val price: Double,
    val startTime: Long,
    val duration: Int,
    val maxParticipants: Int? = null,
    val stops: List<CreateTourStop>,
    val tags: List<String>
)