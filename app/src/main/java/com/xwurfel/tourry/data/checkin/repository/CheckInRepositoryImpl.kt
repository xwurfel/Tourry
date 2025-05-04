package com.xwurfel.tourry.data.checkin.repository

import android.content.Context
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.xwurfel.tourry.data.checkin.dao.CheckInDao
import com.xwurfel.tourry.data.checkin.mapper.toDomain
import com.xwurfel.tourry.data.checkin.mapper.toEntity
import com.xwurfel.tourry.data.network.api.CheckInApi
import com.xwurfel.tourry.data.network.dto.CheckInCreateDto
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.domain.tour.model.CheckIn
import com.xwurfel.tourry.domain.tour.repository.CheckInRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import androidx.core.net.toUri

class CheckInRepositoryImpl @Inject constructor(
    private val checkInApi: CheckInApi,
    private val checkInDao: CheckInDao,
    private val fileUploadService: FileUploadService,
    private val syncDao: SyncDao,
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CheckInRepository {

    override suspend fun saveCheckIn(checkIn: CheckIn): Long = withContext(ioDispatcher) {
        var imageUrl: String? = null
        if (checkIn.imageUri != null) {
            try {
                val uri = checkIn.imageUri.toUri()
                val result = fileUploadService.uploadImage(uri)
                if (result.isSuccess) {
                    imageUrl = result.getOrThrow()
                }
            } catch (e: Exception) {
                // Handle image upload failure but continue with check-in
            }
        }

        if (NetworkUtils.isNetworkAvailable(context)) {
            // Create DTO
            val checkInCreateDto = CheckInCreateDto(
                tourId = checkIn.tourId,
                routePointId = checkIn.routePointId,
                latitude = 0.0, // Replace with actual location if available
                longitude = 0.0, // Replace with actual location if available
                note = checkIn.note,
                imageUrl = imageUrl,
                forceCheckIn = false // Set to true if you want to bypass location check
            )

            // Make API call
            when (val response = NetworkUtils.safeApiCall {
                checkInApi.createCheckIn(checkInCreateDto)
            }) {
                is ApiResponse.Success -> {
                    val createdCheckIn = response.data

                    // Save to local cache
                    val checkInEntity = checkIn.copy(
                        id = createdCheckIn.id,
                        imageUri = createdCheckIn.imageUrl
                    ).toEntity()

                    checkInDao.insertCheckIn(checkInEntity)
                    return@withContext createdCheckIn.id
                }

                is ApiResponse.Error -> {
                    // Fall back to local save
                    val localId = checkInDao.insertCheckIn(checkIn.toEntity())

                    // Mark for sync later
                    val syncEntity = com.xwurfel.tourry.data.sync.SyncEntity(
                        entityType = "check_in",
                        entityId = localId,
                        actionType = SyncActionType.CREATE,
                        actionData = gson.toJson(checkIn.copy(id = localId))
                    )
                    syncDao.insertSyncAction(syncEntity)

                    return@withContext localId
                }

                ApiResponse.Loading -> {
                    throw Exception("Request is still loading")
                }
            }
        } else {
            // Save locally for sync later
            val localId = checkInDao.insertCheckIn(checkIn.toEntity())

            // Mark for sync later
            val syncEntity = com.xwurfel.tourry.data.sync.SyncEntity(
                entityType = "check_in",
                entityId = localId,
                actionType = SyncActionType.CREATE,
                actionData = gson.toJson(checkIn.copy(id = localId))
            )
            syncDao.insertSyncAction(syncEntity)

            return@withContext localId
        }
    }

    override fun getCheckInsByUser(userId: Long): Flow<List<CheckIn>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                // API doesn't have a direct endpoint for this, so use the general endpoint
                // and filter client-side
                when (val response = NetworkUtils.safeApiCall {
                    checkInApi.getCheckInsForTour(0) // We'll filter by user later
                }) {
                    is ApiResponse.Success -> {
                        val checkIns = response.data
                            .filter { it.user.id == userId }
                            .map { dto ->
                                CheckIn(
                                    id = dto.id,
                                    userId = dto.user.id,
                                    tourId = dto.tour.id,
                                    routePointId = dto.routePoint.id,
                                    timestamp = dto.timestamp,
                                    note = dto.note,
                                    imageUri = dto.imageUrl
                                )
                            }

                        // Update local cache
                        checkInDao.deleteCheckInsByUser(userId)
                        checkInDao.insertCheckIns(checkIns.map { it.toEntity() })

                        emit(checkIns)
                    }

                    is ApiResponse.Error -> {
                        // Fall back to local cache
                        emit(checkInDao.getCheckInsByUser(userId).map { it.toDomain() })
                    }

                    ApiResponse.Loading -> {
                        // Should not happen
                    }
                }
            } catch (e: Exception) {
                // Fall back to local cache on any error
                emit(checkInDao.getCheckInsByUser(userId).map { it.toDomain() })
            }
        } else {
            // Use local cache when offline
            emit(checkInDao.getCheckInsByUser(userId).map { it.toDomain() })
        }
    }

    override fun getCheckInsByTour(tourId: Long): Flow<List<CheckIn>> {
        TODO("Not yet implemented")
    }

    override fun getCheckInsByUserAndTour(
        userId: Long,
        tourId: Long
    ): Flow<List<CheckIn>> {
        TODO("Not yet implemented")
    }

    override suspend fun getCheckInByUserTourAndRoutePoint(
        userId: Long,
        tourId: Long,
        routePointId: Long
    ): CheckIn? {
        TODO("Not yet implemented")
    }

    // Implement the remaining methods from the CheckInRepository interface
    // using the same pattern: try API first, fall back to local cache when needed

    // Continuing from the previous implementation in CheckInRepositoryImpl
    override suspend fun getRoutePointsWithCheckInStatus(
        userId: Long,
        tourId: Long
    ): List<Pair<RoutePoint, Boolean>> = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall {
                    checkInApi.getRoutePointsWithCheckInStatus(tourId)
                }) {
                    is ApiResponse.Success -> {
                        response.data.map { dto ->
                            val routePoint = RoutePoint(
                                id = dto.routePoint.id,
                                tourId = tourId,
                                location = LatLng(
                                    dto.routePoint.latitude,
                                    dto.routePoint.longitude
                                ),
                                title = dto.routePoint.title,
                                description = dto.routePoint.description,
                                order = dto.routePoint.orderIndex,
                                durationMinutes = dto.routePoint.durationMinutes,
                                arrivalInstructions = dto.routePoint.arrivalInstructions,
                                imageUri = dto.routePoint.imageUrl
                            )
                            Pair(routePoint, dto.isCheckedIn)
                        }
                    }

                    is ApiResponse.Error -> {
                        // Fall back to local implementation
                        // This would require joining data from routePoints and checkIns tables
                        val routePoints = routePointDao.getRoutePointsForTour(tourId)
                        val checkIns = checkInDao.getCheckInsByUserAndTour(userId, tourId)

                        routePoints.map { entity ->
                            Pair(
                                entity.toDomain(),
                                checkIns.any { it.routePointId == entity.id }
                            )
                        }
                    }

                    ApiResponse.Loading -> {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                // Fall back to local implementation on error
                val routePoints = routePointDao.getRoutePointsForTour(tourId)
                val checkIns = checkInDao.getCheckInsByUserAndTour(userId, tourId)

                routePoints.map { entity ->
                    Pair(
                        entity.toDomain(),
                        checkIns.any { it.routePointId == entity.id }
                    )
                }
            }
        } else {
            // Use local data when offline
            val routePoints = routePointDao.getRoutePointsForTour(tourId)
            val checkIns = checkInDao.getCheckInsByUserAndTour(userId, tourId)

            routePoints.map { entity ->
                Pair(
                    entity.toDomain(),
                    checkIns.any { it.routePointId == entity.id }
                )
            }
        }
    }

    override suspend fun hasCheckedIn(userId: Long, tourId: Long, routePointId: Long): Boolean =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    when (val response = NetworkUtils.safeApiCall {
                        checkInApi.getRoutePointsWithCheckInStatus(tourId)
                    }) {
                        is ApiResponse.Success -> {
                            response.data.find {
                                it.routePoint.id == routePointId
                            }?.isCheckedIn ?: false
                        }

                        is ApiResponse.Error -> {
                            // Fall back to local data
                            checkInDao.getCheckInByUserTourAndRoutePoint(
                                userId, tourId, routePointId
                            ) != null
                        }

                        ApiResponse.Loading -> false
                    }
                } catch (e: Exception) {
                    // Fall back to local data on error
                    checkInDao.getCheckInByUserTourAndRoutePoint(
                        userId, tourId, routePointId
                    ) != null
                }
            } else {
                // Use local data when offline
                checkInDao.getCheckInByUserTourAndRoutePoint(
                    userId, tourId, routePointId
                ) != null
            }
        }

    override suspend fun getCheckInsCountByUserAndTour(userId: Long, tourId: Long): Int =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    when (val response = NetworkUtils.safeApiCall {
                        checkInApi.getCheckInCountForTour(tourId)
                    }) {
                        is ApiResponse.Success -> {
                            response.data["count"] ?: 0
                        }

                        is ApiResponse.Error -> {
                            // Fall back to local data
                            checkInDao.getCheckInsCountByUserAndTour(userId, tourId)
                        }

                        ApiResponse.Loading -> 0
                    }
                } catch (e: Exception) {
                    // Fall back to local data on error
                    checkInDao.getCheckInsCountByUserAndTour(userId, tourId)
                }
            } else {
                // Use local data when offline
                checkInDao.getCheckInsCountByUserAndTour(userId, tourId)
            }
        }

    override fun getCheckInsByTimeRange(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<CheckIn>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCheckIn(checkInId: Long) {
        TODO("Not yet implemented")
    }
}
