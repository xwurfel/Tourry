package com.xwurfel.tourry.ui.tour.summary.mapper

import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.model.TourStats

object TourSummaryMapper {

    fun Tour.toSummaryData(): TourSummaryData {
        return TourSummaryData(
            tourTitle = title,
            coverImageUrl = coverImageUrl,
            tourId = id
        )
    }

    fun TourStats.toUiTourStats(): TourStats {
        return TourStats(
            durationMinutes = durationMinutes,
            distanceKm = distanceKm,
            stopsVisited = stopsVisited,
            totalStops = totalStops,
            completionPercentage = completionPercentage
        )
    }
}

data class TourSummaryData(
    val tourTitle: String,
    val coverImageUrl: String?,
    val tourId: String
)