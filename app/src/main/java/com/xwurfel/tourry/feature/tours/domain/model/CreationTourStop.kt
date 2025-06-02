package com.xwurfel.tourry.feature.tours.domain.model

import java.util.UUID

data class CreationTourStop(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val mediaUrls: List<String> = emptyList(),
    val audioUrl: String? = null,
    val order: Int = 0
)