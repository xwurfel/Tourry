package com.xwurfel.tourry.feature.tours.domain.model

data class TourParticipation(
    val id: String,
    val tourId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val joinedAt: Long,
    val status: ParticipationStatus,
    val completionPercentage: Float,
    val rating: Int? = null,
    val review: String? = null,
    val reviewedAt: Long? = null
)