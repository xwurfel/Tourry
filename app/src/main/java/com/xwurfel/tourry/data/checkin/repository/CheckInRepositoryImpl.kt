package com.xwurfel.tourry.data.checkin.repository

import com.xwurfel.tourry.data.checkin.dao.CheckInDao
import com.xwurfel.tourry.data.checkin.mapper.toDomain
import com.xwurfel.tourry.data.checkin.mapper.toEntity
import com.xwurfel.tourry.domain.tour.model.CheckIn
import com.xwurfel.tourry.domain.tour.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class CheckInRepositoryImpl @Inject constructor(
    private val checkInDao: CheckInDao
) : CheckInRepository {

    override suspend fun saveCheckIn(checkIn: CheckIn): Long {
        return checkInDao.insertCheckIn(checkIn.toEntity())
    }

    override fun getCheckInsByUser(userId: Long): Flow<List<CheckIn>> {
        return checkInDao.getCheckInsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCheckInsByTour(tourId: Long): Flow<List<CheckIn>> {
        return checkInDao.getCheckInsByTour(tourId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCheckInsByUserAndTour(userId: Long, tourId: Long): Flow<List<CheckIn>> {
        return checkInDao.getCheckInsByUserAndTour(userId, tourId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCheckInByUserTourAndRoutePoint(userId: Long, tourId: Long, routePointId: Long): CheckIn? {
        return checkInDao.getCheckInByUserTourAndRoutePoint(userId, tourId, routePointId)?.toDomain()
    }

    override suspend fun hasCheckedIn(userId: Long, tourId: Long, routePointId: Long): Boolean {
        return checkInDao.getCheckInByUserTourAndRoutePoint(userId, tourId, routePointId) != null
    }

    override suspend fun getCheckInsCountByUserAndTour(userId: Long, tourId: Long): Int {
        return checkInDao.getCheckInsCountByUserAndTour(userId, tourId)
    }

    override fun getCheckInsByTimeRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<CheckIn>> {
        return checkInDao.getCheckInsByTimeRange(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteCheckIn(checkInId: Long) {
        checkInDao.deleteCheckIn(checkInId)
    }
}