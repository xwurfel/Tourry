package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import javax.inject.Inject

class SearchToursUseCase @Inject constructor(
    private val tourRepository: TourRepository
) {
    suspend operator fun invoke(
        query: String,
        themes: List<String> = emptyList(),
        maxPrice: Double? = null,
        maxDistance: Float? = null,
        userLocation: Pair<Double, Double>? = null
    ): DomainResult<List<Tour>> {
        return tourRepository.searchTours(
            query = query,
            themes = themes,
            maxPrice = maxPrice,
            maxDistance = maxDistance,
            userLocation = userLocation
        )
    }
}