package com.xwurfel.tourry.data.tour.repository

import android.content.Context
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.xwurfel.tourry.data.network.api.TourApi
import com.xwurfel.tourry.data.network.dto.TourCreateDto
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.data.tour.dao.TourDao
import com.xwurfel.tourry.data.tour.mapper.toDomain
import com.xwurfel.tourry.data.tour.mapper.toEntity
import com.xwurfel.tourry.data.upload.FileUploadService
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.tour.model.Tour
import com.xwurfel.tourry.domain.tour.repository.TourRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TourRepositoryImpl @Inject constructor(
    private val tourApi: TourApi,
    private val tourDao: TourDao,
    private val syncDao: SyncDao,
    private val gson: Gson,
    private val fileUploadService: FileUploadService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val context: Context
) : TourRepository {

    override suspend fun saveTour(tour: Tour): Long = withContext(ioDispatcher) {
        // Handle image upload if needed
        var imageUrl: String? = null
        if (tour.imageUri != null) {
            try {
                val result = fileUploadService.uploadImage(tour.imageUri)
                if (result.isSuccess) {
                    imageUrl = result.getOrThrow()
                }
            } catch (e: Exception) {
                // Continue with tour creation even if image upload fails
            }
        }

        // Create DTO for API
        val tourCreateDto = TourCreateDto(
            title = tour.title,
            description = tour.description,
            imageUrl = imageUrl ?: tour.imageUri?.toString(),
            meetingPointLatitude = tour.meetingPoint.latitude,
            meetingPointLongitude = tour.meetingPoint.longitude,
            meetingPointAddress = tour.meetingPointAddress,
            startDateTime = tour.startDateTime,
            endDateTime = tour.endDateTime,
            price = tour.price,
            capacity = tour.capacity,
            categoryId = tour.categoryId,
            organizerId = tour.organizerId
        )

        if (NetworkUtils.isNetworkAvailable(context)) {
            // Try API call
            when (val response = NetworkUtils.safeApiCall {
                if (tour.id == 0L) {
                    tourApi.createTour(tourCreateDto)
                } else {
                    tourApi.updateTour(tour.id, tourCreateDto)
                }
            }) {
                is ApiResponse.Success -> {
                    val tourResponse = response.data

                    // Convert response to entity and save to local DB
                    val tourEntity = tour.copy(
                        id = tourResponse.id,
                        imageUri = tourResponse.imageUrl?.let { Uri.parse(it) },
                        createdAt = tourResponse.createdAt,
                        updatedAt = tourResponse.updatedAt
                    ).toEntity()

                    tourDao.insertTour(tourEntity)
                    return@withContext tourResponse.id
                }

                is ApiResponse.Error -> {
                    // Save locally and mark for sync later
                    val localId = tourDao.insertTour(tour.toEntity())
                    val syncEntity = SyncEntity(
                        entityType = "tour",
                        entityId = localId,
                        actionType = if (tour.id == 0L) SyncActionType.CREATE else SyncActionType.UPDATE,
                        actionData = gson.toJson(tour.copy(id = localId))
                    )
                    syncDao.insertSyncAction(syncEntity)

                    return@withContext localId
                }

                ApiResponse.Loading -> {
                    throw Exception("Request is still loading")
                }
            }
        } else {
            // Save locally and mark for sync when network is available
            val localId = tourDao.insertTour(tour.toEntity())
            val syncEntity = SyncEntity(
                entityType = "tour",
                entityId = localId,
                actionType = if (tour.id == 0L) SyncActionType.CREATE else SyncActionType.UPDATE,
                actionData = gson.toJson(tour.copy(id = localId))
            )
            syncDao.insertSyncAction(syncEntity)

            return@withContext localId
        }
    }

    override fun getAllTours(): Flow<List<Tour>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            // Try to fetch from API
            when (val response = NetworkUtils.safeApiCall { tourApi.getAllTours() }) {
                is ApiResponse.Success -> {
                    // Fetch full details for each tour
                    val tours = response.data.mapNotNull { tourMinDto ->
                        try {
                            val tourResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourResponse is ApiResponse.Success) {
                                val tourData = tourResponse.data
                                Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.let { Uri.parse(it) },
                                    meetingPoint = LatLng(
                                        tourData.meetingPointLatitude,
                                        tourData.meetingPointLongitude
                                    ),
                                    meetingPointAddress = tourData.meetingPointAddress,
                                    startDateTime = tourData.startDateTime,
                                    endDateTime = tourData.endDateTime,
                                    price = tourData.price,
                                    capacity = tourData.capacity,
                                    categoryId = tourData.category.id,
                                    organizerId = tourData.organizer.id,
                                    createdAt = tourData.createdAt,
                                    updatedAt = tourData.updatedAt
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Update local cache
                    val tourEntities = tours.map { it.toEntity() }
                    tourDao.deleteAllTours()
                    tourDao.insertTours(tourEntities)

                    emit(tours)
                }

                is ApiResponse.Error -> {
                    // Use local cache
                    val localTours = tourDao.getAllTours().first().map { it.toDomain() }
                    emit(localTours)
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            // Offline mode - use local cache
            val localTours = tourDao.getAllTours().first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override fun getTourById(id: Long): Flow<Tour?> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            // Try to fetch from API
            when (val response = NetworkUtils.safeApiCall { tourApi.getTourById(id) }) {
                is ApiResponse.Success -> {
                    val tourData = response.data
                    val tour = Tour(
                        id = tourData.id,
                        title = tourData.title,
                        description = tourData.description,
                        imageUri = tourData.imageUrl?.let { Uri.parse(it) },
                        meetingPoint = LatLng(
                            tourData.meetingPointLatitude,
                            tourData.meetingPointLongitude
                        ),
                        meetingPointAddress = tourData.meetingPointAddress,
                        startDateTime = tourData.startDateTime,
                        endDateTime = tourData.endDateTime,
                        price = tourData.price,
                        capacity = tourData.capacity,
                        categoryId = tourData.category.id,
                        organizerId = tourData.organizer.id,
                        createdAt = tourData.createdAt,
                        updatedAt = tourData.updatedAt
                    )

                    // Update local cache
                    tourDao.insertTour(tour.toEntity())

                    emit(tour)
                }

                is ApiResponse.Error -> {
                    // Use local cache
                    val localTour = tourDao.getTourById(id).first()?.toDomain()
                    emit(localTour)
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            // Offline mode - use local cache
            val localTour = tourDao.getTourById(id).first()?.toDomain()
            emit(localTour)
        }
    }

    override fun getToursByOrganizer(organizerId: Long): Flow<List<Tour>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            when (val response = NetworkUtils.safeApiCall {
                tourApi.getToursByOrganizer(organizerId)
            }) {
                is ApiResponse.Success -> {
                    // Fetch full details for each tour
                    val tours = response.data.mapNotNull { tourMinDto ->
                        try {
                            val tourResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourResponse is ApiResponse.Success) {
                                val tourData = tourResponse.data
                                Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.let { Uri.parse(it) },
                                    meetingPoint = LatLng(
                                        tourData.meetingPointLatitude,
                                        tourData.meetingPointLongitude
                                    ),
                                    meetingPointAddress = tourData.meetingPointAddress,
                                    startDateTime = tourData.startDateTime,
                                    endDateTime = tourData.endDateTime,
                                    price = tourData.price,
                                    capacity = tourData.capacity,
                                    categoryId = tourData.category.id,
                                    organizerId = tourData.organizer.id,
                                    createdAt = tourData.createdAt,
                                    updatedAt = tourData.updatedAt
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Cache results
                    tours.forEach { tourDao.insertTour(it.toEntity()) }

                    emit(tours)
                }

                is ApiResponse.Error -> {
                    // Use local cache
                    val localTours =
                        tourDao.getToursByOrganizer(organizerId).first().map { it.toDomain() }
                    emit(localTours)
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            // Offline mode - use local cache
            val localTours = tourDao.getToursByOrganizer(organizerId).first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override fun getToursByCategory(categoryId: Long): Flow<List<Tour>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            when (val response = NetworkUtils.safeApiCall {
                tourApi.getToursByCategory(categoryId)
            }) {
                is ApiResponse.Success -> {
                    // Fetch full details for each tour
                    val tours = response.data.mapNotNull { tourMinDto ->
                        try {
                            val tourResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourResponse is ApiResponse.Success) {
                                val tourData = tourResponse.data
                                Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.let { Uri.parse(it) },
                                    meetingPoint = LatLng(
                                        tourData.meetingPointLatitude,
                                        tourData.meetingPointLongitude
                                    ),
                                    meetingPointAddress = tourData.meetingPointAddress,
                                    startDateTime = tourData.startDateTime,
                                    endDateTime = tourData.endDateTime,
                                    price = tourData.price,
                                    capacity = tourData.capacity,
                                    categoryId = tourData.category.id,
                                    organizerId = tourData.organizer.id,
                                    createdAt = tourData.createdAt,
                                    updatedAt = tourData.updatedAt
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Cache results
                    tours.forEach { tourDao.insertTour(it.toEntity()) }

                    emit(tours)
                }

                is ApiResponse.Error -> {
                    // Use local cache
                    val localTours =
                        tourDao.getToursByCategory(categoryId).first().map { it.toDomain() }
                    emit(localTours)
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            // Offline mode - use local cache
            val localTours = tourDao.getToursByCategory(categoryId).first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override fun getUpcomingTours(startDate: LocalDateTime): Flow<List<Tour>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            when (val response = NetworkUtils.safeApiCall { tourApi.getUpcomingTours() }) {
                is ApiResponse.Success -> {
                    // Fetch full details for each tour
                    val tours = response.data.mapNotNull { tourMinDto ->
                        try {
                            val tourResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourResponse is ApiResponse.Success) {
                                val tourData = tourResponse.data
                                Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.let { Uri.parse(it) },
                                    meetingPoint = LatLng(
                                        tourData.meetingPointLatitude,
                                        tourData.meetingPointLongitude
                                    ),
                                    meetingPointAddress = tourData.meetingPointAddress,
                                    startDateTime = tourData.startDateTime,
                                    endDateTime = tourData.endDateTime,
                                    price = tourData.price,
                                    capacity = tourData.capacity,
                                    categoryId = tourData.category.id,
                                    organizerId = tourData.organizer.id,
                                    createdAt = tourData.createdAt,
                                    updatedAt = tourData.updatedAt
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Cache results
                    tours.forEach { tourDao.insertTour(it.toEntity()) }

                    emit(tours)
                }

                is ApiResponse.Error -> {
                    // Use local cache
                    val localTours =
                        tourDao.getUpcomingTours(startDate).first().map { it.toDomain() }
                    emit(localTours)
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            // Offline mode - use local cache
            val localTours = tourDao.getUpcomingTours(startDate).first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override fun searchTours(query: String): Flow<List<Tour>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            when (val response = NetworkUtils.safeApiCall {
                tourApi.searchTours(query)
            }) {
                is ApiResponse.Success -> {
                    // Fetch full details for each tour
                    val tours = response.data.mapNotNull { tourMinDto ->
                        try {
                            val tourResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourResponse is ApiResponse.Success) {
                                val tourData = tourResponse.data
                                Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.let { Uri.parse(it) },
                                    meetingPoint = LatLng(
                                        tourData.meetingPointLatitude,
                                        tourData.meetingPointLongitude
                                    ),
                                    meetingPointAddress = tourData.meetingPointAddress,
                                    startDateTime = tourData.startDateTime,
                                    endDateTime = tourData.endDateTime,
                                    price = tourData.price,
                                    capacity = tourData.capacity,
                                    categoryId = tourData.category.id,
                                    organizerId = tourData.organizer.id,
                                    createdAt = tourData.createdAt,
                                    updatedAt = tourData.updatedAt
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Cache results (don't delete existing tours)
                    tours.forEach { tourDao.insertTour(it.toEntity()) }

                    emit(tours)
                }

                is ApiResponse.Error -> {
                    // Perform local search
                    val localTours = tourDao.searchTours(query).first().map { it.toDomain() }
                    emit(localTours)
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            // Offline mode - local search
            val localTours = tourDao.searchTours(query).first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override suspend fun deleteTour(id: Long) = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            when (val response = NetworkUtils.safeApiCall { tourApi.deleteTour(id) }) {
                is ApiResponse.Success -> {
                    tourDao.deleteTour(id)
                }

                is ApiResponse.Error -> {
                    // Mark for deletion when network is available
                    val syncEntity = SyncEntity(
                        entityType = "tour",
                        entityId = id,
                        actionType = SyncActionType.DELETE
                    )
                    syncDao.insertSyncAction(syncEntity)

                    // Delete locally for immediate UI update
                    tourDao.deleteTour(id)
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            // Mark for deletion when network is available
            val syncEntity = SyncEntity(
                entityType = "tour",
                entityId = id,
                actionType = SyncActionType.DELETE
            )
            syncDao.insertSyncAction(syncEntity)

            // Delete locally for immediate UI update
            tourDao.deleteTour(id)
        }
    }
}