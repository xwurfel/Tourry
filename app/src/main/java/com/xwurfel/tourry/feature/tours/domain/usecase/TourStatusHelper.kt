package com.xwurfel.tourry.feature.tours.domain.usecase

import com.xwurfel.tourry.feature.tours.domain.model.TourStatus

object TourStatusHelper {

    /**
     * Calculates the current status of a tour based on time and manual state
     */
    fun calculateTourStatus(
        startTime: Long,
        isManuallyStarted: Boolean = false,
        isCompleted: Boolean = false,
        isCancelled: Boolean = false,
        currentTime: Long = System.currentTimeMillis()
    ): TourStatus {
        return when {
            isCancelled -> TourStatus.CANCELLED
            isCompleted -> TourStatus.COMPLETED
            isManuallyStarted -> TourStatus.ACTIVE
            isReadyToStart(startTime, currentTime) -> TourStatus.READY_TO_START
            else -> TourStatus.UPCOMING
        }
    }

    /**
     * Determines if a tour is ready to be started (within 1 hour of start time)
     */
    fun isReadyToStart(startTime: Long, currentTime: Long = System.currentTimeMillis()): Boolean {
        val timeUntilStart = startTime - currentTime
        return timeUntilStart in 0..3600000 // 0 to 1 hour in milliseconds
    }

    /**
     * Determines if a tour can be manually started by a participant
     */
    fun canStartTour(
        status: TourStatus,
        isUserJoined: Boolean,
        startTime: Long,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        return status == TourStatus.READY_TO_START &&
                isUserJoined &&
                isReadyToStart(startTime, currentTime)
    }

    /**
     * Gets the display text for a tour status
     */
    fun getStatusDisplayText(status: TourStatus): String {
        return when (status) {
            TourStatus.UPCOMING -> "Upcoming"
            TourStatus.READY_TO_START -> "Ready to Start"
            TourStatus.ACTIVE -> "Live Now"
            TourStatus.COMPLETED -> "Completed"
            TourStatus.CANCELLED -> "Cancelled"
        }
    }

    /**
     * Gets the action button text based on tour status and user state
     */
    fun getActionButtonText(
        status: TourStatus,
        isUserJoined: Boolean
    ): String? {
        return when {
            !isUserJoined && status in listOf(
                TourStatus.UPCOMING,
                TourStatus.READY_TO_START
            ) -> "Join Tour"

            isUserJoined && status == TourStatus.READY_TO_START -> "Start Tour"
            isUserJoined && status == TourStatus.ACTIVE -> "Continue Tour"
            else -> null
        }
    }
}