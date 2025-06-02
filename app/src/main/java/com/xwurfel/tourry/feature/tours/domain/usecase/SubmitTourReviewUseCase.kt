package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import javax.inject.Inject

class SubmitTourReviewUseCase @Inject constructor(
    private val tourRepository: TourRepository
) {
    suspend operator fun invoke(
        tourId: String,
        rating: Int,
        review: String,
        completionPercentage: Float
    ): DomainResult<Unit> {
        return tourRepository.submitReview(
            tourId = tourId,
            rating = rating,
            review = review,
            completionPercentage = completionPercentage
        )
    }
}