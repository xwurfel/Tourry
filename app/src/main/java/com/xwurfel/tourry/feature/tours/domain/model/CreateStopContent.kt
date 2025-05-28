package com.xwurfel.tourry.feature.tours.domain.model

data class CreateStopContent(
    val text: String,
    val imageUrls: List<String> = emptyList(),
    val audioUrl: String? = null,
    val videoUrl: String? = null
)
