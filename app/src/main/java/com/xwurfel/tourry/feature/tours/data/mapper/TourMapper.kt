package com.xwurfel.tourry.feature.tours.data.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.xwurfel.tourry.feature.tours.data.model.FirestoreStopContent
import com.xwurfel.tourry.feature.tours.data.model.FirestoreTour
import com.xwurfel.tourry.feature.tours.data.model.FirestoreTourParticipation
import com.xwurfel.tourry.feature.tours.data.model.FirestoreTourStop
import com.xwurfel.tourry.feature.tours.domain.model.CreateStopContent
import com.xwurfel.tourry.feature.tours.domain.model.CreateTourRequest
import com.xwurfel.tourry.feature.tours.domain.model.CreateTourStop
import com.xwurfel.tourry.feature.tours.domain.model.ParticipationStatus
import com.xwurfel.tourry.feature.tours.domain.model.StopContent
import com.xwurfel.tourry.feature.tours.domain.model.Tour
import com.xwurfel.tourry.feature.tours.domain.model.TourAuthor
import com.xwurfel.tourry.feature.tours.domain.model.TourParticipation
import com.xwurfel.tourry.feature.tours.domain.model.TourStop

object TourMapper {

    fun toDomain(firestoreTour: FirestoreTour): Tour {
        return Tour(
            id = firestoreTour.id,
            title = firestoreTour.title,
            description = firestoreTour.description,
            theme = firestoreTour.theme,
            coverImageUrl = firestoreTour.coverImageUrl,
            author = TourAuthor(
                id = firestoreTour.authorId,
                name = firestoreTour.authorName,
                avatarUrl = firestoreTour.authorAvatarUrl
            ),
            price = firestoreTour.price,
            currency = firestoreTour.currency,
            startTime = firestoreTour.startTime.toDate().time,
            duration = firestoreTour.duration,
            maxParticipants = firestoreTour.maxParticipants,
            currentParticipants = firestoreTour.currentParticipants,
            rating = firestoreTour.rating,
            reviewsCount = firestoreTour.reviewsCount,
            isActive = firestoreTour.isActive,
            stops = firestoreTour.stops.map { toDomain(it) },
            tags = firestoreTour.tags,
            createdAt = firestoreTour.createdAt.toDate().time,
            updatedAt = firestoreTour.updatedAt.toDate().time,
            isManuallyStarted = firestoreTour.isManuallyStarted,
            manuallyStartedAt = firestoreTour.manuallyStartedAt?.toDate()?.time,
            manuallyStartedBy = firestoreTour.manuallyStartedBy,
            isCompleted = firestoreTour.isCompleted,
            completedAt = firestoreTour.completedAt?.toDate()?.time,
            isJoined = false,
            spotsLeft = null
        )
    }

    fun toFirestore(
        createRequest: CreateTourRequest,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String?
    ): FirestoreTour {
        return FirestoreTour(
            id = "", // Will be set by Firestore
            title = createRequest.title,
            description = createRequest.description,
            theme = createRequest.theme,
            coverImageUrl = createRequest.coverImageUrl,
            authorId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            price = createRequest.price,
            startTime = Timestamp(java.util.Date(createRequest.startTime)),
            duration = createRequest.duration,
            maxParticipants = createRequest.maxParticipants,
            stops = createRequest.stops.map { toFirestore(it) },
            tags = createRequest.tags,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
        )
    }

    private fun toDomain(firestoreStop: FirestoreTourStop): TourStop {
        return TourStop(
            id = firestoreStop.id,
            name = firestoreStop.name,
            description = firestoreStop.description,
            latitude = firestoreStop.location.latitude,
            longitude = firestoreStop.location.longitude,
            order = firestoreStop.order,
            content = firestoreStop.content?.let { toDomain(it) }
        )
    }

    fun toFirestore(createStop: CreateTourStop): FirestoreTourStop {
        return FirestoreTourStop(
            id = java.util.UUID.randomUUID().toString(),
            name = createStop.name,
            description = createStop.description,
            location = GeoPoint(createStop.latitude, createStop.longitude),
            order = createStop.order,
            content = createStop.content?.let { toFirestore(it) }
        )
    }

    private fun toDomain(firestoreContent: FirestoreStopContent): StopContent {
        return StopContent(
            text = firestoreContent.text,
            imageUrls = firestoreContent.imageUrls,
            audioUrl = firestoreContent.audioUrl,
            videoUrl = firestoreContent.videoUrl
        )
    }

    private fun toFirestore(createContent: CreateStopContent): FirestoreStopContent {
        return FirestoreStopContent(
            text = createContent.text,
            imageUrls = createContent.imageUrls,
            audioUrl = createContent.audioUrl,
            videoUrl = createContent.videoUrl
        )
    }

    fun toDomain(firestoreParticipation: FirestoreTourParticipation): TourParticipation {
        return TourParticipation(
            id = firestoreParticipation.id,
            tourId = firestoreParticipation.tourId,
            userId = firestoreParticipation.userId,
            userName = firestoreParticipation.userName,
            userAvatarUrl = firestoreParticipation.userAvatarUrl,
            joinedAt = firestoreParticipation.joinedAt.toDate().time,
            status = when (firestoreParticipation.status) {
                "completed" -> ParticipationStatus.COMPLETED
                "cancelled" -> ParticipationStatus.CANCELLED
                else -> ParticipationStatus.JOINED
            },
            completionPercentage = firestoreParticipation.completionPercentage,
            rating = firestoreParticipation.rating,
            review = firestoreParticipation.review,
            reviewedAt = firestoreParticipation.reviewedAt?.toDate()?.time
        )
    }
}