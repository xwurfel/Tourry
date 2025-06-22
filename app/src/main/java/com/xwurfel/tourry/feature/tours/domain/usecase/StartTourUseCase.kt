package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.core.domain.util.result
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartTourUseCase @Inject constructor(
    private val tourRepository: TourRepository,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(tourId: String): DomainResult<Unit> = result {
        val userId = getCurrentUserIdUseCase().firstOrNull()
            ?: throw Exception("User not authenticated")

        tourRepository.startTour(tourId, userId)
    }
}