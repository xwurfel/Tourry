package com.xwurfel.tourry.feature.tours.domain.model

data class StopContent(
    val text: String,
    val imageUrls: List<String>,
    val audioUrl: String? = null,
    val videoUrl: String? = null
)