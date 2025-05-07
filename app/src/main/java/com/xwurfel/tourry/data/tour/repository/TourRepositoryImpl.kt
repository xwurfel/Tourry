package com.xwurfel.tourry.data.tour.repository


import android.content.Context
import androidx.core.net.toUri
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
    private val fileUploadService: FileUploadService,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val context: Context
) : TourRepository {

    override suspend fun saveTour(tour: Tour): Long = withContext(ioDispatcher) {
        var imageUrl: String? = null

        if (tour.imageUri != null) {
            try {
                val result = fileUploadService.uploadImage(tour.imageUri)
                if (result.isSuccess) {
                    imageUrl = result.getOrThrow()
                }
            } catch (_: Exception) {
                // Continue with tour creation even if image upload fails
            }
        }

        val tourCreateDto = TourCreateDto(
            title = tour.title,
            description = tour.description,
            imageUrl = imageUrl,
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
            try {
                val response = if (tour.id == 0L) {
                    NetworkUtils.safeApiCall { tourApi.createTour(tourCreateDto) }
                } else {
                    NetworkUtils.safeApiCall { tourApi.updateTour(tour.id, tourCreateDto) }
                }

                when (response) {
                    is ApiResponse.Success -> {
                        val tourResponse = response.data
                        val tourEntity = tour.copy(
                            id = tourResponse.id,
                            imageUri = tourResponse.imageUrl?.toUri(),
                            createdAt = tourResponse.createdAt,
                            updatedAt = tourResponse.updatedAt
                        ).toEntity()

                        tourDao.insertTour(tourEntity)
                        return@withContext tourResponse.id
                    }

                    is ApiResponse.Error -> {
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
            } catch (_: Exception) {
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
        } else {
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
            try {
                when (val response = NetworkUtils.safeApiCall { tourApi.getAllTours() }) {
                    is ApiResponse.Success -> {
                        val tourDtos = response.data
                        val tours = mutableListOf<Tour>()

                        for (tourMinDto in tourDtos) {
                            val tourDetailsResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourDetailsResponse is ApiResponse.Success) {
                                val tourData = tourDetailsResponse.data
                                val tour = Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.toUri(),
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
                                tours.add(tour)
                            }
                        }

                        tourDao.deleteAllTours()
                        tours.forEach { tourDao.insertTour(it.toEntity()) }
                        emit(tours)
                    }

                    is ApiResponse.Error -> {
                        val localTours = tourDao.getAllTours().first().map { it.toDomain() }
                        emit(localTours)
                    }

                    ApiResponse.Loading -> {
                        val localTours = tourDao.getAllTours().first().map { it.toDomain() }
                        emit(localTours)
                    }
                }
            } catch (_: Exception) {
                val localTours = tourDao.getAllTours().first().map { it.toDomain() }
                emit(localTours)
            }
        } else {
            val localTours = tourDao.getAllTours().first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override fun getTourById(id: Long): Flow<Tour?> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall { tourApi.getTourById(id) }) {
                    is ApiResponse.Success -> {
                        val tourData = response.data
                        val tour = Tour(
                            id = tourData.id,
                            title = tourData.title,
                            description = tourData.description,
                            imageUri = tourData.imageUrl?.toUri(),
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

                        tourDao.insertTour(tour.toEntity())
                        emit(tour)
                    }

                    is ApiResponse.Error -> {
                        val localTour = tourDao.getTourById(id).first()?.toDomain()
                        emit(localTour)
                    }

                    ApiResponse.Loading -> {
                        val localTour = tourDao.getTourById(id).first()?.toDomain()
                        emit(localTour)
                    }
                }
            } catch (_: Exception) {
                val localTour = tourDao.getTourById(id).first()?.toDomain()
                emit(localTour)
            }
        } else {
            val localTour = tourDao.getTourById(id).first()?.toDomain()
            emit(localTour)
        }
    }

    override fun getToursByOrganizer(organizerId: Long): Flow<List<Tour>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response =
                    NetworkUtils.safeApiCall { tourApi.getToursByOrganizer(organizerId) }) {
                    is ApiResponse.Success -> {
                        val tours = mutableListOf<Tour>()

                        for (tourMinDto in response.data) {
                            val tourDetailsResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourDetailsResponse is ApiResponse.Success) {
                                val tourData = tourDetailsResponse.data
                                val tour = Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.toUri(),
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
                                tours.add(tour)
                                tourDao.insertTour(tour.toEntity())
                            }
                        }

                        emit(tours)
                    }

                    is ApiResponse.Error -> {
                        val localTours =
                            tourDao.getToursByOrganizer(organizerId).first().map { it.toDomain() }
                        emit(localTours)
                    }

                    ApiResponse.Loading -> {
                        val localTours =
                            tourDao.getToursByOrganizer(organizerId).first().map { it.toDomain() }
                        emit(localTours)
                    }
                }
            } catch (_: Exception) {
                val localTours =
                    tourDao.getToursByOrganizer(organizerId).first().map { it.toDomain() }
                emit(localTours)
            }
        } else {
            val localTours = tourDao.getToursByOrganizer(organizerId).first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override fun getToursByCategory(categoryId: Long): Flow<List<Tour>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response =
                    NetworkUtils.safeApiCall { tourApi.getToursByCategory(categoryId) }) {
                    is ApiResponse.Success -> {
                        val tours = mutableListOf<Tour>()

                        for (tourMinDto in response.data) {
                            val tourDetailsResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourDetailsResponse is ApiResponse.Success) {
                                val tourData = tourDetailsResponse.data
                                val tour = Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.toUri(),
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
                                tours.add(tour)
                                tourDao.insertTour(tour.toEntity())
                            }
                        }

                        emit(tours)
                    }

                    is ApiResponse.Error -> {
                        val localTours =
                            tourDao.getToursByCategory(categoryId).first().map { it.toDomain() }
                        emit(localTours)
                    }

                    ApiResponse.Loading -> {
                        val localTours =
                            tourDao.getToursByCategory(categoryId).first().map { it.toDomain() }
                        emit(localTours)
                    }
                }
            } catch (_: Exception) {
                val localTours =
                    tourDao.getToursByCategory(categoryId).first().map { it.toDomain() }
                emit(localTours)
            }
        } else {
            val localTours = tourDao.getToursByCategory(categoryId).first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override fun getUpcomingTours(startDate: LocalDateTime): Flow<List<Tour>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall { tourApi.getUpcomingTours() }) {
                    is ApiResponse.Success -> {
                        val tours = mutableListOf<Tour>()

                        for (tourMinDto in response.data) {
                            val tourDetailsResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourDetailsResponse is ApiResponse.Success) {
                                val tourData = tourDetailsResponse.data
                                val tour = Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.toUri(),
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

                                if (tour.startDateTime.isAfter(startDate)) {
                                    tours.add(tour)
                                    tourDao.insertTour(tour.toEntity())
                                }
                            }
                        }

                        emit(tours)
                    }

                    is ApiResponse.Error -> {
                        val localTours =
                            tourDao.getUpcomingTours(startDate).first().map { it.toDomain() }
                        emit(localTours)
                    }

                    ApiResponse.Loading -> {
                        val localTours =
                            tourDao.getUpcomingTours(startDate).first().map { it.toDomain() }
                        emit(localTours)
                    }
                }
            } catch (_: Exception) {
                val localTours = tourDao.getUpcomingTours(startDate).first().map { it.toDomain() }
                emit(localTours)
            }
        } else {
            val localTours = tourDao.getUpcomingTours(startDate).first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override fun searchTours(query: String): Flow<List<Tour>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall { tourApi.searchTours(query) }) {
                    is ApiResponse.Success -> {
                        val tours = mutableListOf<Tour>()

                        for (tourMinDto in response.data) {
                            val tourDetailsResponse = NetworkUtils.safeApiCall {
                                tourApi.getTourById(tourMinDto.id)
                            }

                            if (tourDetailsResponse is ApiResponse.Success) {
                                val tourData = tourDetailsResponse.data
                                val tour = Tour(
                                    id = tourData.id,
                                    title = tourData.title,
                                    description = tourData.description,
                                    imageUri = tourData.imageUrl?.toUri(),
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
                                tours.add(tour)
                                tourDao.insertTour(tour.toEntity())
                            }
                        }

                        emit(tours)
                    }

                    is ApiResponse.Error -> {
                        val localTours = tourDao.searchTours(query).first().map { it.toDomain() }
                        emit(localTours)
                    }

                    ApiResponse.Loading -> {
                        val localTours = tourDao.searchTours(query).first().map { it.toDomain() }
                        emit(localTours)
                    }
                }
            } catch (_: Exception) {
                val localTours = tourDao.searchTours(query).first().map { it.toDomain() }
                emit(localTours)
            }
        } else {
            val localTours = tourDao.searchTours(query).first().map { it.toDomain() }
            emit(localTours)
        }
    }

    override suspend fun deleteTour(id: Long) = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (NetworkUtils.safeApiCall { tourApi.deleteTour(id) }) {
                    is ApiResponse.Success -> {
                        tourDao.deleteTour(id)
                    }

                    is ApiResponse.Error -> {
                        val syncEntity = SyncEntity(
                            entityType = "tour",
                            entityId = id,
                            actionType = SyncActionType.DELETE
                        )
                        syncDao.insertSyncAction(syncEntity)
                        tourDao.deleteTour(id)
                    }

                    ApiResponse.Loading -> {
                        // Should not happen with safeApiCall
                    }
                }
            } catch (_: Exception) {
                val syncEntity = SyncEntity(
                    entityType = "tour",
                    entityId = id,
                    actionType = SyncActionType.DELETE
                )
                syncDao.insertSyncAction(syncEntity)
                tourDao.deleteTour(id)
            }
        } else {
            val syncEntity = SyncEntity(
                entityType = "tour",
                entityId = id,
                actionType = SyncActionType.DELETE
            )
            syncDao.insertSyncAction(syncEntity)
            tourDao.deleteTour(id)
        }
    }
}