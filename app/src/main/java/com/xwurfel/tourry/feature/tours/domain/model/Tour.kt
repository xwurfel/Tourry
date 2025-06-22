package com.xwurfel.tourry.feature.tours.domain.model

import com.xwurfel.tourry.feature.tours.domain.usecase.TourStatusHelper

data class Tour(
    val id: String,
    val title: String,
    val description: String,
    val theme: String,
    val coverImageUrl: String? = null,
    val author: TourAuthor,
    val price: Double,
    val currency: String,
    val startTime: Long,
    val duration: Int, // minutes
    val maxParticipants: Int? = null,
    val currentParticipants: Int,
    val rating: Float,
    val reviewsCount: Int,
    val isActive: Boolean,
    val stops: List<TourStop>,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val isManuallyStarted: Boolean = false,
    val manuallyStartedAt: Long? = null,
    val manuallyStartedBy: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isJoined: Boolean = false,
    val spotsLeft: Int?,
) {
    val status: TourStatus
        get() = TourStatusHelper.calculateTourStatus(
            startTime = startTime,
            isManuallyStarted = isManuallyStarted,
            isCompleted = isCompleted,
            isCancelled = !isActive
        )

    val isLive: Boolean
        get() = status == TourStatus.ACTIVE

    val canBeStarted: Boolean
        get() = status == TourStatus.READY_TO_START

    val distance: Float get() = stops.size * 0.3f // Rough calculation

    val isFree: Boolean
        get() = price == 0.0
}