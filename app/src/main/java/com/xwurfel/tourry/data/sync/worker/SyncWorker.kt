package com.xwurfel.tourry.data.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.dao.SyncDao
import com.xwurfel.tourry.domain.booking.repository.BookingRepository
import com.xwurfel.tourry.domain.route.repository.RouteRepository
import com.xwurfel.tourry.domain.tour.repository.CheckInRepository
import com.xwurfel.tourry.domain.tour.repository.TourRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


//TODO: Finish this
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncDao: SyncDao,
    private val tourRepository: TourRepository,
    private val bookingRepository: BookingRepository,
    private val routeRepository: RouteRepository,
    private val checkInRepository: CheckInRepository,
    private val gson: Gson
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Check if network is available
        if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            return@withContext Result.retry()
        }

        try {
            // Process CREATE actions
            processSyncActions(SyncActionType.CREATE)

            // Process UPDATE actions
            processSyncActions(SyncActionType.UPDATE)

            // Process DELETE actions
            processSyncActions(SyncActionType.DELETE)

            Result.success()
        } catch (e: Exception) {
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
                    "route_point" -> syncRoutePoint(action)
                    "check_in" -> syncCheckIn(action)
                    // Add other entity types as needed
                }

                // Remove the sync action after successful processing
                syncDao.deleteSyncAction(action.id)
            } catch (e: Exception) {
                // Log error but continue with other actions
                // If we're on the last retry, we'll fail the entire work
                if (runAttemptCount >= 3) {
                    throw e
                }
            }
        }
    }

    private suspend fun syncTour(action: SyncEntity) {
        // Implementation for syncing tour entities
        // This will depend on the specific structure of your Tour domain model
        // and how it's stored in the actionData JSON
    }

    private suspend fun syncBooking(action: SyncEntity) {
        // Implementation for syncing booking entities
    }

    private suspend fun syncRoutePoint(action: SyncEntity) {
        // Implementation for syncing route point entities
    }

    private suspend fun syncCheckIn(action: SyncEntity) {
        // Implementation for syncing check-in entities
    }
}