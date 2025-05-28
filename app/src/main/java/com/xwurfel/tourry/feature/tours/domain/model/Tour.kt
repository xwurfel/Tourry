package com.xwurfel.tourry.feature.tours.domain.model

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
    val isLive: Boolean,
    val stops: List<TourStop>,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long
) {
    val isFree: Boolean get() = price <= 0.0
    val spotsLeft: Int? get() = maxParticipants?.let { it - currentParticipants }
    val distance: Float get() = stops.size * 0.3f // Rough calculation
}