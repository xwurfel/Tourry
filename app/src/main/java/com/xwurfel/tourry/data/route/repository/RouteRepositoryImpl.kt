package com.xwurfel.tourry.data.route.repository

import com.xwurfel.tourry.data.route.dao.RoutePointDao
import com.xwurfel.tourry.data.route.mapper.toDomain
import com.xwurfel.tourry.data.route.mapper.toEntity
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.domain.route.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val routePointDao: RoutePointDao
) : RouteRepository {

    override suspend fun getRoutePointsForTour(tourId: Long): List<RoutePoint> {
        return routePointDao.getRoutePointsForTour(tourId).map { it.toDomain() }
    }

    override fun observeRoutePointsForTour(tourId: Long): Flow<List<RoutePoint>> {
        return routePointDao.observeRoutePointsForTour(tourId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveRoutePointsForTour(tourId: Long, routePoints: List<RoutePoint>) {
        routePointDao.deleteAllRoutePointsForTour(tourId)

        val routePointEntities = routePoints.mapIndexed { index, routePoint ->
            routePoint.copy(order = index).toEntity()
        }

        routePointDao.insertRoutePoints(routePointEntities)
    }

    override suspend fun saveRoutePoint(routePoint: RoutePoint): Long {
        return routePointDao.insertRoutePoint(routePoint.toEntity())
    }

    override suspend fun updateRoutePoint(routePoint: RoutePoint) {
        routePointDao.updateRoutePoint(routePoint.toEntity())
    }

    override suspend fun deleteRoutePoint(routePointId: Long) {
        routePointDao.deleteRoutePoint(routePointId)
    }

    override suspend fun deleteAllRoutePointsForTour(tourId: Long) {
        routePointDao.deleteAllRoutePointsForTour(tourId)
    }

    override suspend fun getRoutePointById(routePointId: Long): RoutePoint? {
        return routePointDao.getRoutePointById(routePointId)?.toDomain()
    }
}
