package com.xwurfel.tourry.domain.poi.model

import android.net.Uri
import com.google.android.gms.maps.model.LatLng

data class PointOfInterest(
    val id: Long = 0,
    val title: String,
    val description: String,
    val imageUri: Uri?,
    val location: LatLng
)