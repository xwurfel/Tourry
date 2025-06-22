package com.xwurfel.tourry.feature.tours.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class FirestoreTour(
    @PropertyName("id") val id: String = "",
    @PropertyName("title") val title: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("theme") val theme: String = "",
    @PropertyName("coverImageUrl") val coverImageUrl: String? = null,
    @PropertyName("authorId") val authorId: String = "",
    @PropertyName("authorName") val authorName: String = "",
    @PropertyName("authorAvatarUrl") val authorAvatarUrl: String? = null,
    @PropertyName("price") val price: Double = 0.0,
    @PropertyName("currency") val currency: String = "USD",
    @PropertyName("startTime") val startTime: Timestamp = Timestamp.now(),
    @PropertyName("duration") val duration: Int = 0, // minutes
    @PropertyName("maxParticipants") val maxParticipants: Int? = null,
    @PropertyName("currentParticipants") val currentParticipants: Int = 0,
    @PropertyName("rating") val rating: Float = 0f,
    @PropertyName("reviewsCount") val reviewsCount: Int = 0,
    @PropertyName("isActive") val isActive: Boolean = true,
    @PropertyName("isLive") val isLive: Boolean = false,
    @PropertyName("stops") val stops: List<FirestoreTourStop> = emptyList(),
    @PropertyName("tags") val tags: List<String> = emptyList(),
    @PropertyName("createdAt") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("updatedAt") val updatedAt: Timestamp = Timestamp.now(),
    @PropertyName("isManuallyStarted") val isManuallyStarted: Boolean = false,
    @PropertyName("manuallyStartedAt") val manuallyStartedAt: Timestamp? = null,
    @PropertyName("manuallyStartedBy") val manuallyStartedBy: String? = null, // User ID who started the tour
    @PropertyName("isCompleted") val isCompleted: Boolean = false,
    @PropertyName("completedAt") val completedAt: Timestamp? = null,
)

