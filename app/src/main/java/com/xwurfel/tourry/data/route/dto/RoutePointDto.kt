package com.xwurfel.tourry.data.route.dto

import com.google.android.gms.maps.model.LatLng

data class RoutePointDto(
    val id: Long = 0,
    val tourId: Long,
    val location: LatLng,
    val title: String,
    val description: String,
    val order: Int,
    val durationMinutes: Int? = null,
    val arrivalInstructions: String? = null,
    val imageUri: String? = null
)