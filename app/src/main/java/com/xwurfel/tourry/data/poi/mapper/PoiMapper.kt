package com.xwurfel.tourry.data.poi.mapper

import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.data.poi.entity.PoiEntity
import com.xwurfel.tourry.domain.poi.model.PointOfInterest

fun PoiEntity.toDomain(): PointOfInterest {
    return PointOfInterest(
        id = id,
        title = title,
        description = description,
        imageUri = imageUriString?.let { Uri.parse(it) },
        location = LatLng(latitude, longitude)
    )
}

fun PointOfInterest.toEntity(): PoiEntity {
    return PoiEntity(
        id = id,
        title = title,
        description = description,
        imageUriString = imageUri?.toString(),
        latitude = location.latitude,
        longitude = location.longitude
    )
}