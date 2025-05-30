package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import javax.inject.Inject

class CompleteTourSessionUseCase @Inject constructor(
    private val tourRepository: TourRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        completionPercentage: Float,
        totalDuration: Long
    ): DomainResult<Unit> {
        return tourRepository.completeTourSession(sessionId, completionPercentage, totalDuration)
    }
}
