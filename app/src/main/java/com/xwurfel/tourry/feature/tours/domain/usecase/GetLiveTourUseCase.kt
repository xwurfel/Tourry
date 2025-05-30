package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.tours.domain.model.LiveTour
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import javax.inject.Inject

class GetLiveTourUseCase @Inject constructor(
    private val tourRepository: TourRepository
) {
    suspend operator fun invoke(tourId: String): DomainResult<LiveTour> {
        return tourRepository.getLiveTour(tourId)
    }
}