package com.xwurfel.tourry.feature.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xwurfel.tourry.R
import com.xwurfel.tourry.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// TODO: USE THIS
@Singleton
class TourryNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    fun showGeofenceExitNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GEOFENCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.geofence_exit_title))
            .setContentText(context.getString(R.string.geofence_exit_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(GEOFENCE_EXIT_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle notification permission not granted
        }
    }

    fun showRegroupNotification(tourTitle: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REGROUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.regroup_title))
            .setContentText(context.getString(R.string.regroup_text, tourTitle))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        try {
            notificationManager.notify(REGROUP_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Handle notification permission not granted
        }
    }

    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                GEOFENCE_CHANNEL_ID,
                context.getString(R.string.geofence_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.geofence_channel_description)
            },
            NotificationChannel(
                REGROUP_CHANNEL_ID,
                context.getString(R.string.regroup_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.regroup_channel_description)
            }
        )

        val systemNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        channels.forEach { channel ->
            systemNotificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val GEOFENCE_CHANNEL_ID = "geofence_alerts"
        private const val REGROUP_CHANNEL_ID = "regroup_requests"

        private const val GEOFENCE_EXIT_NOTIFICATION_ID = 2001
        private const val REGROUP_NOTIFICATION_ID = 2002
    }
}