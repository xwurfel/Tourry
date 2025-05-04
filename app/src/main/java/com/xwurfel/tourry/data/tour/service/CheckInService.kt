package com.xwurfel.tourry.domain.tour.service

import android.location.Location
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.domain.route.repository.RouteRepository
import com.xwurfel.tourry.domain.tour.model.CheckIn
import com.xwurfel.tourry.domain.tour.repository.CheckInRepository
import com.xwurfel.tourry.domain.user.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInService @Inject constructor(
    private val checkInRepository: CheckInRepository,
    private val routeRepository: RouteRepository,
    private val userRepository: UserRepository
) {
    companion object {
        private const val DEFAULT_CHECK_IN_RADIUS = 100.0
    }

    /**
     * Checks a user into a tour route point.
     *
     * @param userId The ID of the user checking in
     * @param tourId The ID of the tour
     * @param routePointId The ID of the route point
     * @param userLocation The current location of the user
     * @param note Optional note for the check-in
     * @param imageUri Optional image URI for the check-in
     * @param forceCheckIn If true, allows check-in regardless of distance (for testing or admin use)
     * @return Result containing the created CheckIn on success, or an error message on failure
     */
    suspend fun checkInToRoutePoint(
        userId: Long,
        tourId: Long,
        routePointId: Long,
        userLocation: LatLng,
        note: String? = null,
        imageUri: Uri? = null,
        forceCheckIn: Boolean = false
    ): Result<CheckIn> {
        try {
            val routePoint = routeRepository.getRoutePointById(routePointId)
                ?: return Result.failure(IllegalArgumentException("Route point not found"))

            val existingCheckIn =
                checkInRepository.getCheckInByUserTourAndRoutePoint(userId, tourId, routePointId)
            if (existingCheckIn != null) {
                return Result.failure(IllegalArgumentException("Already checked in to this point"))
            }

            if (!forceCheckIn) {
                val distance = calculateDistance(userLocation, routePoint.location)
                if (distance > DEFAULT_CHECK_IN_RADIUS) {
                    return Result.failure(
                        IllegalArgumentException(
                            "Too far from check-in point (${distance.toInt()}m away, need to be within ${DEFAULT_CHECK_IN_RADIUS.toInt()}m)"
                        )
                    )
                }
            }

            val checkIn = CheckIn(
                userId = userId,
                tourId = tourId,
                routePointId = routePointId,
                timestamp = LocalDateTime.now(),
                note = note,
                imageUri = imageUri?.toString()
            )

            val checkInId = checkInRepository.saveCheckIn(checkIn)
            return Result.success(checkIn.copy(id = checkInId))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun getUserTourCheckIns(userId: Long, tourId: Long): Flow<List<CheckIn>> {
        return checkInRepository.getCheckInsByUserAndTour(userId, tourId)
    }

    suspend fun getUserTourCheckInsCount(userId: Long, tourId: Long): Int {
        return checkInRepository.getCheckInsCountByUserAndTour(userId, tourId)
    }

    suspend fun getTourRoutePointsWithCheckInStatus(
        userId: Long,
        tourId: Long
    ): List<Pair<RoutePoint, Boolean>> {
        val routePoints = routeRepository.getRoutePointsForTour(tourId)
        val result = mutableListOf<Pair<RoutePoint, Boolean>>()

        for (routePoint in routePoints) {
            val isCheckedIn = checkInRepository.hasCheckedIn(userId, tourId, routePoint.id)
            result.add(Pair(routePoint, isCheckedIn))
        }

        return result
    }


    suspend fun getNextRoutePointToVisit(userId: Long, tourId: Long): RoutePoint? {
        val routePointsWithStatus = getTourRoutePointsWithCheckInStatus(userId, tourId)
        return routePointsWithStatus.find { !it.second }?.first
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val location1 = Location("")
        location1.latitude = point1.latitude
        location1.longitude = point1.longitude

        val location2 = Location("")
        location2.latitude = point2.latitude
        location2.longitude = point2.longitude

        return location1.distanceTo(location2).toDouble()
    }
}