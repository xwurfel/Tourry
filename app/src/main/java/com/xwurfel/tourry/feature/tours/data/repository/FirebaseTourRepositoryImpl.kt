package com.xwurfel.tourry.feature.tours.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.xwurfel.tourry.core.domain.error.DomainError
import com.xwurfel.tourry.core.domain.util.DomainResult
import com.xwurfel.tourry.core.domain.util.result
import com.xwurfel.tourry.feature.profile.data.model.FirestoreUser
import com.xwurfel.tourry.feature.tours.data.mapper.TourMapper
import com.xwurfel.tourry.feature.tours.data.model.FirestoreLiveLocation
import com.xwurfel.tourry.feature.tours.data.model.FirestoreTour
import com.xwurfel.tourry.feature.tours.data.model.FirestoreTourParticipation
import com.xwurfel.tourry.feature.tours.domain.model.CreateTourRequest
import com.xwurfel.tourry.feature.tours.domain.model.LiveParticipant
import com.xwurfel.tourry.feature.tours.domain.model.LiveTour
import com.xwurfel.tourry.feature.tours.domain.model.LiveTourStop
import com.xwurfel.tourry.feature.tours.domain.model.StopContent
import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.model.TourParticipation
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTourRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : TourRepository {

    companion object {
        private const val TOURS_COLLECTION = "tours"
        private const val PARTICIPATIONS_COLLECTION = "participations"
        private const val LIVE_LOCATIONS_COLLECTION = "live_locations"
        private const val USERS_COLLECTION = "users"
    }

    override fun observeAvailableTours(): Flow<List<Tour>> = callbackFlow {
        val listener = firestore.collection(TOURS_COLLECTION)
            .whereEqualTo("isActive", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing tours")
                    return@addSnapshotListener
                }

                val currentTime = System.currentTimeMillis()
                val tours = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<FirestoreTour>()?.copy(id = doc.id)?.let { firestoreTour ->
                        val startTime = firestoreTour.startTime.toDate().time
                        val isLiveSoon =
                            startTime - currentTime in 0..3600000 // 1 hour in milliseconds

                        TourMapper.toDomain(firestoreTour.copy(isLive = isLiveSoon))
                    }
                } ?: emptyList()

                trySend(tours)
            }

        awaitClose { listener.remove() }
    }

    override fun observeToursByAuthor(authorId: String): Flow<List<Tour>> = callbackFlow {
        val listener = firestore.collection(TOURS_COLLECTION)
            .whereEqualTo("authorId", authorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error observing tours by author")
                    return@addSnapshotListener
                }

                val tours = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<FirestoreTour>()?.copy(id = doc.id)?.let {
                        TourMapper.toDomain(it)
                    }
                } ?: emptyList()

                trySend(tours)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getTourById(tourId: String): DomainResult<Tour> = result {
        val doc = firestore.collection(TOURS_COLLECTION)
            .document(tourId)
            .get()
            .await()

        doc.toObject<FirestoreTour>()?.copy(id = doc.id)?.let {
            TourMapper.toDomain(it)
        } ?: throw Exception("Tour not found")
    }

    override suspend fun searchTours(
        query: String,
        themes: List<String>,
        maxPrice: Double?,
        maxDistance: Float?,
        userLocation: Pair<Double, Double>?
    ): DomainResult<List<Tour>> = result {
        // Note: Firestore has limited query capabilities
        // For production, consider using Algolia or Elasticsearch for advanced search
        var firestoreQuery = firestore.collection(TOURS_COLLECTION)
            .whereEqualTo("isActive", true)

        if (themes.isNotEmpty()) {
            firestoreQuery = firestoreQuery.whereIn("theme", themes)
        }

        if (maxPrice != null) {
            firestoreQuery = firestoreQuery.whereLessThanOrEqualTo("price", maxPrice)
        }

        val snapshot = firestoreQuery.get().await()
        val tours = snapshot.documents.mapNotNull { doc ->
            doc.toObject<FirestoreTour>()?.copy(id = doc.id)?.let {
                TourMapper.toDomain(it)
            }
        }

        // Client-side filtering for text search and distance
        tours.filter { tour ->
            val matchesText = query.isBlank() ||
                    tour.title.contains(query, ignoreCase = true) ||
                    tour.description.contains(query, ignoreCase = true) ||
                    tour.tags.any { it.contains(query, ignoreCase = true) }

            val withinDistance = maxDistance == null || userLocation == null ||
                    // Simple distance calculation - in production use proper geospatial queries
                    tour.distance <= maxDistance

            matchesText && withinDistance
        }
    }

    override suspend fun createTour(request: CreateTourRequest): DomainResult<String> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        // Get user profile for author info
        val userDoc = firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .get()
            .await()

        val userProfile = userDoc.toObject<FirestoreUser>() ?: FirestoreUser(
            id = currentUser.uid,
            name = currentUser.displayName ?: "Unknown",
            email = currentUser.email ?: "",
            avatarUrl = currentUser.photoUrl?.toString()
        )

        val firestoreTour = TourMapper.toFirestore(
            createRequest = request,
            authorId = currentUser.uid,
            authorName = userProfile.name,
            authorAvatarUrl = userProfile.avatarUrl
        )

        val docRef = firestore.collection(TOURS_COLLECTION).add(firestoreTour).await()
        docRef.id
    }

    override suspend fun updateTour(
        tourId: String,
        request: CreateTourRequest
    ): DomainResult<Unit> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        // Verify user owns this tour
        val tourDoc = firestore.collection(TOURS_COLLECTION).document(tourId).get().await()
        val tour = tourDoc.toObject<FirestoreTour>()

        if (tour?.authorId != currentUser.uid) {
            throw Exception("Unauthorized to update this tour")
        }

        val updates = mapOf(
            "title" to request.title,
            "description" to request.description,
            "theme" to request.theme,
            "coverImageUrl" to request.coverImageUrl,
            "price" to request.price,
            "startTime" to com.google.firebase.Timestamp(java.util.Date(request.startTime)),
            "duration" to request.duration,
            "maxParticipants" to request.maxParticipants,
            "stops" to request.stops.map { TourMapper.toFirestore(it) },
            "tags" to request.tags,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection(TOURS_COLLECTION).document(tourId).update(updates).await()
    }

    override suspend fun deleteTour(tourId: String): DomainResult<Unit> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        // Verify user owns this tour
        val tourDoc = firestore.collection(TOURS_COLLECTION).document(tourId).get().await()
        val tour = tourDoc.toObject<FirestoreTour>()

        if (tour?.authorId != currentUser.uid) {
            throw Exception("Unauthorized to delete this tour")
        }

        // Soft delete by setting isActive to false
        firestore.collection(TOURS_COLLECTION).document(tourId)
            .update("isActive", false, "updatedAt", com.google.firebase.Timestamp.now())
            .await()
    }

    override suspend fun setTourLiveStatus(tourId: String, isLive: Boolean): DomainResult<Unit> =
        result {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

            // Verify user owns this tour
            val tourDoc = firestore.collection(TOURS_COLLECTION).document(tourId).get().await()
            val tour = tourDoc.toObject<FirestoreTour>()

            if (tour?.authorId != currentUser.uid) {
                throw Exception("Unauthorized to modify this tour")
            }

            firestore.collection(TOURS_COLLECTION).document(tourId)
                .update("isLive", isLive, "updatedAt", com.google.firebase.Timestamp.now())
                .await()
        }

    override suspend fun joinTour(tourId: String): DomainResult<Unit> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        // Check if already joined
        val existingParticipation = firestore.collection(PARTICIPATIONS_COLLECTION)
            .whereEqualTo("tourId", tourId)
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("status", "joined")
            .get()
            .await()

        if (!existingParticipation.isEmpty) {
            throw Exception("Already joined this tour")
        }

        // Get user profile
        val userDoc = firestore.collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .get()
            .await()

        val userProfile = userDoc.toObject<FirestoreUser>() ?: FirestoreUser(
            id = currentUser.uid,
            name = currentUser.displayName ?: "Unknown",
            email = currentUser.email ?: "",
            avatarUrl = currentUser.photoUrl?.toString()
        )

        // Create participation record
        val participation = FirestoreTourParticipation(
            tourId = tourId,
            userId = currentUser.uid,
            userName = userProfile.name,
            userAvatarUrl = userProfile.avatarUrl,
            status = "joined"
        )

        // Use transaction to ensure consistency
        firestore.runTransaction { transaction ->
            val tourRef = firestore.collection(TOURS_COLLECTION).document(tourId)
            val tourSnapshot = transaction.get(tourRef)
            val tour = tourSnapshot.toObject<FirestoreTour>()

            if (tour == null) {
                throw Exception("Tour not found")
            }

            if (tour.maxParticipants != null && tour.currentParticipants >= tour.maxParticipants) {
                throw Exception("Tour is full")
            }

            // Add participation
            val participationRef = firestore.collection(PARTICIPATIONS_COLLECTION).document()
            transaction.set(participationRef, participation.copy(id = participationRef.id))

            // Update participant count
            transaction.update(tourRef, "currentParticipants", tour.currentParticipants + 1)
        }.await()
    }

    override suspend fun leaveTour(tourId: String): DomainResult<Unit> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        val participationQuery = firestore.collection(PARTICIPATIONS_COLLECTION)
            .whereEqualTo("tourId", tourId)
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("status", "joined")
            .get()
            .await()

        if (participationQuery.isEmpty) {
            throw Exception("Not currently joined to this tour")
        }

        // Use transaction to ensure consistency
        firestore.runTransaction { transaction ->
            val participationDoc = participationQuery.documents.first()
            val tourRef = firestore.collection(TOURS_COLLECTION).document(tourId)
            val tourSnapshot = transaction.get(tourRef)
            val tour = tourSnapshot.toObject<FirestoreTour>()

            if (tour != null) {
                // Update participation status
                transaction.update(
                    participationDoc.reference,
                    "status", "cancelled",
                    "updatedAt", com.google.firebase.Timestamp.now()
                )

                // Update participant count
                transaction.update(
                    tourRef, "currentParticipants",
                    maxOf(0, tour.currentParticipants - 1)
                )
            }
        }.await()
    }

    override fun observeUserParticipations(userId: String): Flow<List<TourParticipation>> =
        callbackFlow {
            val listener = firestore.collection(PARTICIPATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("joinedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Error observing user participations")
                        return@addSnapshotListener
                    }

                    val participations = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<FirestoreTourParticipation>()?.copy(id = doc.id)?.let {
                            TourMapper.toDomain(it)
                        }
                    } ?: emptyList()

                    trySend(participations)
                }

            awaitClose { listener.remove() }
        }

    override fun observeTourParticipations(tourId: String): Flow<List<TourParticipation>> =
        callbackFlow {
            val listener = firestore.collection(PARTICIPATIONS_COLLECTION)
                .whereEqualTo("tourId", tourId)
                .orderBy("joinedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Error observing tour participations")
                        return@addSnapshotListener
                    }

                    val participations = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<FirestoreTourParticipation>()?.copy(id = doc.id)?.let {
                            TourMapper.toDomain(it)
                        }
                    } ?: emptyList()

                    trySend(participations)
                }

            awaitClose { listener.remove() }
        }

    override suspend fun submitReview(
        tourId: String,
        rating: Int,
        review: String,
        completionPercentage: Float
    ): DomainResult<Unit> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        // Find user's participation
        val participationQuery = firestore.collection(PARTICIPATIONS_COLLECTION)
            .whereEqualTo("tourId", tourId)
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .await()

        if (participationQuery.isEmpty) {
            throw Exception("Must join tour before reviewing")
        }

        val participationDoc = participationQuery.documents.first()

        // Update participation with review
        participationDoc.reference.update(
            mapOf(
                "rating" to rating,
                "review" to review,
                "completionPercentage" to completionPercentage,
                "reviewedAt" to com.google.firebase.Timestamp.now(),
                "status" to "completed"
            )
        ).await()

        // Update tour's overall rating (simplified calculation)
        updateTourRating(tourId)
    }

    private suspend fun updateTourRating(tourId: String) {
        val participations = firestore.collection(PARTICIPATIONS_COLLECTION)
            .whereEqualTo("tourId", tourId)
            .whereNotEqualTo("rating", null)
            .get()
            .await()

        val ratings = participations.documents.mapNotNull { doc ->
            doc.getLong("rating")?.toInt()
        }

        if (ratings.isNotEmpty()) {
            val averageRating = ratings.average().toFloat()
            val reviewsCount = ratings.size

            firestore.collection(TOURS_COLLECTION).document(tourId)
                .update(
                    mapOf(
                        "rating" to averageRating,
                        "reviewsCount" to reviewsCount,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                ).await()
        }
    }

    override suspend fun updateUserLocation(
        tourId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        currentStopId: String?
    ): DomainResult<Unit> = result {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        val liveLocation = FirestoreLiveLocation(
            userId = currentUser.uid,
            tourId = tourId,
            location = com.google.firebase.firestore.GeoPoint(latitude, longitude),
            accuracy = accuracy,
            currentStopId = currentStopId,
            timestamp = com.google.firebase.Timestamp.now()
        )

        // Use userId as document ID to ensure one location per user per tour
        firestore.collection(LIVE_LOCATIONS_COLLECTION)
            .document("${tourId}_${currentUser.uid}")
            .set(liveLocation)
            .await()
    }

    override fun observeLiveParticipants(tourId: String): Flow<List<LiveParticipant>> =
        callbackFlow {
            val listener = firestore.collection(LIVE_LOCATIONS_COLLECTION)
                .whereEqualTo("tourId", tourId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Error observing live participants")
                        return@addSnapshotListener
                    }

                    val participants = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<FirestoreLiveLocation>()?.let { location ->
                            LiveParticipant(
                                userId = location.userId,
                                userName = "User", // Would need to join with users collection
                                userAvatarUrl = null,
                                latitude = location.location.latitude,
                                longitude = location.location.longitude,
                                accuracy = location.accuracy,
                                lastUpdate = location.timestamp.toDate().time,
                                currentStopId = location.currentStopId
                            )
                        }
                    } ?: emptyList()

                    trySend(participants)
                }

            awaitClose { listener.remove() }
        }

    override suspend fun getLiveTour(tourId: String): DomainResult<LiveTour> {
        return try {
            delay(500) // Simulate network call

            // Mock data for now - replace with Firebase calls later
            val mockTour = LiveTour(
                id = tourId,
                title = "Amazing City Tour",
                description = "Discover the hidden gems of our beautiful city",
                authorId = "author_123",
                authorName = "Local Guide",
                stops = listOf(
                    LiveTourStop(
                        id = "stop_1",
                        name = "City Center",
                        description = "The heart of our beautiful city",
                        latitude = 48.8566,
                        longitude = 2.3522,
                        order = 1,
                        content = StopContent(
                            text = "Welcome to the city center!",
                            imageUrls = emptyList(),
                            audioUrl = "https://example.com/audio1.mp3"
                        )
                    ),
                    LiveTourStop(
                        id = "stop_2",
                        name = "Historic Square",
                        description = "A place full of history",
                        latitude = 48.8576,
                        longitude = 2.3532,
                        order = 2,
                        content = StopContent(
                            text = "This historic square has been here for centuries.",
                            imageUrls = emptyList(),
                            audioUrl = "https://example.com/audio2.mp3"
                        )
                    )
                )
            )

            DomainResult.Success(mockTour)
        } catch (_: Exception) {
            DomainResult.Failure(DomainError.SomethingWentWrongError())
        }
    }

    override suspend fun startTourSession(tourId: String, userId: String): DomainResult<String> {
        return try {
            delay(300)
            val sessionId = "session_${System.currentTimeMillis()}"
            DomainResult.Success(sessionId)
        } catch (_: Exception) {
            DomainResult.Failure(DomainError.SomethingWentWrongError())
        }
    }

    override suspend fun recordStopVisit(
        sessionId: String,
        stopId: String,
        timestamp: Long,
        userLocation: Pair<Double, Double>
    ): DomainResult<Unit> {
        return try {
            delay(100)
            // In Firebase: save to tour_sessions/{sessionId}/visits/{stopId}
            DomainResult.Success(Unit)
        } catch (_: Exception) {
            DomainResult.Failure(DomainError.SomethingWentWrongError())
        }
    }

    override suspend fun completeTourSession(
        sessionId: String,
        completionPercentage: Float,
        totalDuration: Long
    ): DomainResult<Unit> {
        return try {
            delay(200)
            // In Firebase: update tour_sessions/{sessionId} with completion data
            DomainResult.Success(Unit)
        } catch (_: Exception) {
            DomainResult.Failure(DomainError.SomethingWentWrongError())
        }
    }
}