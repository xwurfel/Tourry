package com.xwurfel.tourry.ui.tour.detail.mapper

import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.model.TourDetail
import com.xwurfel.tourry.feature.tours.domain.model.TourGuide
import com.xwurfel.tourry.feature.tours.domain.model.TourStopDetail

object TourDetailMapper {

    fun Tour.toTourDetail(isJoined: Boolean = false): TourDetail {
        return TourDetail(
            id = id,
            title = title,
            description = description,
            coverImageUrl = coverImageUrl,
            theme = theme,
            rating = rating,
            reviewsCount = reviewsCount,
            duration = duration,
            distance = distance,
            price = price,
            isFree = isFree,
            startTime = startTime,
            isLive = isLive,
            isJoined = isJoined,
            spotsLeft = spotsLeft,
            guide = TourGuide(
                id = author.id,
                name = author.name,
                avatarUrl = author.avatarUrl,
                rating = author.rating,
                toursCount = author.toursCount
            ),
            stops = stops.map { stop ->
                TourStopDetail(
                    id = stop.id,
                    name = stop.name,
                    description = stop.description,
                    latitude = stop.latitude,
                    longitude = stop.longitude,
                    order = stop.order
                )
            }
        )
    }
}