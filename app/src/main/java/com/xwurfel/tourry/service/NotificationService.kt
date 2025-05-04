package com.xwurfel.tourry.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.xwurfel.tourry.R
import com.xwurfel.tourry.core.MainActivity
import com.xwurfel.tourry.presentation.navigation.Destinations
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager
) {
    companion object {
        private const val CHANNEL_ID_TOUR_GEOFENCE = "tourry_geofence_channel"
        private const val CHANNEL_ID_TOUR_CHECK_IN = "tourry_check_in_channel"
        private const val NOTIFICATION_ID_GEOFENCE = 1001
        private const val NOTIFICATION_ID_CHECK_IN = 1002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Geofence channel
        val geofenceChannel = NotificationChannel(
            CHANNEL_ID_TOUR_GEOFENCE,
            "Tour Geofence Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when entering or exiting tour points of interest"
        }

        // Check-in channel
        val checkInChannel = NotificationChannel(
            CHANNEL_ID_TOUR_CHECK_IN,
            "Tour Check-In Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications about tour check-ins"
        }

        notificationManager.createNotificationChannels(listOf(geofenceChannel, checkInChannel))
    }

    /**
     * Shows a geofence notification when user enters a tour point area.
     *
     * @param tourId The tour ID
     * @param pointId The route point ID
     * @param pointName The name of the point
     */
    fun showGeofenceEntryNotification(tourId: Long, pointId: Long, pointName: String) {
        val title = "You've reached $pointName!"
        val message = "You've arrived at a point of interest. Tap to check in."

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("DESTINATION", Destinations.TourCheckIn(tourId).toString())
            putExtra("ROUTE_POINT_ID", pointId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            pointId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TOUR_GEOFENCE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_GEOFENCE, notification)
    }

    /**
     * Shows a geofence notification when user exits a tour point area.
     *
     * @param pointName The name of the point
     */
    fun showGeofenceExitNotification(pointName: String) {
        val title = "You're leaving $pointName"
        val message = "You've left this point of interest. Don't forget to check in if you haven't already!"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TOUR_GEOFENCE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_GEOFENCE + 1, notification)
    }

    /**
     * Shows a notification when a user successfully checks in.
     *
     * @param tourName The name of the tour
     * @param pointName The name of the point checked in to
     * @param progress The current progress (0-1) of tour completion
     */
    fun showCheckInSuccessNotification(tourName: String, pointName: String, progress: Float) {
        val progressPercent = (progress * 100).toInt()
        val title = "Checked in to $pointName"
        val message = "You've checked in to $pointName on tour $tourName. Tour progress: $progressPercent%"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TOUR_CHECK_IN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_CHECK_IN, notification)
    }

    /**
     * Shows a notification when a tour is completed (all points checked in).
     *
     * @param tourName The name of the completed tour
     */
    fun showTourCompletedNotification(tourName: String) {
        val title = "Tour Completed!"
        val message = "Congratulations! You've completed the $tourName tour."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TOUR_CHECK_IN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_CHECK_IN + 1, notification)
    }
}