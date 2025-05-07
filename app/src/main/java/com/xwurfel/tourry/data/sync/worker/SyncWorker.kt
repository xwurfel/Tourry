package com.xwurfel.tourry.data.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.xwurfel.tourry.data.network.api.BookingApi
import com.xwurfel.tourry.data.network.api.CheckInApi
import com.xwurfel.tourry.data.network.api.RouteApi
import com.xwurfel.tourry.data.network.api.TourApi
import com.xwurfel.tourry.data.network.dto.BookingCreateDto
import com.xwurfel.tourry.data.network.dto.CheckInCreateDto
import com.xwurfel.tourry.data.network.dto.RoutePointCreateDto
import com.xwurfel.tourry.data.network.dto.TourCreateDto
import com.xwurfel.tourry.data.network.dto.UpdateBookingStatusDto
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.domain.booking.model.Booking
import com.xwurfel.tourry.domain.route.model.RoutePoint
import com.xwurfel.tourry.domain.tour.model.CheckIn
import com.xwurfel.tourry.domain.tour.model.Tour
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncDao: SyncDao,
    private val tourApi: TourApi,
    private val bookingApi: BookingApi,
    private val routeApi: RouteApi,
    private val checkInApi: CheckInApi,
    private val gson: Gson
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            return@withContext Result.retry()
        }

        try {
            processSyncActions(SyncActionType.CREATE)
            processSyncActions(SyncActionType.UPDATE)
            processSyncActions(SyncActionType.DELETE)

            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun processSyncActions(actionType: SyncActionType) {
        val actions = syncDao.getSyncActionsByType(actionType)

        for (action in actions) {
            try {
                when (action.entityType) {
                    "tour" -> syncTour(action)
                    "booking" -> syncBooking(action)
                    "booking_status" -> syncBookingStatus(action)
                    "route_point" -> syncRoutePoint(action)
                    "check_in" -> syncCheckIn(action)
                }

                syncDao.deleteSyncAction(action.id)
            } catch (e: Exception) {
                if (runAttemptCount >= 3) {
                    throw e
                }
            }
        }
    }

    private suspend fun syncTour(action: SyncEntity) {
        val tourJson = action.actionData ?: return
        val tour = gson.fromJson(tourJson, Tour::class.java)

        val tourCreateDto = TourCreateDto(
            title = tour.title,
            description = tour.description,
            imageUrl = tour.imageUri?.toString(),
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

        when (action.actionType) {
            SyncActionType.CREATE -> {
                val response = NetworkUtils.safeApiCall {
                    tourApi.createTour(tourCreateDto)
                }
                if (response is ApiResponse.Error) {
                    throw Exception("Failed to sync tour creation: ${response.message}")
                }
            }

            SyncActionType.UPDATE -> {
                val response = NetworkUtils.safeApiCall {
                    tourApi.updateTour(tour.id, tourCreateDto)
                }
                if (response is ApiResponse.Error) {
                    throw Exception("Failed to sync tour update: ${response.message}")
                }
            }

            SyncActionType.DELETE -> {
                val response = NetworkUtils.safeApiCall {
                    tourApi.deleteTour(action.entityId)
                }
                if (response is ApiResponse.Error) {
                    throw Exception("Failed to sync tour deletion: ${response.message}")
                }
            }
        }
    }

    private suspend fun syncBooking(action: SyncEntity) {
        val bookingJson = action.actionData ?: return
        val booking = gson.fromJson(bookingJson, Booking::class.java)

        val bookingCreateDto = BookingCreateDto(
            tourId = booking.tourId,
            numberOfParticipants = booking.numberOfParticipants,
            notes = booking.notes
        )

        when (action.actionType) {
            SyncActionType.CREATE -> {
                val response = NetworkUtils.safeApiCall {
                    bookingApi.createBooking(bookingCreateDto)
                }
                if (response is ApiResponse.Error) {
                    throw Exception("Failed to sync booking creation: ${response.message}")
                }
            }

            SyncActionType.DELETE -> {
                val response = NetworkUtils.safeApiCall {
                    bookingApi.deleteBooking(action.entityId)
                }
                if (response is ApiResponse.Error) {
                    throw Exception("Failed to sync booking deletion: ${response.message}")
                }
            }

            else -> {}
        }
    }

    private suspend fun syncBookingStatus(action: SyncEntity) {
        val statusJson = action.actionData ?: return
        val statusMap = gson.fromJson(statusJson, Map::class.java) as Map<String, String>
        val status = statusMap["status"] ?: return

        val statusDto = UpdateBookingStatusDto(status)

        val response = NetworkUtils.safeApiCall {
            bookingApi.updateBookingStatus(action.entityId, statusDto)
        }
        if (response is ApiResponse.Error) {
            throw Exception("Failed to sync booking status: ${response.message}")
        }
    }

    private suspend fun syncRoutePoint(action: SyncEntity) {
        val routePointJson = action.actionData ?: return
        val routePoint = gson.fromJson(routePointJson, RoutePoint::class.java)

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

        when (action.actionType) {
            SyncActionType.CREATE -> {
                val response = NetworkUtils.safeApiCall {
                    routeApi.createRoutePoint(routePointDto)
                }
                if (response is ApiResponse.Error) {
                    throw Exception("Failed to sync route point creation: ${response.message}")
                }
            }

            SyncActionType.UPDATE -> {
                val response = NetworkUtils.safeApiCall {
                    routeApi.updateRoutePoint(routePoint.id, routePointDto)
                }
                if (response is ApiResponse.Error) {
                    throw Exception("Failed to sync route point update: ${response.message}")
                }
            }

            SyncActionType.DELETE -> {
                val response = NetworkUtils.safeApiCall {
                    routeApi.deleteRoutePoint(action.entityId)
                }
                if (response is ApiResponse.Error) {
                    throw Exception("Failed to sync route point deletion: ${response.message}")
                }
            }
        }
    }

    private suspend fun syncCheckIn(action: SyncEntity) {
        val checkInJson = action.actionData ?: return
        val checkIn = gson.fromJson(checkInJson, CheckIn::class.java)

        val checkInDto = CheckInCreateDto(
            tourId = checkIn.tourId,
            routePointId = checkIn.routePointId,
            latitude = 0.0, // Default value, should be replaced with actual location if available
            longitude = 0.0, // Default value, should be replaced with actual location if available
            note = checkIn.note,
            imageUrl = checkIn.imageUri,
            forceCheckIn = true // Force check-in when syncing
        )

        when (action.actionType) {
            SyncActionType.CREATE -> {
                val response = NetworkUtils.safeApiCall {
                    checkInApi.createCheckIn(checkInDto)
                }
                if (response is ApiResponse.Error) {
                    throw Exception("Failed to sync check-in creation: ${response.message}")
                }
            }

            SyncActionType.DELETE -> {
                // API may not support deleting check-ins, handle accordingly
            }

            else -> {}
        }
    }
}