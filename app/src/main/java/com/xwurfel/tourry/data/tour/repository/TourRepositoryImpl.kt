package com.xwurfel.tourry.data.tour.repository

import com.xwurfel.tourry.data.tour.dao.TourDao
import com.xwurfel.tourry.data.tour.mapper.toDomain
import com.xwurfel.tourry.data.tour.mapper.toEntity
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.tour.repository.TourRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class TourRepositoryImpl @Inject constructor(
    private val tourDao: TourDao
) : TourRepository {

    override suspend fun saveTour(tour: Tour): Long {
        return tourDao.insertTour(tour.toEntity())
    }

    override fun getAllTours(): Flow<List<Tour>> {
        return tourDao.getAllTours().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTourById(id: Long): Flow<Tour?> {
        return tourDao.getTourById(id).map { it?.toDomain() }
    }

    override fun getToursByOrganizer(organizerId: Long): Flow<List<Tour>> {
        return tourDao.getToursByOrganizer(organizerId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getToursByCategory(categoryId: Long): Flow<List<Tour>> {
        return tourDao.getToursByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUpcomingTours(startDate: LocalDateTime): Flow<List<Tour>> {
        return tourDao.getUpcomingTours(startDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchTours(query: String): Flow<List<Tour>> {
        return tourDao.searchTours(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteTour(id: Long) {
        tourDao.deleteTour(id)
    }
}