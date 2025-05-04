package com.xwurfel.tourry.data.booking.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.xwurfel.tourry.data.booking.entity.BookingEntity
import com.xwurfel.tourry.domain.booking.model.BookingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(bookingEntity: BookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookingEntities: List<BookingEntity>)

    @Query("SELECT * FROM bookings")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    fun getBookingById(id: Long): Flow<BookingEntity?>

    @Query("SELECT * FROM bookings WHERE userId = :userId")
    fun getBookingsByUser(userId: Long): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE tourId = :tourId")
    fun getBookingsByTour(tourId: Long): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE tourId = :tourId AND status = :status")
    fun getBookingsByTourAndStatus(tourId: Long, status: BookingStatus): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE userId = :userId AND status = :status")
    fun getBookingsByUserAndStatus(userId: Long, status: BookingStatus): Flow<List<BookingEntity>>

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateBookingStatus(id: Long, status: BookingStatus)

    @Query("UPDATE bookings SET paymentProcessed = :processed WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, processed: Boolean)

    @Query("SELECT COUNT(*) FROM bookings WHERE tourId = :tourId AND status != :excludeStatus")
    suspend fun getActiveBookingsCountForTour(
        tourId: Long,
        excludeStatus: BookingStatus = BookingStatus.CANCELLED
    ): Int

    @Query("SELECT SUM(numberOfParticipants) FROM bookings WHERE tourId = :tourId AND status != :excludeStatus")
    suspend fun getTotalParticipantsForTour(
        tourId: Long,
        excludeStatus: BookingStatus = BookingStatus.CANCELLED
    ): Int?

    @Transaction
    @Query("DELETE FROM bookings")
    suspend fun deleteAllBookings()

    @Transaction
    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBooking(id: Long)

    @Transaction
    @Query("DELETE FROM bookings WHERE tourId = :tourId")
    suspend fun deleteBookingsForTour(tourId: Long)
}