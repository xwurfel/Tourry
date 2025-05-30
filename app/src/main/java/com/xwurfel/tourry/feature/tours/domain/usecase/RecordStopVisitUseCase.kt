package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import javax.inject.Inject

class RecordStopVisitUseCase @Inject constructor(
    private val tourRepository: TourRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        stopId: String,
        timestamp: Long,
        userLocation: Pair<Double, Double>
    ): DomainResult<Unit> {
        return tourRepository.recordStopVisit(sessionId, stopId, timestamp, userLocation)
    }
}