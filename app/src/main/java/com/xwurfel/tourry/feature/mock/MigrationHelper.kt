package com.xwurfel.tourry.feature.mock

import com.xwurfel.tourry.core.domain.util.onFailure
import com.xwurfel.tourry.core.domain.util.onSuccess
import com.xwurfel.tourry.feature.tours.domain.model.CreateTourRequest
import com.xwurfel.tourry.feature.tours.domain.model.CreateTourStop
import com.xwurfel.tourry.feature.tours.domain.repository.TourRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to migrate data from MockDataManager to Firebase during development
 * This is optional and only for development/testing purposes
 */
@Singleton
class MigrationHelper @Inject constructor(
    private val mockDataManager: MockDataManager,
    private val tourRepository: TourRepository
) {

    /**
     * Migrates mock tours to Firebase (for development/testing)
     * Call this once during development to populate Firebase with test data
     */
    suspend fun migrateMockToursToFirebase(): Result<Unit> {
        return try {
            Timber.d("Starting migration of mock tours to Firebase...")

            // Get all tours from mock data manager
            val mockTours = mockDataManager.availableTours.value

            var successCount = 0
            var errorCount = 0

            mockTours.forEach { tourPreview ->
                // Convert TourPreview to CreateTourRequest
                val tourDetail = mockDataManager.getTourDetail(tourPreview.id)

                if (tourDetail != null) {
                    val createRequest = CreateTourRequest(
                        title = tourDetail.title,
                        description = tourDetail.description,
                        theme = tourDetail.theme,
                        coverImageUrl = tourDetail.coverImageUrl,
                        price = tourDetail.price,
                        startTime = tourDetail.startTime,
                        duration = tourDetail.duration,
                        maxParticipants = null, // Mock doesn't have this
                        stops = tourDetail.stops.map { stop ->
                            CreateTourStop(
                                name = stop.name,
                                description = stop.description,
                                latitude = stop.latitude,
                                longitude = stop.longitude,
                                order = stop.order
                            )
                        },
                        tags = listOf(tourDetail.theme.lowercase()) // Simple tag generation
                    )

                    tourRepository.createTour(createRequest)
                        .onSuccess { tourId ->
                            Timber.d("Successfully migrated tour: ${tourDetail.title} (ID: $tourId)")
                            successCount++
                        }
                        .onFailure { error ->
                            Timber.e("Failed to migrate tour: ${tourDetail.title} - ${error.msg}")
                            errorCount++
                        }
                } else {
                    Timber.w("Could not get detail for tour: ${tourPreview.id}")
                    errorCount++
                }
            }

            Timber.d("Migration completed: $successCount successful, $errorCount failed")

            if (errorCount == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Migration completed with $errorCount errors"))
            }

        } catch (e: Exception) {
            Timber.e(e, "Migration failed")
            Result.failure(e)
        }
    }

    /**
     * Compares data between mock and Firebase to verify migration
     */
    suspend fun verifyMigration(): Result<String> {
        return try {
            val mockTours = mockDataManager.availableTours.value

            tourRepository.searchTours(
                query = "",
                themes = emptyList(),
                maxPrice = null,
                maxDistance = null,
                userLocation = null
            ).onSuccess { firebaseTours ->
                val report = buildString {
                    appendLine("Migration Verification Report")
                    appendLine("==================================")
                    appendLine("Mock tours count: ${mockTours.size}")
                    appendLine("Firebase tours count: ${firebaseTours.size}")
                    appendLine()

                    if (mockTours.size == firebaseTours.size) {
                        appendLine("✅ Tour counts match!")
                    } else {
                        appendLine("❌ Tour counts don't match!")
                    }

                    appendLine()
                    appendLine("Mock Tours:")
                    mockTours.forEach { tour ->
                        appendLine("- ${tour.title} (${tour.id})")
                    }

                    appendLine()
                    appendLine("Firebase Tours:")
                    firebaseTours.forEach { tour ->
                        appendLine("- ${tour.title} (${tour.id})")
                    }
                }

                Timber.d(report)
                Result.success(report)
            }.onFailure { error ->
                Result.failure<String>(Exception("Failed to verify migration: ${error.msg}"))
            }

        } catch (e: Exception) {
            Timber.e(e, "Verification failed")
            Result.failure<String>(e)
        } as Result<String>
    }
}