package com.xwurfel.tourry.domain.tour.model

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime

data class Tour(
    val id: Long = 0,
    val title: String,
    val description: String,
    val imageUri: Uri?,
    val meetingPoint: LatLng,
    val meetingPointAddress: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val price: Double,
    val capacity: Int,
    val categoryId: Long,
    val organizerId: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)