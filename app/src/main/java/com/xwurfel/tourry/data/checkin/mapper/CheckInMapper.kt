package com.xwurfel.tourry.data.checkin.mapper

import com.xwurfel.tourry.data.checkin.entity.CheckInEntity
import com.xwurfel.tourry.domain.tour.model.CheckIn

fun CheckInEntity.toDomain(): CheckIn {
    return CheckIn(
        id = id,
        userId = userId,
        tourId = tourId,
        routePointId = routePointId,
        timestamp = timestamp,
        note = note,
        imageUri = imageUriString
    )
}

fun CheckIn.toEntity(): CheckInEntity {
    return CheckInEntity(
        id = id,
        userId = userId,
        tourId = tourId,
        routePointId = routePointId,
        timestamp = timestamp,
        note = note,
        imageUriString = imageUri
    )
}