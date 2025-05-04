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
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.di.coroutines.IoDispatcher
import com.xwurfel.tourry.domain.booking.model.Booking
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import com.xwurfel.tourry.domain.booking.repository.BookingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

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

            when (val response = NetworkUtils.safeApiCall {
                bookingApi.createBooking(bookingCreateDto)
            }) {
                is ApiResponse.Success -> {
                    val createdBooking = response.data

                    // Create booking entity from response
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

                    // Save to local database
                    bookingDao.insertBooking(bookingEntity)

                    return@withContext createdBooking.id
                }

                is ApiResponse.Error -> {
                    // Fall back to local save
                    val localId = bookingDao.insertBooking(booking.toEntity())

                    // Mark for sync later
                    val syncEntity = com.xwurfel.tourry.data.sync.SyncEntity(
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
        } else {
            // Save locally for offline mode
            val localId = bookingDao.insertBooking(booking.toEntity())

            // Mark for sync later
            val syncEntity = com.xwurfel.tourry.data.sync.SyncEntity(
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

                    // Update local cache
                    bookingDao.deleteAllBookings()
                    bookingDao.insertBookings(bookings.map { it.toEntity() })

                    emit(bookings)
                }

                is ApiResponse.Error -> {
                    // Fall back to local cache
                    emitAll(bookingDao.getAllBookings().map { entities ->
                        entities.map { it.toDomain() }
                    })
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            // Use local cache in offline mode
            emitAll(bookingDao.getAllBookings().map { entities ->
                entities.map { it.toDomain() }
            })
        }
    }

    override fun getBookingById(id: Long): Flow<Booking?> = flow {
        if (NetworkUtils.isNetworkAvailable(context)) {
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

                    // Update local cache
                    bookingDao.insertBooking(booking.toEntity())

                    emit(booking)
                }

                is ApiResponse.Error -> {
                    // Fall back to local cache
                    emitAll(bookingDao.getBookingById(id).map { it?.toDomain() })
                }

                ApiResponse.Loading -> {
                    // Should not happen with safeApiCall
                }
            }
        } else {
            // Use local cache in offline mode
            emitAll(bookingDao.getBookingById(id).map { it?.toDomain() })
        }
    }

    override fun getBookingsByUser(userId: Long): Flow<List<Booking>> {
        TODO("Not yet implemented")
    }

    override fun getBookingsByTour(tourId: Long): Flow<List<Booking>> {
        TODO("Not yet implemented")
    }

    override fun getBookingsByTourAndStatus(
        tourId: Long,
        status: BookingStatus
    ): Flow<List<Booking>> {
        TODO("Not yet implemented")
    }

    override fun getBookingsByUserAndStatus(
        userId: Long,
        status: BookingStatus
    ): Flow<List<Booking>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateBookingStatus(id: Long, status: BookingStatus) {
        withContext(ioDispatcher) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val statusDto = UpdateBookingStatusDto(status.name)

                when (val response = NetworkUtils.safeApiCall {
                    bookingApi.updateBookingStatus(id, statusDto)
                }) {
                    is ApiResponse.Success -> {
                        bookingDao.updateBookingStatus(id, status)
                    }

                    is ApiResponse.Error -> {
                        bookingDao.updateBookingStatus(id, status)

                        val syncEntity = com.xwurfel.tourry.data.sync.SyncEntity(
                            entityType = "booking_status",
                            entityId = id,
                            actionType = SyncActionType.UPDATE,
                            actionData = gson.toJson(mapOf("status" to status.name))
                        )
                        syncDao.insertSyncAction(syncEntity)
                    }

                    ApiResponse.Loading -> {}
                }
            } else {
                bookingDao.updateBookingStatus(id, status)

                val syncEntity = com.xwurfel.tourry.data.sync.SyncEntity(
                    entityType = "booking_status",
                    entityId = id,
                    actionType = SyncActionType.UPDATE,
                    actionData = gson.toJson(mapOf("status" to status.name))
                )
                syncDao.insertSyncAction(syncEntity)
            }
        }
    }

    // TODO: Implement this
    override suspend fun updatePaymentStatus(id: Long, processed: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun getActiveBookingsCountForTour(tourId: Long): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getTotalParticipantsForTour(tourId: Long): Int? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteBooking(id: Long) {
        TODO("Not yet implemented")
    }

}