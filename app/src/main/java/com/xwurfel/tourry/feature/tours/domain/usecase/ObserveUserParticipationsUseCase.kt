package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.feature.tours.domain.model.TourParticipation
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUserParticipationsUseCase @Inject constructor(
    private val tourRepository: TourRepository
) {
    operator fun invoke(userId: String): Flow<List<TourParticipation>> {
        return tourRepository.observeUserParticipations(userId)
    }
}