package com.xwurfel.tourry.domain.route.repository

import com.xwurfel.tourry.domain.route.model.RoutePoint
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    suspend fun getRoutePointsForTour(tourId: Long): List<RoutePoint>

    fun observeRoutePointsForTour(tourId: Long): Flow<List<RoutePoint>>

    suspend fun saveRoutePointsForTour(tourId: Long, routePoints: List<RoutePoint>)

    suspend fun saveRoutePoint(routePoint: RoutePoint): Long

    suspend fun updateRoutePoint(routePoint: RoutePoint)

    suspend fun deleteRoutePoint(routePointId: Long)

    suspend fun deleteAllRoutePointsForTour(tourId: Long)

    suspend fun getRoutePointById(routePointId: Long): RoutePoint?
}