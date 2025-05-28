package com.xwurfel.tourry.feature.profile.data.model

import com.google.firebase.database.PropertyName

data class FirestoreUserStats(
    @PropertyName("toursCreated") val toursCreated: Int = 0,
    @PropertyName("toursJoined") val toursJoined: Int = 0,
    @PropertyName("toursCompleted") val toursCompleted: Int = 0,
    @PropertyName("totalDistance") val totalDistance: Float = 0f,
    @PropertyName("totalDuration") val totalDuration: Int = 0,
    @PropertyName("favoriteThemes") val favoriteThemes: List<String> = emptyList(),
    @PropertyName("averageRating") val averageRating: Float = 0f
)