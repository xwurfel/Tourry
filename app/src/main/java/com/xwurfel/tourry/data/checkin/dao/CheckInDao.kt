package com.xwurfel.tourry.data.checkin.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xwurfel.tourry.data.checkin.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface CheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkInEntity: CheckInEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIns(checkInEntities: List<CheckInEntity>)

    @Query("SELECT * FROM check_ins WHERE userId = :userId ORDER BY timestamp DESC")
    fun getCheckInsByUser(userId: Long): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE tourId = :tourId ORDER BY timestamp DESC")
    fun getCheckInsByTour(tourId: Long): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE userId = :userId AND tourId = :tourId ORDER BY timestamp DESC")
    fun getCheckInsByUserAndTour(userId: Long, tourId: Long): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE userId = :userId AND tourId = :tourId AND routePointId = :routePointId")
    suspend fun getCheckInByUserTourAndRoutePoint(
        userId: Long,
        tourId: Long,
        routePointId: Long
    ): CheckInEntity?

    @Query("SELECT COUNT(*) FROM check_ins WHERE userId = :userId AND tourId = :tourId")
    suspend fun getCheckInsCountByUserAndTour(userId: Long, tourId: Long): Int

    @Query("SELECT * FROM check_ins WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getCheckInsByTimeRange(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<CheckInEntity>>

    @Query("DELETE FROM check_ins WHERE id = :id")
    suspend fun deleteCheckIn(id: Long)

    @Query("DELETE FROM check_ins WHERE userId = :userId")
    suspend fun deleteCheckInsByUser(userId: Long)

    @Query("DELETE FROM check_ins WHERE tourId = :tourId")
    suspend fun deleteCheckInsByTour(tourId: Long)

    @Query("DELETE FROM check_ins WHERE userId = :userId AND tourId = :tourId")
    suspend fun deleteCheckInsByUserAndTour(userId: Long, tourId: Long)
}