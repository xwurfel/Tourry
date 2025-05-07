package com.xwurfel.tourry.data.booking.repository

import android.content.Context
import com.google.gson.Gson
import com.xwurfel.tourry.data.booking.dao.BookingDao
import com.xwurfel.tourry.data.booking.mapper.toDomain
import com.xwurfel.tourry.data.booking.mapper.toEntity
import com.xwurfel.tourry.data.network.api.BookingApi
import com.xwurfel.tourry.data.network.dto.BookingCreateDto
import com.xwurfel.tourry.data.network.dto.UpdateBookingStatusDto
import com.xwurfel.tourry.data.network.util.ApiResponse
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.booking.model.Booking
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import com.xwurfel.tourry.domain.booking.repository.BookingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepositoryImpl @Inject constructor(
    private val bookingApi: BookingApi,
    private val bookingDao: BookingDao,
    private val syncDao: SyncDao,
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BookingRepository {

    override suspend fun createBooking(booking: Booking): Long = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            val bookingCreateDto = BookingCreateDto(
                tourId = booking.tourId,
                numberOfParticipants = booking.numberOfParticipants,
                notes = booking.notes
            )

            try {
                when (val response = NetworkUtils.safeApiCall {
                    bookingApi.createBooking(bookingCreateDto)
                }) {
                    is ApiResponse.Success -> {
                        val createdBooking = response.data
                        val bookingEntity = Booking(
                            id = createdBooking.id,
                            tourId = createdBooking.tour.id,
                            userId = createdBooking.user.id,
                            numberOfParticipants = createdBooking.numberOfParticipants,
                            totalPrice = createdBooking.totalPrice,
                            status = BookingStatus.valueOf(createdBooking.status),
                            notes = createdBooking.notes,
                            paymentProcessed = createdBooking.paymentProcessed,
                            createdAt = createdBooking.createdAt,
                            updatedAt = createdBooking.updatedAt
                        ).toEntity()

                        bookingDao.insertBooking(bookingEntity)
                        return@withContext createdBooking.id
                    }

                    is ApiResponse.Error -> {
                        val localId = bookingDao.insertBooking(booking.toEntity())

                        val syncEntity = SyncEntity(
                            entityType = "booking",
                            entityId = localId,
                            actionType = SyncActionType.CREATE,
                            actionData = gson.toJson(booking.copy(id = localId))
                        )
                        syncDao.insertSyncAction(syncEntity)

                        return@withContext localId
                    }

                    ApiResponse.Loading -> {
                        throw Exception("Request is still loading")
                    }
                }
            } catch (_: Exception) {
                val localId = bookingDao.insertBooking(booking.toEntity())

                val syncEntity = SyncEntity(
                    entityType = "booking",
                    entityId = localId,
                    actionType = SyncActionType.CREATE,
                    actionData = gson.toJson(booking.copy(id = localId))
                )
                syncDao.insertSyncAction(syncEntity)

                return@withContext localId
            }
        } else {
            val localId = bookingDao.insertBooking(booking.toEntity())

            val syncEntity = SyncEntity(
                entityType = "booking",
                entityId = localId,
                actionType = SyncActionType.CREATE,
                actionData = gson.toJson(booking.copy(id = localId))
            )
            syncDao.insertSyncAction(syncEntity)

            return@withContext localId
        }
    }

    override fun getAllBookings(): Flow<List<Booking>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall { bookingApi.getAllBookings() }) {
                    is ApiResponse.Success -> {
                        val bookings = response.data.map { dto ->
                            Booking(
                                id = dto.id,
                                tourId = dto.tour.id,
                                userId = dto.user.id,
                                numberOfParticipants = dto.numberOfParticipants,
                                totalPrice = dto.totalPrice,
                                status = BookingStatus.valueOf(dto.status),
                                notes = dto.notes,
                                paymentProcessed = dto.paymentProcessed,
                                createdAt = dto.createdAt,
                                updatedAt = dto.updatedAt
                            )
                        }

                        bookingDao.deleteAllBookings()
                        bookingDao.insertBookings(bookings.map { it.toEntity() })
                        emit(bookings)
                    }

                    is ApiResponse.Error -> {
                        val localBookings =
                            bookingDao.getAllBookings().first().map { it.toDomain() }
                        emit(localBookings)
                    }

                    ApiResponse.Loading -> {
                        val localBookings =
                            bookingDao.getAllBookings().first().map { it.toDomain() }
                        emit(localBookings)
                    }
                }
            } catch (_: Exception) {
                val localBookings = bookingDao.getAllBookings().first().map { it.toDomain() }
                emit(localBookings)
            }
        } else {
            val localBookings = bookingDao.getAllBookings().first().map { it.toDomain() }
            emit(localBookings)
        }
    }

    override fun getBookingById(id: Long): Flow<Booking?> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response = NetworkUtils.safeApiCall { bookingApi.getBookingById(id) }) {
                    is ApiResponse.Success -> {
                        val dto = response.data
                        val booking = Booking(
                            id = dto.id,
                            tourId = dto.tour.id,
                            userId = dto.user.id,
                            numberOfParticipants = dto.numberOfParticipants,
                            totalPrice = dto.totalPrice,
                            status = BookingStatus.valueOf(dto.status),
                            notes = dto.notes,
                            paymentProcessed = dto.paymentProcessed,
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt
                        )

                        bookingDao.insertBooking(booking.toEntity())
                        emit(booking)
                    }

                    is ApiResponse.Error -> {
                        val localBooking = bookingDao.getBookingById(id).first()?.toDomain()
                        emit(localBooking)
                    }

                    ApiResponse.Loading -> {
                        val localBooking = bookingDao.getBookingById(id).first()?.toDomain()
                        emit(localBooking)
                    }
                }
            } catch (_: Exception) {
                val localBooking = bookingDao.getBookingById(id).first()?.toDomain()
                emit(localBooking)
            }
        } else {
            val localBooking = bookingDao.getBookingById(id).first()?.toDomain()
            emit(localBooking)
        }
    }

    override fun getBookingsByUser(userId: Long): Flow<List<Booking>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response =
                    NetworkUtils.safeApiCall { bookingApi.getCurrentUserBookings() }) {
                    is ApiResponse.Success -> {
                        val bookings = response.data.map { dto ->
                            Booking(
                                id = dto.id,
                                tourId = dto.tour.id,
                                userId = dto.user.id,
                                numberOfParticipants = dto.numberOfParticipants,
                                totalPrice = dto.totalPrice,
                                status = BookingStatus.valueOf(dto.status),
                                notes = dto.notes,
                                paymentProcessed = dto.paymentProcessed,
                                createdAt = dto.createdAt,
                                updatedAt = dto.updatedAt
                            )
                        }.filter { it.userId == userId }

                        bookingDao.getBookingsByUser(userId)
                        bookingDao.insertBookings(bookings.map { it.toEntity() })

                        emit(bookings)
                    }

                    is ApiResponse.Error -> {
                        val localBookings =
                            bookingDao.getBookingsByUser(userId).first().map { it.toDomain() }
                        emit(localBookings)
                    }

                    ApiResponse.Loading -> {
                        val localBookings =
                            bookingDao.getBookingsByUser(userId).first().map { it.toDomain() }
                        emit(localBookings)
                    }
                }
            } catch (_: Exception) {
                val localBookings =
                    bookingDao.getBookingsByUser(userId).first().map { it.toDomain() }
                emit(localBookings)
            }
        } else {
            val localBookings = bookingDao.getBookingsByUser(userId).first().map { it.toDomain() }
            emit(localBookings)
        }
    }

    override fun getBookingsByTour(tourId: Long): Flow<List<Booking>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response =
                    NetworkUtils.safeApiCall { bookingApi.getBookingsForTour(tourId) }) {
                    is ApiResponse.Success -> {
                        val bookings = response.data.map { dto ->
                            Booking(
                                id = dto.id,
                                tourId = dto.tour.id,
                                userId = dto.user.id,
                                numberOfParticipants = dto.numberOfParticipants,
                                totalPrice = dto.totalPrice,
                                status = BookingStatus.valueOf(dto.status),
                                notes = dto.notes,
                                paymentProcessed = dto.paymentProcessed,
                                createdAt = dto.createdAt,
                                updatedAt = dto.updatedAt
                            )
                        }

                        bookingDao.deleteBookingsForTour(tourId)
                        bookingDao.insertBookings(bookings.map { it.toEntity() })

                        emit(bookings)
                    }

                    is ApiResponse.Error -> {
                        val localBookings =
                            bookingDao.getBookingsByTour(tourId).first().map { it.toDomain() }
                        emit(localBookings)
                    }

                    ApiResponse.Loading -> {
                        val localBookings =
                            bookingDao.getBookingsByTour(tourId).first().map { it.toDomain() }
                        emit(localBookings)
                    }
                }
            } catch (_: Exception) {
                val localBookings =
                    bookingDao.getBookingsByTour(tourId).first().map { it.toDomain() }
                emit(localBookings)
            }
        } else {
            val localBookings = bookingDao.getBookingsByTour(tourId).first().map { it.toDomain() }
            emit(localBookings)
        }
    }

    override fun getBookingsByTourAndStatus(
        tourId: Long,
        status: BookingStatus
    ): Flow<List<Booking>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response =
                    NetworkUtils.safeApiCall { bookingApi.getBookingsForTour(tourId) }) {
                    is ApiResponse.Success -> {
                        val bookings = response.data
                            .filter { dto -> BookingStatus.valueOf(dto.status) == status }
                            .map { dto ->
                                Booking(
                                    id = dto.id,
                                    tourId = dto.tour.id,
                                    userId = dto.user.id,
                                    numberOfParticipants = dto.numberOfParticipants,
                                    totalPrice = dto.totalPrice,
                                    status = BookingStatus.valueOf(dto.status),
                                    notes = dto.notes,
                                    paymentProcessed = dto.paymentProcessed,
                                    createdAt = dto.createdAt,
                                    updatedAt = dto.updatedAt
                                )
                            }

                        // Update cache for these bookings
                        bookings.forEach { booking ->
                            bookingDao.insertBooking(booking.toEntity())
                        }

                        emit(bookings)
                    }

                    is ApiResponse.Error -> {
                        val localBookings =
                            bookingDao.getBookingsByTourAndStatus(tourId, status).first()
                                .map { it.toDomain() }
                        emit(localBookings)
                    }

                    ApiResponse.Loading -> {
                        val localBookings =
                            bookingDao.getBookingsByTourAndStatus(tourId, status).first()
                                .map { it.toDomain() }
                        emit(localBookings)
                    }
                }
            } catch (_: Exception) {
                val localBookings = bookingDao.getBookingsByTourAndStatus(tourId, status).first()
                    .map { it.toDomain() }
                emit(localBookings)
            }
        } else {
            val localBookings =
                bookingDao.getBookingsByTourAndStatus(tourId, status).first().map { it.toDomain() }
            emit(localBookings)
        }
    }

    override fun getBookingsByUserAndStatus(
        userId: Long,
        status: BookingStatus
    ): Flow<List<Booking>> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (val response =
                    NetworkUtils.safeApiCall { bookingApi.getCurrentUserBookings() }) {
                    is ApiResponse.Success -> {
                        val bookings = response.data
                            .filter { dto ->
                                dto.user.id == userId && BookingStatus.valueOf(dto.status) == status
                            }
                            .map { dto ->
                                Booking(
                                    id = dto.id,
                                    tourId = dto.tour.id,
                                    userId = dto.user.id,
                                    numberOfParticipants = dto.numberOfParticipants,
                                    totalPrice = dto.totalPrice,
                                    status = BookingStatus.valueOf(dto.status),
                                    notes = dto.notes,
                                    paymentProcessed = dto.paymentProcessed,
                                    createdAt = dto.createdAt,
                                    updatedAt = dto.updatedAt
                                )
                            }

                        bookings.forEach { booking ->
                            bookingDao.insertBooking(booking.toEntity())
                        }

                        emit(bookings)
                    }

                    is ApiResponse.Error -> {
                        val localBookings =
                            bookingDao.getBookingsByUserAndStatus(userId, status).first()
                                .map { it.toDomain() }
                        emit(localBookings)
                    }

                    ApiResponse.Loading -> {
                        val localBookings =
                            bookingDao.getBookingsByUserAndStatus(userId, status).first()
                                .map { it.toDomain() }
                        emit(localBookings)
                    }
                }
            } catch (_: Exception) {
                val localBookings = bookingDao.getBookingsByUserAndStatus(userId, status).first()
                    .map { it.toDomain() }
                emit(localBookings)
            }
        } else {
            val localBookings =
                bookingDao.getBookingsByUserAndStatus(userId, status).first().map { it.toDomain() }
            emit(localBookings)
        }
    }

    override suspend fun updateBookingStatus(id: Long, status: BookingStatus): Unit =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val statusDto = UpdateBookingStatusDto(status.name)

                try {
                    when (NetworkUtils.safeApiCall {
                        bookingApi.updateBookingStatus(id, statusDto)
                    }) {
                        is ApiResponse.Success -> {
                            bookingDao.updateBookingStatus(id, status)
                        }

                        is ApiResponse.Error -> {
                            bookingDao.updateBookingStatus(id, status)

                            val syncEntity = SyncEntity(
                                entityType = "booking_status",
                                entityId = id,
                                actionType = SyncActionType.UPDATE,
                                actionData = gson.toJson(mapOf("status" to status.name))
                            )
                            syncDao.insertSyncAction(syncEntity)
                        }

                        ApiResponse.Loading -> {
                            // Should not happen with safeApiCall
                        }
                    }
                } catch (_: Exception) {
                    bookingDao.updateBookingStatus(id, status)

                    val syncEntity = SyncEntity(
                        entityType = "booking_status",
                        entityId = id,
                        actionType = SyncActionType.UPDATE,
                        actionData = gson.toJson(mapOf("status" to status.name))
                    )
                    syncDao.insertSyncAction(syncEntity)
                }
            } else {
                bookingDao.updateBookingStatus(id, status)

                val syncEntity = SyncEntity(
                    entityType = "booking_status",
                    entityId = id,
                    actionType = SyncActionType.UPDATE,
                    actionData = gson.toJson(mapOf("status" to status.name))
                )
                syncDao.insertSyncAction(syncEntity)
            }
        }

    override suspend fun updatePaymentStatus(id: Long, processed: Boolean): Unit =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    when (NetworkUtils.safeApiCall {
                        bookingApi.processPayment(id)
                    }) {
                        is ApiResponse.Success -> {
                            bookingDao.updatePaymentStatus(id, processed)
                        }

                        is ApiResponse.Error -> {
                            bookingDao.updatePaymentStatus(id, processed)

                            val syncEntity = SyncEntity(
                                entityType = "booking_payment",
                                entityId = id,
                                actionType = SyncActionType.UPDATE,
                                actionData = gson.toJson(mapOf("paymentProcessed" to processed))
                            )
                            syncDao.insertSyncAction(syncEntity)
                        }

                        ApiResponse.Loading -> {
                            // Should not happen with safeApiCall
                        }
                    }
                } catch (_: Exception) {
                    bookingDao.updatePaymentStatus(id, processed)

                    val syncEntity = SyncEntity(
                        entityType = "booking_payment",
                        entityId = id,
                        actionType = SyncActionType.UPDATE,
                        actionData = gson.toJson(mapOf("paymentProcessed" to processed))
                    )
                    syncDao.insertSyncAction(syncEntity)
                }
            } else {
                bookingDao.updatePaymentStatus(id, processed)

                val syncEntity = SyncEntity(
                    entityType = "booking_payment",
                    entityId = id,
                    actionType = SyncActionType.UPDATE,
                    actionData = gson.toJson(mapOf("paymentProcessed" to processed))
                )
                syncDao.insertSyncAction(syncEntity)
            }
        }

    override suspend fun getActiveBookingsCountForTour(tourId: Long): Int =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    when (val response =
                        NetworkUtils.safeApiCall { bookingApi.getBookingsForTour(tourId) }) {
                        is ApiResponse.Success -> {
                            response.data.count { dto ->
                                BookingStatus.valueOf(dto.status) != BookingStatus.CANCELLED
                            }
                        }

                        is ApiResponse.Error -> {
                            bookingDao.getActiveBookingsCountForTour(tourId)
                        }

                        ApiResponse.Loading -> {
                            bookingDao.getActiveBookingsCountForTour(tourId)
                        }
                    }
                } catch (_: Exception) {
                    bookingDao.getActiveBookingsCountForTour(tourId)
                }
            } else {
                bookingDao.getActiveBookingsCountForTour(tourId)
            }
        }

    override suspend fun getTotalParticipantsForTour(tourId: Long): Int? =
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    when (val response =
                        NetworkUtils.safeApiCall { bookingApi.getBookingsForTour(tourId) }) {
                        is ApiResponse.Success -> {
                            response.data
                                .filter { dto -> BookingStatus.valueOf(dto.status) != BookingStatus.CANCELLED }
                                .sumOf { it.numberOfParticipants }
                        }

                        is ApiResponse.Error -> {
                            bookingDao.getTotalParticipantsForTour(tourId)
                        }

                        ApiResponse.Loading -> {
                            bookingDao.getTotalParticipantsForTour(tourId)
                        }
                    }
                } catch (_: Exception) {
                    bookingDao.getTotalParticipantsForTour(tourId)
                }
            } else {
                bookingDao.getTotalParticipantsForTour(tourId)
            }
        }

    override suspend fun deleteBooking(id: Long) = withContext(ioDispatcher) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                when (NetworkUtils.safeApiCall { bookingApi.deleteBooking(id) }) {
                    is ApiResponse.Success -> {
                        bookingDao.deleteBooking(id)
                    }

                    is ApiResponse.Error -> {
                        val syncEntity = SyncEntity(
                            entityType = "booking",
                            entityId = id,
                            actionType = SyncActionType.DELETE
                        )
                        syncDao.insertSyncAction(syncEntity)

                        bookingDao.deleteBooking(id)
                    }

                    ApiResponse.Loading -> {
                    }
                }
            } catch (_: Exception) {
                val syncEntity = SyncEntity(
                    entityType = "booking",
                    entityId = id,
                    actionType = SyncActionType.DELETE
                )
                syncDao.insertSyncAction(syncEntity)

                bookingDao.deleteBooking(id)
            }
        } else {
            val syncEntity = SyncEntity(
                entityType = "booking",
                entityId = id,
                actionType = SyncActionType.DELETE
            )
            syncDao.insertSyncAction(syncEntity)

            bookingDao.deleteBooking(id)
        }
    }
}