package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import javax.inject.Inject

class StartTourSessionUseCase @Inject constructor(
    private val tourRepository: TourRepository
) {
    suspend operator fun invoke(
        tourId: String,
        userId: String
    ): DomainResult<String> {
        return tourRepository.startTourSession(tourId, userId)
    }
}
