package com.xwurfel.tourry.feature.tours.domain.model

import com.google.android.gms.maps.model.LatLng

// Data models remain the same
data class TourPreview(
    val id: String,
    val title: String,
    val description: String,
    val coverImageUrl: String?,
    val rating: Float,
    val price: Double,
    val isFree: Boolean,
    val isLiveSoon: Boolean,
    val startTime: Long,
    val duration: Int, // in minutes
    val distance: Float? = null, // in km
    val coordinates: LatLng,
)