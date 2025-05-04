package com.xwurfel.tourry.data.route.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xwurfel.tourry.data.route.entity.RoutePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutePointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutePoint(routePointEntity: RoutePointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutePoints(routePointEntities: List<RoutePointEntity>)

    @Update
    suspend fun updateRoutePoint(routePointEntity: RoutePointEntity)

    @Query("SELECT * FROM route_points WHERE tourId = :tourId ORDER BY `order`")
    suspend fun getRoutePointsForTour(tourId: Long): List<RoutePointEntity>

    @Query("SELECT * FROM route_points WHERE tourId = :tourId ORDER BY `order`")
    fun observeRoutePointsForTour(tourId: Long): Flow<List<RoutePointEntity>>

    @Query("SELECT * FROM route_points WHERE id = :id")
    suspend fun getRoutePointById(id: Long): RoutePointEntity?

    @Query("DELETE FROM route_points WHERE id = :id")
    suspend fun deleteRoutePoint(id: Long)

    @Query("DELETE FROM route_points WHERE tourId = :tourId")
    suspend fun deleteAllRoutePointsForTour(tourId: Long)
}