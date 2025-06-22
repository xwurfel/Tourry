package com.xwurfel.tourry.ui.explore.mapper

import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.model.TourPreview

object TourPreviewMapper {

    fun Tour.toTourPreview(): TourPreview {
        return TourPreview(
            id = id,
            title = title,
            description = description,
            coverImageUrl = coverImageUrl,
            rating = rating,
            price = price,
            isFree = isFree,
            isLiveSoon = isLive,
            startTime = startTime,
            duration = duration,
            distance = distance,
            coordinates = LatLng(
                stops.first().latitude,
                stops.first().longitude
            )
        )
    }

    fun List<Tour>.toTourPreviews(): List<TourPreview> {
        return map { it.toTourPreview() }
    }
}