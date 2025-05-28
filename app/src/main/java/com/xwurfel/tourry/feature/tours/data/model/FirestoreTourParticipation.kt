package com.xwurfel.tourry.feature.tours.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class FirestoreTourParticipation(
    @PropertyName("id") val id: String = "",
    @PropertyName("tourId") val tourId: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("userName") val userName: String = "",
    @PropertyName("userAvatarUrl") val userAvatarUrl: String? = null,
    @PropertyName("joinedAt") val joinedAt: Timestamp = Timestamp.Companion.now(),
    @PropertyName("status") val status: String = "joined", // joined, completed, cancelled
    @PropertyName("completionPercentage") val completionPercentage: Float = 0f,
    @PropertyName("rating") val rating: Int? = null,
    @PropertyName("review") val review: String? = null,
    @PropertyName("reviewedAt") val reviewedAt: Timestamp? = null
)