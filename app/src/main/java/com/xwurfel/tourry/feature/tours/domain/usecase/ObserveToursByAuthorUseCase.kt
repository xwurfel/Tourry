package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveToursByAuthorUseCase @Inject constructor(
    private val tourRepository: TourRepository
) {
    operator fun invoke(authorId: String): Flow<List<Tour>> {
        return tourRepository.observeToursByAuthor(authorId)
    }
}