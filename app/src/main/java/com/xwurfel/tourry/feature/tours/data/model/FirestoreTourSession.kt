package com.xwurfel.tourry.feature.tours.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class FirestoreTourSession(
    @PropertyName("id") val id: String = "",
    @PropertyName("tourId") val tourId: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("startTime") val startTime: Timestamp = Timestamp.Companion.now(),
    @PropertyName("endTime") val endTime: Timestamp? = null,
    @PropertyName("duration") val duration: Int = 0, // seconds
    @PropertyName("completionPercentage") val completionPercentage: Float = 0f,
    @PropertyName("stopsVisited") val stopsVisited: List<String> = emptyList(),
    @PropertyName("pauseCount") val pauseCount: Int = 0,
    @PropertyName("totalPauseTime") val totalPauseTime: Int = 0, // seconds
    @PropertyName("deviceInfo") val deviceInfo: Map<String, String> = emptyMap()
)