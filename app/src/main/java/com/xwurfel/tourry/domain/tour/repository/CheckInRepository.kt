package com.xwurfel.tourry.domain.tour.repository

import com.xwurfel.tourry.domain.tour.model.CheckIn
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface CheckInRepository {
    suspend fun saveCheckIn(checkIn: CheckIn): Long

    fun getCheckInsByUser(userId: Long): Flow<List<CheckIn>>

    fun getCheckInsByTour(tourId: Long): Flow<List<CheckIn>>

    fun getCheckInsByUserAndTour(userId: Long, tourId: Long): Flow<List<CheckIn>>

    suspend fun getCheckInByUserTourAndRoutePoint(userId: Long, tourId: Long, routePointId: Long): CheckIn?

    suspend fun hasCheckedIn(userId: Long, tourId: Long, routePointId: Long): Boolean

    suspend fun getCheckInsCountByUserAndTour(userId: Long, tourId: Long): Int

    fun getCheckInsByTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<CheckIn>>

    suspend fun deleteCheckIn(checkInId: Long)
}