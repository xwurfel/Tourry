package com.xwurfel.tourry.feature.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TourAnalytics @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val analyticsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeSessions = ConcurrentHashMap<String, TourSession>()

    fun startTourSession(tourId: String, tourTitle: String, isCreator: Boolean = false) {
        val session = TourSession(
            tourId = tourId,
            tourTitle = tourTitle,
            isCreator = isCreator,
            startTime = System.currentTimeMillis()
        )
        activeSessions[tourId] = session

        analyticsScope.launch {
            // TODO: Send to analytics backend
            logEvent(
                "tour_started", mapOf(
                    "tour_id" to tourId,
                    "tour_title" to tourTitle,
                    "is_creator" to isCreator.toString()
                )
            )
        }
    }

    fun trackStopVisited(tourId: String, stopId: String, stopName: String, timeSpent: Long = 0) {
        activeSessions[tourId]?.let { session ->
            session.visitedStops.add(
                VisitedStop(
                    stopId, stopName, System.currentTimeMillis(), timeSpent
                )
            )

            analyticsScope.launch {
                logEvent(
                    "stop_visited", mapOf(
                        "tour_id" to tourId,
                        "stop_id" to stopId,
                        "stop_name" to stopName,
                        "time_spent_seconds" to (timeSpent / 1000).toString()
                    )
                )
            }
        }
    }

    fun trackAudioPlayed(tourId: String, stopId: String, audioUrl: String, duration: Long) {
        analyticsScope.launch {
            logEvent(
                "audio_played", mapOf(
                    "tour_id" to tourId,
                    "stop_id" to stopId,
                    "audio_url" to audioUrl,
                    "duration_seconds" to (duration / 1000).toString()
                )
            )
        }
    }

    fun trackTourPaused(tourId: String, reason: String = "user_action") {
        activeSessions[tourId]?.let { session ->
            session.pauseCount++
            session.totalPauseTime += System.currentTimeMillis() - session.lastPauseTime

            analyticsScope.launch {
                logEvent(
                    "tour_paused", mapOf(
                        "tour_id" to tourId,
                        "reason" to reason,
                        "pause_count" to session.pauseCount.toString()
                    )
                )
            }
        }
    }

    fun trackTourResumed(tourId: String) {
        activeSessions[tourId]?.let { session ->
            session.lastPauseTime = System.currentTimeMillis()

            analyticsScope.launch {
                logEvent(
                    "tour_resumed", mapOf(
                        "tour_id" to tourId
                    )
                )
            }
        }
    }

    fun trackGeofenceEvent(
        tourId: String, stopId: String, eventType: String, accuracy: Float? = null
    ) {
        analyticsScope.launch {
            val params = mutableMapOf(
                "tour_id" to tourId, "stop_id" to stopId, "event_type" to eventType
            )
            accuracy?.let { params["accuracy_meters"] = it.toString() }

            logEvent("geofence_event", params)
        }
    }

    fun endTourSession(tourId: String, completionPercentage: Float, rating: Int? = null) {
        activeSessions[tourId]?.let { session ->
            val endTime = System.currentTimeMillis()
            val totalDuration = endTime - session.startTime - session.totalPauseTime

            analyticsScope.launch {
                val params = mutableMapOf(
                    "tour_id" to tourId,
                    "tour_title" to session.tourTitle,
                    "is_creator" to session.isCreator.toString(),
                    "duration_seconds" to (totalDuration / 1000).toString(),
                    "stops_visited" to session.visitedStops.size.toString(),
                    "completion_percentage" to completionPercentage.toString(),
                    "pause_count" to session.pauseCount.toString(),
                    "total_pause_time_seconds" to (session.totalPauseTime / 1000).toString()
                )

                rating?.let { params["rating"] = it.toString() }

                logEvent("tour_completed", params)

                // TODO: Send detailed session data to backend
                sendSessionSummary(session, totalDuration, completionPercentage, rating)
            }

            activeSessions.remove(tourId)
        }
    }

    fun trackUserFeedback(tourId: String, rating: Int, feedback: String) {
        analyticsScope.launch {
            logEvent(
                "feedback_submitted", mapOf(
                    "tour_id" to tourId,
                    "rating" to rating.toString(),
                    "feedback_length" to feedback.length.toString(),
                    "has_feedback" to feedback.isNotBlank().toString()
                )
            )
        }
    }

    fun trackTourCreation(
        tourId: String, stopCount: Int, hasAudio: Boolean, hasImages: Boolean, price: Double
    ) {
        analyticsScope.launch {
            logEvent(
                "tour_created", mapOf(
                    "tour_id" to tourId,
                    "stop_count" to stopCount.toString(),
                    "has_audio" to hasAudio.toString(),
                    "has_images" to hasImages.toString(),
                    "price" to price.toString(),
                    "is_free" to (price == 0.0).toString()
                )
            )
        }
    }

    fun trackSearch(query: String, resultsCount: Int, filterUsed: String? = null) {
        analyticsScope.launch {
            val params = mutableMapOf(
                "query" to query,
                "results_count" to resultsCount.toString(),
                "query_length" to query.length.toString()
            )
            filterUsed?.let { params["filter_used"] = it }

            logEvent("search_performed", params)
        }
    }

    private suspend fun logEvent(eventName: String, parameters: Map<String, String>) {
        // TODO: Implement actual analytics logging
        // This could be Firebase Analytics, custom backend, or other service
        println("Analytics Event: $eventName - $parameters")
    }

    private suspend fun sendSessionSummary(
        session: TourSession, duration: Long, completionPercentage: Float, rating: Int?
    ) {
        // TODO: Send comprehensive session data to backend for analysis
        val summary = SessionSummary(
            tourId = session.tourId,
            tourTitle = session.tourTitle,
            isCreator = session.isCreator,
            startTime = session.startTime,
            duration = duration,
            visitedStops = session.visitedStops,
            pauseCount = session.pauseCount,
            totalPauseTime = session.totalPauseTime,
            completionPercentage = completionPercentage,
            rating = rating
        )

        // Send to backend API
        println("Session Summary: $summary")
    }
}

data class TourSession(
    val tourId: String,
    val tourTitle: String,
    val isCreator: Boolean,
    val startTime: Long,
    var pauseCount: Int = 0,
    var totalPauseTime: Long = 0L,
    var lastPauseTime: Long = 0L,
    val visitedStops: MutableList<VisitedStop> = mutableListOf()
)

data class VisitedStop(
    val stopId: String, val stopName: String, val visitTime: Long, val timeSpent: Long
)

data class SessionSummary(
    val tourId: String,
    val tourTitle: String,
    val isCreator: Boolean,
    val startTime: Long,
    val duration: Long,
    val visitedStops: List<VisitedStop>,
    val pauseCount: Int,
    val totalPauseTime: Long,
    val completionPercentage: Float,
    val rating: Int?
)