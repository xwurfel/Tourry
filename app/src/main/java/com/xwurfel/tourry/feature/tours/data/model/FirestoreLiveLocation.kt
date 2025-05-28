package com.xwurfel.tourry.feature.tours.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class FirestoreLiveLocation(
    @PropertyName("userId") val userId: String = "",
    @PropertyName("tourId") val tourId: String = "",
    @PropertyName("location") val location: GeoPoint = GeoPoint(0.0, 0.0),
    @PropertyName("accuracy") val accuracy: Float = 0f,
    @PropertyName("timestamp") val timestamp: Timestamp = Timestamp.Companion.now(),
    @PropertyName("currentStopId") val currentStopId: String? = null
)