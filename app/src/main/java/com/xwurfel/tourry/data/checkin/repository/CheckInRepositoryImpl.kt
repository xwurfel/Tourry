package com.xwurfel.tourry.data.checkin.repository

import android.content.Context
import androidx.core.net.toUri
import com.google.gson.Gson
import com.xwurfel.tourry.data.checkin.dao.CheckInDao
import com.xwurfel.tourry.data.checkin.mapper.toDomain
import com.xwurfel.tourry.data.checkin.mapper.toEntity
import com.xwurfel.tourry.data.network.api.CheckInApi
import com.xwurfel.tourry.data.network.dto.CheckInCreateDto
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.route.dao.RoutePointDao
import com.xwurfel.tourry.data.route.mapper.toDomain
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.data.upload.FileUploadService
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.domain.tour.model.CheckIn
import com.xwurfel.tourry.domain.tour.repository.CheckInRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepositoryImpl @Inject constructor(
    private val checkInApi: CheckInApi,
    private val checkInDao: CheckInDao,
    private val routePointDao: RoutePointDao,
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
            } catch (_: Exception) {
            }
        }

        if (NetworkUtils.isNetworkAvailable(context)) {
            val checkInCreateDto = CheckInCreateDto(
                tourId = checkIn.tourId,
                routePointId = checkIn.routePointId,
                latitude = 0.0, // Replace with actual location if available
                longitude = 0.0, // Replace with actual location if available
                note = checkIn.note,
                imageUrl = imageUrl ?: checkIn.imageUri,
                forceCheckIn = false // Set to true to bypass location check if needed
            )

            try {
                when (val response = NetworkUtils.safeApiCall {
                    checkInApi.createCheckIn(checkInCreateDto)
                }) {
                    is ApiResponse.Success -> {
                        val createdCheckIn = response.data
                        val checkInEntity = CheckIn(
                            id = createdCheckIn.id,
                            userId = createdCheckIn.user.id,
                            tourId = createdCheckIn.tour.id,
                            routePointId = createdCheckIn.routePoint.id,
                            timestamp = createdCheckIn.timestamp,
                            note = createdCheckIn.note,
                            imageUri = createdCheckIn.imageUrl
                        ).toEntity()

                        checkInDao.insertCheckIn(checkInEntity)
                        return@withContext createdCheckIn.id
                    }

                    is ApiResponse.Error -> {
                        val localId = checkInDao.insertCheckIn(checkIn.toEntity())

                        val syncEntity = SyncEntity(
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
            } catch (_: Exception) {
                val localId = checkInDao.insertCheckIn(checkIn.toEntity())

                val syncEntity = SyncEntity(
                    entityType = "check_in",
                    entityId = localId,
                    actionType = SyncActionType.CREATE,
                    actionData = gson.toJson(checkIn.copy(id = localId))
                )
                syncDao.insertSyncAction(syncEntity)

                return@withContext localId
            }
        } else {
            val localId = checkInDao.insertCheckIn(checkIn.toEntity())

            val syncEntity = SyncEntity(
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
                when (val response = NetworkUtils.safeApiCall {
                    checkInApi.getCheckInsForTour(0)
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
                        val localCheckIns =
                            checkInDao.getCheckInsByUser(userId).first().map { it.toDomain() }
                        emit(localCheckIns)
                    }

                    ApiResponse.Loading -> {
                        val localCheckIns =
                            checkInDao.getCheckInsByUser(userId).first().map { it.toDomain() }
                        emit(localCheckIns)
                    }
                }
            } catch (_: Exception) {
                val localCheckIns =
                    checkInDao.getCheckInsByUser(userId).first().map { it.toDomain() }
                emit(localCheckIns)
            }
        } else {
            val localCheckIns = checkInDao.getCheckInsByUser(userId).first().map { it.toDomain() }
            emit(localCheckIns)
        }
    }

    override fun getCheckInsByTour(tourId: Long): Flow<List<CheckIn>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall {
                    checkInApi.getCheckInsForTour(tourId)
                }) {
                    is ApiResponse.Success -> {
                        val checkIns = response.data.map { dto ->
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
                        checkInDao.deleteCheckInsByTour(tourId)
                        checkInDao.insertCheckIns(checkIns.map { it.toEntity() })

                        emit(checkIns)
                    }

                    is ApiResponse.Error -> {
                        val localCheckIns =
                            checkInDao.getCheckInsByTour(tourId).first().map { it.toDomain() }
                        emit(localCheckIns)
                    }

                    ApiResponse.Loading -> {
                        val localCheckIns =
                            checkInDao.getCheckInsByTour(tourId).first().map { it.toDomain() }
                        emit(localCheckIns)
                    }
                }
            } catch (_: Exception) {
                val localCheckIns =
                    checkInDao.getCheckInsByTour(tourId).first().map { it.toDomain() }
                emit(localCheckIns)
            }
        } else {
            val localCheckIns = checkInDao.getCheckInsByTour(tourId).first().map { it.toDomain() }
            emit(localCheckIns)
        }
    }

    override fun getCheckInsByUserAndTour(userId: Long, tourId: Long): Flow<List<CheckIn>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall {
                    checkInApi.getCheckInsForTour(tourId)
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
                        checkInDao.deleteCheckInsByUserAndTour(userId, tourId)
                        checkInDao.insertCheckIns(checkIns.map { it.toEntity() })

                        emit(checkIns)
                    }

                    is ApiResponse.Error -> {
                        val localCheckIns =
                            checkInDao.getCheckInsByUserAndTour(userId, tourId).first()
                                .map { it.toDomain() }
                        emit(localCheckIns)
                    }

                    ApiResponse.Loading -> {
                        val localCheckIns =
                            checkInDao.getCheckInsByUserAndTour(userId, tourId).first()
                                .map { it.toDomain() }
                        emit(localCheckIns)
                    }
                }
            } catch (_: Exception) {
                val localCheckIns = checkInDao.getCheckInsByUserAndTour(userId, tourId).first()
                    .map { it.toDomain() }
                emit(localCheckIns)
            }
        } else {
            val localCheckIns =
                checkInDao.getCheckInsByUserAndTour(userId, tourId).first().map { it.toDomain() }
            emit(localCheckIns)
        }
    }

    override suspend fun getCheckInByUserTourAndRoutePoint(
        userId: Long,
        tourId: Long,
        routePointId: Long
    ): CheckIn? = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall {
                    checkInApi.getRoutePointsWithCheckInStatus(tourId)
                }) {
                    is ApiResponse.Success -> {
                        val isCheckedIn = response.data.find {
                            it.routePoint.id == routePointId
                        }?.isCheckedIn == true

                        if (isCheckedIn) {
                            // If it's checked in, we need to get the check-in details from all check-ins
                            val checkInsResponse = NetworkUtils.safeApiCall {
                                checkInApi.getCheckInsForTour(tourId)
                            }

                            if (checkInsResponse is ApiResponse.Success) {
                                val checkIn = checkInsResponse.data
                                    .filter {
                                        it.user.id == userId &&
                                                it.routePoint.id == routePointId
                                    }
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
                                    .firstOrNull()

                                // Cache the check-in if found
                                checkIn?.let {
                                    checkInDao.insertCheckIn(it.toEntity())
                                }

                                checkIn
                            } else {
                                checkInDao.getCheckInByUserTourAndRoutePoint(
                                    userId,
                                    tourId,
                                    routePointId
                                )?.toDomain()
                            }
                        } else {
                            null
                        }
                    }

                    is ApiResponse.Error -> {
                        checkInDao.getCheckInByUserTourAndRoutePoint(userId, tourId, routePointId)
                            ?.toDomain()
                    }

                    ApiResponse.Loading -> {
                        checkInDao.getCheckInByUserTourAndRoutePoint(userId, tourId, routePointId)
                            ?.toDomain()
                    }
                }
            } catch (_: Exception) {
                checkInDao.getCheckInByUserTourAndRoutePoint(userId, tourId, routePointId)
                    ?.toDomain()
            }
        } else {
            checkInDao.getCheckInByUserTourAndRoutePoint(userId, tourId, routePointId)?.toDomain()
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
                            }?.isCheckedIn == true
                        }

                        is ApiResponse.Error -> {
                            checkInDao.getCheckInByUserTourAndRoutePoint(
                                userId,
                                tourId,
                                routePointId
                            ) != null
                        }

                        ApiResponse.Loading -> {
                            checkInDao.getCheckInByUserTourAndRoutePoint(
                                userId,
                                tourId,
                                routePointId
                            ) != null
                        }
                    }
                } catch (_: Exception) {
                    checkInDao.getCheckInByUserTourAndRoutePoint(
                        userId,
                        tourId,
                        routePointId
                    ) != null
                }
            } else {
                checkInDao.getCheckInByUserTourAndRoutePoint(userId, tourId, routePointId) != null
            }
        }

    override suspend fun getCheckInsCountByUserAndTour(userId: Long, tourId: Long): Int =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    when (val response = NetworkUtils.safeApiCall {
                        checkInApi.getTourProgress(tourId)
                    }) {
                        is ApiResponse.Success -> {
                            response.data.checkedInPoints
                        }

                        is ApiResponse.Error -> {
                            checkInDao.getCheckInsCountByUserAndTour(userId, tourId)
                        }

                        ApiResponse.Loading -> {
                            checkInDao.getCheckInsCountByUserAndTour(userId, tourId)
                        }
                    }
                } catch (_: Exception) {
                    checkInDao.getCheckInsCountByUserAndTour(userId, tourId)
                }
            } else {
                checkInDao.getCheckInsCountByUserAndTour(userId, tourId)
            }
        }

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
                                location = dto.routePoint.location,
                                title = dto.routePoint.title,
                                description = dto.routePoint.description,
                                order = dto.routePoint.order,
                                durationMinutes = dto.routePoint.durationMinutes,
                                arrivalInstructions = dto.routePoint.arrivalInstructions,
                                imageUri = dto.routePoint.imageUri
                            )
                            Pair(routePoint, dto.isCheckedIn)
                        }
                    }

                    is ApiResponse.Error -> {
                        // Fall back to local implementation
                        val routePoints = routePointDao.getRoutePointsForTour(tourId)
                        val checkIns = checkInDao.getCheckInsByUserAndTour(userId, tourId).first()

                        routePoints.map { entity ->
                            val routePoint = entity.toDomain()
                            val isCheckedIn = checkIns.any { it.routePointId == entity.id }
                            Pair(routePoint, isCheckedIn)
                        }
                    }

                    ApiResponse.Loading -> {
                        emptyList()
                    }
                }
            } catch (_: Exception) {
                val routePoints = routePointDao.getRoutePointsForTour(tourId)
                val checkIns = checkInDao.getCheckInsByUserAndTour(userId, tourId).first()

                routePoints.map { entity ->
                    val routePoint = entity.toDomain()
                    val isCheckedIn = checkIns.any { it.routePointId == entity.id }
                    Pair(routePoint, isCheckedIn)
                }
            }
        } else {
            val routePoints = routePointDao.getRoutePointsForTour(tourId)
            val checkIns = checkInDao.getCheckInsByUserAndTour(userId, tourId).first()

            routePoints.map { entity ->
                val routePoint = entity.toDomain()
                val isCheckedIn = checkIns.any { it.routePointId == entity.id }
                Pair(routePoint, isCheckedIn)
            }
        }
    }

    override fun getCheckInsByTimeRange(
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Flow<List<CheckIn>> = flow {
        val localCheckIns =
            checkInDao.getCheckInsByTimeRange(startTime, endTime).first().map { it.toDomain() }
        emit(localCheckIns)
    }

    override suspend fun deleteCheckIn(checkInId: Long) = withContext(ioDispatcher) {
        // TODO: add check-in deletion to backend
        // API may not support deleting check-ins, so implement with caution
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                // If the API supports deleting check-ins, use this pattern:
                /*
                when (val response = NetworkUtils.safeApiCall {
                    checkInApi.deleteCheckIn(checkInId)
                }) {
                    is ApiResponse.Success -> {
                        checkInDao.deleteCheckIn(checkInId)
                    }
                    is ApiResponse.Error -> {
                        val syncEntity = SyncEntity(
                            entityType = "check_in",
                            entityId = checkInId,
                            actionType = SyncActionType.DELETE
                        )
                        syncDao.insertSyncAction(syncEntity)

                        // For UI updates, delete locally anyway
                        checkInDao.deleteCheckIn(checkInId)
                    }
                    ApiResponse.Loading -> {
                        // Should not happen with safeApiCall
                    }
                }
                */

                // For now, we'll just mark it for deletion and delete locally
                val syncEntity = SyncEntity(
                    entityType = "check_in",
                    entityId = checkInId,
                    actionType = SyncActionType.DELETE
                )
                syncDao.insertSyncAction(syncEntity)
                checkInDao.deleteCheckIn(checkInId)
            } catch (_: Exception) {
                val syncEntity = SyncEntity(
                    entityType = "check_in",
                    entityId = checkInId,
                    actionType = SyncActionType.DELETE
                )
                syncDao.insertSyncAction(syncEntity)
                checkInDao.deleteCheckIn(checkInId)
            }
        } else {
            val syncEntity = SyncEntity(
                entityType = "check_in",
                entityId = checkInId,
                actionType = SyncActionType.DELETE
            )
            syncDao.insertSyncAction(syncEntity)
            checkInDao.deleteCheckIn(checkInId)
        }
    }
}