package com.xwurfel.tourry.domain.tour.repository

import com.xwurfel.tourry.domain.tour.model.Tour
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface TourRepository {
    suspend fun saveTour(tour: Tour): Long

    fun getAllTours(): Flow<List<Tour>>

    fun getTourById(id: Long): Flow<Tour?>

    fun getToursByOrganizer(organizerId: Long): Flow<List<Tour>>

    fun getToursByCategory(categoryId: Long): Flow<List<Tour>>

    fun getUpcomingTours(startDate: LocalDateTime = LocalDateTime.now()): Flow<List<Tour>>

    fun searchTours(query: String): Flow<List<Tour>>

    suspend fun deleteTour(id: Long)
}