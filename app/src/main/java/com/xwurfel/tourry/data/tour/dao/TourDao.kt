package com.xwurfel.tourry.data.tour.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xwurfel.tourry.data.tour.entity.TourEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TourDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTour(tourEntity: TourEntity): Long

    @Query("SELECT * FROM tours")
    fun getAllTours(): Flow<List<TourEntity>>

    @Query("SELECT * FROM tours WHERE id = :id")
    fun getTourById(id: Long): Flow<TourEntity?>

    @Query("SELECT * FROM tours WHERE organizerId = :organizerId")
    fun getToursByOrganizer(organizerId: Long): Flow<List<TourEntity>>

    @Query("SELECT * FROM tours WHERE categoryId = :categoryId")
    fun getToursByCategory(categoryId: Long): Flow<List<TourEntity>>

    @Query("SELECT * FROM tours WHERE startDateTime >= :startDate")
    fun getUpcomingTours(startDate: LocalDateTime = LocalDateTime.now()): Flow<List<TourEntity>>

    @Query("SELECT * FROM tours WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTours(query: String): Flow<List<TourEntity>>

    @Query("DELETE FROM tours WHERE id = :id")
    suspend fun deleteTour(id: Long)
}