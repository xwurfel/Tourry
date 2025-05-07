package com.xwurfel.tourry.data.route.repository

import android.content.Context
import com.google.gson.Gson
import com.xwurfel.tourry.data.network.api.RouteApi
import com.xwurfel.tourry.data.network.dto.RoutePointCreateDto
import com.xwurfel.tourry.data.network.dto.TourRouteUpdateDto
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.route.dao.RoutePointDao
import com.xwurfel.tourry.data.route.mapper.toDomain
import com.xwurfel.tourry.data.route.mapper.toEntity
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.domain.route.repository.RouteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepositoryImpl @Inject constructor(
    private val routePointDao: RoutePointDao,
    private val routeApi: RouteApi,
    private val syncDao: SyncDao,
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : RouteRepository {

    override suspend fun getRoutePointsForTour(tourId: Long): List<RoutePoint> =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    when (val response =
                        NetworkUtils.safeApiCall { routeApi.getRoutePointsForTour(tourId) }) {
                        is ApiResponse.Success -> {
                            val routePoints = response.data.map { dto ->
                                RoutePoint(
                                    id = dto.id,
                                    tourId = tourId,
                                    location = com.google.android.gms.maps.model.LatLng(
                                        dto.latitude, dto.longitude
                                    ),
                                    title = dto.title,
                                    description = dto.description,
                                    order = dto.orderIndex,
                                    durationMinutes = dto.durationMinutes,
                                    arrivalInstructions = dto.arrivalInstructions,
                                    imageUri = dto.imageUrl
                                )
                            }

                            // Update local cache
                            routePointDao.deleteAllRoutePointsForTour(tourId)
                            routePointDao.insertRoutePoints(routePoints.map { it.toEntity() })

                            routePoints
                        }

                        is ApiResponse.Error -> {
                            routePointDao.getRoutePointsForTour(tourId).map { it.toDomain() }
                        }

                        ApiResponse.Loading -> {
                            routePointDao.getRoutePointsForTour(tourId).map { it.toDomain() }
                        }
                    }
                } catch (_: Exception) {
                    routePointDao.getRoutePointsForTour(tourId).map { it.toDomain() }
                }
            } else {
                routePointDao.getRoutePointsForTour(tourId).map { it.toDomain() }
            }
        }

    override fun observeRoutePointsForTour(tourId: Long): Flow<List<RoutePoint>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response =
                    NetworkUtils.safeApiCall { routeApi.getRoutePointsForTour(tourId) }) {
                    is ApiResponse.Success -> {
                        val routePoints = response.data.map { dto ->
                            RoutePoint(
                                id = dto.id,
                                tourId = tourId,
                                location = com.google.android.gms.maps.model.LatLng(
                                    dto.latitude, dto.longitude
                                ),
                                title = dto.title,
                                description = dto.description,
                                order = dto.orderIndex,
                                durationMinutes = dto.durationMinutes,
                                arrivalInstructions = dto.arrivalInstructions,
                                imageUri = dto.imageUrl
                            )
                        }

                        // Update local cache
                        routePointDao.deleteAllRoutePointsForTour(tourId)
                        routePointDao.insertRoutePoints(routePoints.map { it.toEntity() })

                        emit(routePoints)
                    }

                    is ApiResponse.Error -> {
                        emit(
                            routePointDao.observeRoutePointsForTour(tourId).first()
                                .map { it.toDomain() })
                    }

                    ApiResponse.Loading -> {
                        emit(
                            routePointDao.observeRoutePointsForTour(tourId).first()
                                .map { it.toDomain() })
                    }
                }
            } catch (_: Exception) {
                emit(routePointDao.observeRoutePointsForTour(tourId).first().map { it.toDomain() })
            }
        } else {
            emit(routePointDao.observeRoutePointsForTour(tourId).first().map { it.toDomain() })
        }
    }

    override suspend fun saveRoutePointsForTour(tourId: Long, routePoints: List<RoutePoint>): Unit =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val routePointDtos = routePoints.map { routePoint ->
                    RoutePointCreateDto(
                        tourId = tourId,
                        latitude = routePoint.location.latitude,
                        longitude = routePoint.location.longitude,
                        title = routePoint.title,
                        description = routePoint.description,
                        orderIndex = routePoint.order,
                        durationMinutes = routePoint.durationMinutes,
                        arrivalInstructions = routePoint.arrivalInstructions,
                        imageUrl = routePoint.imageUri
                    )
                }

                val tourRouteUpdateDto = TourRouteUpdateDto(
                    tourId = tourId, routePoints = routePointDtos
                )

                when (val response = NetworkUtils.safeApiCall {
                    routeApi.updateTourRoutePoints(tourRouteUpdateDto)
                }) {
                    is ApiResponse.Success -> {
                        // Update local cache with the returned data
                        val updatedRoutePoints = response.data.map { dto ->
                            RoutePoint(
                                id = dto.id,
                                tourId = tourId,
                                location = com.google.android.gms.maps.model.LatLng(
                                    dto.latitude, dto.longitude
                                ),
                                title = dto.title,
                                description = dto.description,
                                order = dto.orderIndex,
                                durationMinutes = dto.durationMinutes,
                                arrivalInstructions = dto.arrivalInstructions,
                                imageUri = dto.imageUrl
                            )
                        }

                        routePointDao.deleteAllRoutePointsForTour(tourId)
                        routePointDao.insertRoutePoints(updatedRoutePoints.map { it.toEntity() })
                    }

                    is ApiResponse.Error -> {
                        // Save locally and mark for sync later
                        routePointDao.deleteAllRoutePointsForTour(tourId)

                        val entities = routePoints.mapIndexed { index, routePoint ->
                            routePoint.copy(order = index).toEntity()
                        }
                        routePointDao.insertRoutePoints(entities)

                        val syncEntity = SyncEntity(
                            entityType = "tour_route",
                            entityId = tourId,
                            actionType = SyncActionType.UPDATE,
                            actionData = gson.toJson(routePoints)
                        )
                        syncDao.insertSyncAction(syncEntity)
                    }

                    ApiResponse.Loading -> {
                        // Should not happen with safeApiCall
                    }
                }
            } else {
                // Save locally and mark for sync later
                routePointDao.deleteAllRoutePointsForTour(tourId)

                val entities = routePoints.mapIndexed { index, routePoint ->
                    routePoint.copy(order = index).toEntity()
                }
                routePointDao.insertRoutePoints(entities)

                val syncEntity = SyncEntity(
                    entityType = "tour_route",
                    entityId = tourId,
                    actionType = SyncActionType.UPDATE,
                    actionData = gson.toJson(routePoints)
                )
                syncDao.insertSyncAction(syncEntity)
            }
        }

    override suspend fun saveRoutePoint(routePoint: RoutePoint): Long = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val routePointDto = RoutePointCreateDto(
                tourId = routePoint.tourId,
                latitude = routePoint.location.latitude,
                longitude = routePoint.location.longitude,
                title = routePoint.title,
                description = routePoint.description,
                orderIndex = routePoint.order,
                durationMinutes = routePoint.durationMinutes,
                arrivalInstructions = routePoint.arrivalInstructions,
                imageUrl = routePoint.imageUri
            )

            when (val response = NetworkUtils.safeApiCall {
                routeApi.createRoutePoint(routePointDto)
            }) {
                is ApiResponse.Success -> {
                    val createdRoutePoint = response.data
                    val routePointEntity = RoutePoint(
                        id = createdRoutePoint.id,
                        tourId = routePoint.tourId,
                        location = com.google.android.gms.maps.model.LatLng(
                            createdRoutePoint.latitude, createdRoutePoint.longitude
                        ),
                        title = createdRoutePoint.title,
                        description = createdRoutePoint.description,
                        order = createdRoutePoint.orderIndex,
                        durationMinutes = createdRoutePoint.durationMinutes,
                        arrivalInstructions = createdRoutePoint.arrivalInstructions,
                        imageUri = createdRoutePoint.imageUrl
                    ).toEntity()

                    routePointDao.insertRoutePoint(routePointEntity)
                    createdRoutePoint.id
                }

                is ApiResponse.Error -> {
                    val localId = routePointDao.insertRoutePoint(routePoint.toEntity())

                    val syncEntity = SyncEntity(
                        entityType = "route_point",
                        entityId = localId,
                        actionType = SyncActionType.CREATE,
                        actionData = gson.toJson(routePoint.copy(id = localId))
                    )
                    syncDao.insertSyncAction(syncEntity)

                    localId
                }

                ApiResponse.Loading -> {
                    throw Exception("Request is still loading")
                }
            }
        } else {
            val localId = routePointDao.insertRoutePoint(routePoint.toEntity())

            val syncEntity = SyncEntity(
                entityType = "route_point",
                entityId = localId,
                actionType = SyncActionType.CREATE,
                actionData = gson.toJson(routePoint.copy(id = localId))
            )
            syncDao.insertSyncAction(syncEntity)

            localId
        }
    }

    override suspend fun updateRoutePoint(routePoint: RoutePoint): Unit =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val routePointDto = RoutePointCreateDto(
                    tourId = routePoint.tourId,
                    latitude = routePoint.location.latitude,
                    longitude = routePoint.location.longitude,
                    title = routePoint.title,
                    description = routePoint.description,
                    orderIndex = routePoint.order,
                    durationMinutes = routePoint.durationMinutes,
                    arrivalInstructions = routePoint.arrivalInstructions,
                    imageUrl = routePoint.imageUri
                )

                when (val response = NetworkUtils.safeApiCall {
                    routeApi.updateRoutePoint(routePoint.id, routePointDto)
                }) {
                    is ApiResponse.Success -> {
                        val updatedRoutePoint = response.data
                        val routePointEntity = RoutePoint(
                            id = updatedRoutePoint.id,
                            tourId = routePoint.tourId,
                            location = com.google.android.gms.maps.model.LatLng(
                                updatedRoutePoint.latitude, updatedRoutePoint.longitude
                            ),
                            title = updatedRoutePoint.title,
                            description = updatedRoutePoint.description,
                            order = updatedRoutePoint.orderIndex,
                            durationMinutes = updatedRoutePoint.durationMinutes,
                            arrivalInstructions = updatedRoutePoint.arrivalInstructions,
                            imageUri = updatedRoutePoint.imageUrl
                        ).toEntity()

                        routePointDao.updateRoutePoint(routePointEntity)
                    }

                    is ApiResponse.Error -> {
                        routePointDao.updateRoutePoint(routePoint.toEntity())

                        val syncEntity = SyncEntity(
                            entityType = "route_point",
                            entityId = routePoint.id,
                            actionType = SyncActionType.UPDATE,
                            actionData = gson.toJson(routePoint)
                        )
                        syncDao.insertSyncAction(syncEntity)
                    }

                    ApiResponse.Loading -> {
                        // Should not happen with safeApiCall
                    }
                }
            } else {
                routePointDao.updateRoutePoint(routePoint.toEntity())

                val syncEntity = SyncEntity(
                    entityType = "route_point",
                    entityId = routePoint.id,
                    actionType = SyncActionType.UPDATE,
                    actionData = gson.toJson(routePoint)
                )
                syncDao.insertSyncAction(syncEntity)
            }
        }

    override suspend fun deleteRoutePoint(routePointId: Long) = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            when (NetworkUtils.safeApiCall {
                routeApi.deleteRoutePoint(routePointId)
            }) {
                is ApiResponse.Success -> {
                    routePointDao.deleteRoutePoint(routePointId)
                }

                is ApiResponse.Error -> {
                    val syncEntity = SyncEntity(
                        entityType = "route_point",
                        entityId = routePointId,
                        actionType = SyncActionType.DELETE
                    )
                    syncDao.insertSyncAction(syncEntity)

                    routePointDao.deleteRoutePoint(routePointId)
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            val syncEntity = SyncEntity(
                entityType = "route_point",
                entityId = routePointId,
                actionType = SyncActionType.DELETE
            )
            syncDao.insertSyncAction(syncEntity)

            routePointDao.deleteRoutePoint(routePointId)
        }
    }

    override suspend fun deleteAllRoutePointsForTour(tourId: Long) = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val routePoints = routePointDao.getRoutePointsForTour(tourId)

            for (routePoint in routePoints) {
                when (NetworkUtils.safeApiCall {
                    routeApi.deleteRoutePoint(routePoint.id)
                }) {
                    is ApiResponse.Success -> {
                        // Individual delete successful
                    }

                    is ApiResponse.Error -> {
                        val syncEntity = SyncEntity(
                            entityType = "route_point",
                            entityId = routePoint.id,
                            actionType = SyncActionType.DELETE
                        )
                        syncDao.insertSyncAction(syncEntity)
                    }

                    ApiResponse.Loading -> {
                        // Should not happen with safeApiCall
                    }
                }
            }

            routePointDao.deleteAllRoutePointsForTour(tourId)
        } else {
            val routePoints = routePointDao.getRoutePointsForTour(tourId)

            for (routePoint in routePoints) {
                val syncEntity = SyncEntity(
                    entityType = "route_point",
                    entityId = routePoint.id,
                    actionType = SyncActionType.DELETE
                )
                syncDao.insertSyncAction(syncEntity)
            }

            routePointDao.deleteAllRoutePointsForTour(tourId)
        }
    }

    override suspend fun getRoutePointById(routePointId: Long): RoutePoint? =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    when (val response =
                        NetworkUtils.safeApiCall { routeApi.getRoutePointById(routePointId) }) {
                        is ApiResponse.Success -> {
                            val dto = response.data
                            val routePoint = RoutePoint(
                                id = dto.id,
                                tourId = dto.tourId,
                                location = com.google.android.gms.maps.model.LatLng(
                                    dto.latitude, dto.longitude
                                ),
                                title = dto.title,
                                description = dto.description,
                                order = dto.orderIndex,
                                durationMinutes = dto.durationMinutes,
                                arrivalInstructions = dto.arrivalInstructions,
                                imageUri = dto.imageUrl
                            )

                            routePointDao.insertRoutePoint(routePoint.toEntity())

                            routePoint
                        }

                        is ApiResponse.Error -> {
                            routePointDao.getRoutePointById(routePointId)?.toDomain()
                        }

                        ApiResponse.Loading -> {
                            routePointDao.getRoutePointById(routePointId)?.toDomain()
                        }
                    }
                } catch (_: Exception) {
                    routePointDao.getRoutePointById(routePointId)?.toDomain()
                }
            } else {
                routePointDao.getRoutePointById(routePointId)?.toDomain()
            }
        }
}