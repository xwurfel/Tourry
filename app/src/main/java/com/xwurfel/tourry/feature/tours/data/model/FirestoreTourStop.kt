package com.xwurfel.tourry.feature.tours.data.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class FirestoreTourStop(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("location") val location: GeoPoint = GeoPoint(0.0, 0.0),
    @PropertyName("order") val order: Int = 0,
    @PropertyName("geofenceRadius") val geofenceRadius: Float = 50f,
    @PropertyName("content") val content: FirestoreStopContent? = null
)