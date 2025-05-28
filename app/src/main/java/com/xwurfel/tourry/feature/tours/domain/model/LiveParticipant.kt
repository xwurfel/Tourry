package com.xwurfel.tourry.feature.tours.domain.model

data class LiveParticipant(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val lastUpdate: Long,
    val currentStopId: String? = null
)