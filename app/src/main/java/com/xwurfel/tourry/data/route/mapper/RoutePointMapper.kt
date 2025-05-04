package com.xwurfel.tourry.data.route.mapper

import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.data.route.entity.RoutePointEntity
import com.xwurfel.tourry.domain.route.model.RoutePoint

fun RoutePointEntity.toDomain(): RoutePoint {
    return RoutePoint(
        id = id,
        tourId = tourId,
        location = LatLng(latitude, longitude),
        title = title,
        description = description,
        order = order,
        durationMinutes = durationMinutes,
        arrivalInstructions = arrivalInstructions,
        imageUri = imageUriString
    )
}

fun RoutePoint.toEntity(): RoutePointEntity {
    return RoutePointEntity(
        id = id,
        tourId = tourId,
        latitude = location.latitude,
        longitude = location.longitude,
        title = title,
        description = description,
        order = order,
        durationMinutes = durationMinutes,
        arrivalInstructions = arrivalInstructions,
        imageUriString = imageUri
    )
}