package com.xwurfel.tourry.data.tour.mapper

import androidx.core.net.toUri
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.data.tour.entity.TourEntity
import com.xwurfel.tourry.domain.tour.model.Tour

fun TourEntity.toDomain(): Tour {
    return Tour(
        id = id,
        title = title,
        description = description,
        imageUri = imageUriString?.toUri(),
        meetingPoint = LatLng(meetingPointLatitude, meetingPointLongitude),
        meetingPointAddress = meetingPointAddress,
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        price = price,
        capacity = capacity,
        categoryId = categoryId,
        organizerId = organizerId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Tour.toEntity(): TourEntity {
    return TourEntity(
        id = id,
        title = title,
        description = description,
        imageUriString = imageUri?.toString(),
        meetingPointLatitude = meetingPoint.latitude,
        meetingPointLongitude = meetingPoint.longitude,
        meetingPointAddress = meetingPointAddress,
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        price = price,
        capacity = capacity,
        categoryId = categoryId,
        organizerId = organizerId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}