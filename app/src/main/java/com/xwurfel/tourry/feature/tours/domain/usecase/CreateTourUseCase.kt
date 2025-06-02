package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.tours.domain.model.CreateTourRequest
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import javax.inject.Inject

class CreateTourUseCase @Inject constructor(
    private val tourRepository: TourRepository
) {
    suspend operator fun invoke(request: CreateTourRequest): DomainResult<String> {
        return tourRepository.createTour(request)
    }
}