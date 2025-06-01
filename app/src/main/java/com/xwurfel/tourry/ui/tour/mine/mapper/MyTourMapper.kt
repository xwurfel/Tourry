package com.xwurfel.tourry.ui.tour.mine.mapper

import com.xwurfel.tourry.feature.tours.domain.model.ParticipationStatus
import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.model.TourParticipation
import com.xwurfel.tourry.ui.tour.mine.MyTour
import com.xwurfel.tourry.ui.tour.mine.TourStatus

object MyTourMapper {

    fun Tour.toMyTour(rating: Float? = null): MyTour {
        val currentTime = System.currentTimeMillis()

        val status = when {
            isLive -> TourStatus.LIVE
            startTime > currentTime -> TourStatus.UPCOMING
            else -> TourStatus.COMPLETED
        }

        return MyTour(
            id = id,
            title = title,
            coverImageUrl = coverImageUrl,
            startTime = startTime,
            status = status,
            participantsCount = currentParticipants,
            rating = rating
        )
    }

    fun TourParticipation.toMyTour(tour: Tour?): MyTour? {
        if (tour == null) return null

        val currentTime = System.currentTimeMillis()

        val status = when (this.status) {
            ParticipationStatus.COMPLETED -> TourStatus.COMPLETED
            ParticipationStatus.CANCELLED -> TourStatus.COMPLETED
            ParticipationStatus.JOINED -> {
                when {
                    tour.isLive -> TourStatus.LIVE
                    tour.startTime > currentTime -> TourStatus.UPCOMING
                    else -> TourStatus.COMPLETED
                }
            }
        }

        return MyTour(
            id = tourId,
            title = tour.title,
            coverImageUrl = tour.coverImageUrl,
            startTime = tour.startTime,
            status = status,
            participantsCount = tour.currentParticipants,
            rating = rating?.toFloat()
        )
    }
}