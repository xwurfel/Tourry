package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.feature.profile.domain.usecase.GetCurrentUserIdUseCase
import com.xwurfel.tourry.feature.tours.domain.model.ParticipationStatus
import com.xwurfel.tourry.feature.tours.domain.model.TourStats
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetTourStatsUseCase @Inject constructor(
    private val tourRepository: TourRepository,
    private val observeUserParticipationsUseCase: ObserveUserParticipationsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    suspend operator fun invoke(tourId: String): DomainResult<TourStats> {
        return try {
            // Get tour details for basic info
            val tour = tourRepository.getTourById(tourId).let { result ->
                when (result) {
                    is DomainResult.Success -> result.data
                    is DomainResult.Failure -> return result
                }
            }

            // Get current user ID
            val userId = getCurrentUserIdUseCase().first()
                ?: return DomainResult.Failure(
                    com.xwurfel.tourry.core.domain.error.DomainError.UnauthorizedError("User not authenticated")
                )

            // Get user's participation for this tour
            val participations = observeUserParticipationsUseCase(userId).first()
            val userParticipation = participations.find {
                it.tourId == tourId && it.status == ParticipationStatus.COMPLETED
            }

            // Calculate stats based on tour and participation data
            val stats = if (userParticipation != null) {
                TourStats(
                    durationMinutes = tour.duration,
                    distanceKm = tour.distance,
                    stopsVisited = (tour.stops.size * userParticipation.completionPercentage).toInt(),
                    totalStops = tour.stops.size,
                    completionPercentage = userParticipation.completionPercentage
                )
            } else {
                // Fallback for cases where participation data isn't available
                // Generate realistic stats for demo purposes
                TourStats(
                    durationMinutes = tour.duration,
                    distanceKm = tour.distance,
                    stopsVisited = tour.stops.size,
                    totalStops = tour.stops.size,
                    completionPercentage = 1.0f
                )
            }

            DomainResult.Success(stats)
        } catch (e: Exception) {
            DomainResult.Failure(
                com.xwurfel.tourry.core.domain.error.DomainError.SomethingWentWrongError(
                    com.xwurfel.tourry.core.domain.util.UiText.Raw("Failed to calculate tour stats: ${e.message}")
                )
            )
        }
    }
}