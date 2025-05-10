package com.xwurfel.tourry.feature.discovery.data.repository

import com.google.android.gms.maps.model.LatLng
import com.xwurfel.tourry.core.di.IoDispatcher
import com.xwurfel.tourry.core.network.interceptor.NoConnectivityException
import com.xwurfel.tourry.feature.discovery.api.TourDiscoveryApi
import com.xwurfel.tourry.feature.discovery.api.model.BookmarkRequestDto
import com.xwurfel.tourry.feature.discovery.api.model.TourDetailsDto
import com.xwurfel.tourry.feature.discovery.api.model.TourPreviewDto
import com.xwurfel.tourry.feature.discovery.domain.model.TourCategory
import com.xwurfel.tourry.feature.discovery.domain.model.TourDetails
import com.xwurfel.tourry.feature.discovery.domain.model.TourDifficulty
import com.xwurfel.tourry.feature.discovery.domain.model.TourFilter
import com.xwurfel.tourry.feature.discovery.domain.model.TourPreview
import com.xwurfel.tourry.feature.discovery.domain.repository.TourDiscoveryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TourDiscoveryRepositoryImpl @Inject constructor(
    private val api: TourDiscoveryApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TourDiscoveryRepository {

    private val _bookmarkedTours = MutableStateFlow<List<TourPreview>>(emptyList())

    override suspend fun searchTours(filter: TourFilter): Result<List<TourPreview>> =
        withContext(ioDispatcher) {
            try {
                val response = api.searchTours(
                    query = filter.query,
                    location = filter.location,
                    categories = filter.categories.takeIf { it.isNotEmpty() }
                        ?.joinToString(",") { it.name },
                    difficulties = filter.difficulties.takeIf { it.isNotEmpty() }
                        ?.joinToString(",") { it.name },
                    minDurationMinutes = filter.minDuration?.toMinutes()?.toInt(),
                    maxDurationMinutes = filter.maxDuration?.toMinutes()?.toInt(),
                    minRating = filter.minRating,
                    maxPrice = filter.maxPrice
                )

                if (response.isSuccessful) {
                    val tours = response.body()?.map { it.toDomainModel() } ?: emptyList()
                    Result.success(tours)
                } else {
                    Result.failure(Exception("Failed to search tours: ${response.code()} ${response.message()}"))
                }
            } catch (e: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Please check your network and try again."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTourDetails(tourId: String): Result<TourDetails> =
        withContext(ioDispatcher) {
            try {
                val response = api.getTourDetails(tourId)

                if (response.isSuccessful) {
                    val tourDetails = response.body()?.toDomainModel()
                        ?: return@withContext Result.failure(Exception("Tour details not found"))
                    Result.success(tourDetails)
                } else {
                    Result.failure(Exception("Failed to get tour details: ${response.code()} ${response.message()}"))
                }
            } catch (e: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Please check your network and try again."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getFeaturedTours(): Result<List<TourPreview>> = withContext(ioDispatcher) {
        try {
            val response = api.getFeaturedTours()

            if (response.isSuccessful) {
                val tours = response.body()?.map { it.toDomainModel() } ?: emptyList()
                Result.success(tours)
            } else {
                Result.failure(Exception("Failed to get featured tours: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPopularTours(): Result<List<TourPreview>> = withContext(ioDispatcher) {
        try {
            val response = api.getPopularTours()

            if (response.isSuccessful) {
                val tours = response.body()?.map { it.toDomainModel() } ?: emptyList()
                Result.success(tours)
            } else {
                Result.failure(Exception("Failed to get popular tours: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getNearbyTours(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Result<List<TourPreview>> = withContext(ioDispatcher) {
        try {
            val response = api.getNearbyTours(latitude, longitude, radiusKm)

            if (response.isSuccessful) {
                val tours = response.body()?.map { it.toDomainModel() } ?: emptyList()
                Result.success(tours)
            } else {
                Result.failure(Exception("Failed to get nearby tours: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBookmarkedTours(): Result<List<TourPreview>> =
        withContext(ioDispatcher) {
            try {
                val response = api.getBookmarkedTours()

                if (response.isSuccessful) {
                    val tours = response.body()?.map { it.toDomainModel() } ?: emptyList()
                    _bookmarkedTours.value = tours
                    Result.success(tours)
                } else {
                    Result.failure(Exception("Failed to get bookmarked tours: ${response.code()} ${response.message()}"))
                }
            } catch (e: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Please check your network and try again."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun bookmarkTour(tourId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = api.bookmarkTour(BookmarkRequestDto(tourId))

            if (response.isSuccessful) {
                // Update local state optimistically
                updateBookmarkState(tourId, true)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to bookmark tour: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeBookmark(tourId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = api.removeBookmark(tourId)

            if (response.isSuccessful) {
                // Update local state optimistically
                updateBookmarkState(tourId, false)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove bookmark: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeBookmarkedTours(): Flow<List<TourPreview>> {
        return _bookmarkedTours.asStateFlow()
    }

    private fun updateBookmarkState(tourId: String, isBookmarked: Boolean) {
        val currentList = _bookmarkedTours.value.toMutableList()

        if (isBookmarked) {
            // TODO: check this out
            // If we're adding a bookmark and it's not in our list, we'd need to fetch the full tour
            // For simplicity, we'll just refresh the full list later
        } else {
            currentList.removeIf { it.id == tourId }
            _bookmarkedTours.value = currentList
        }
    }

    private fun TourPreviewDto.toDomainModel(): TourPreview {
        return TourPreview(
            id = id,
            title = title,
            description = description,
            thumbnailUrl = thumbnailUrl,
            location = location,
            distance = distance,
            duration = Duration.ofMinutes(durationMinutes.toLong()),
            difficulty = TourDifficulty.valueOf(difficulty),
            category = TourCategory.valueOf(category),
            rating = rating,
            reviewCount = reviewCount,
            price = price,
            isBookmarked = isBookmarked
        )
    }

    private fun TourDetailsDto.toDomainModel(): TourDetails {
        val startPoint = if (startLatitude != null && startLongitude != null) {
            LatLng(startLatitude, startLongitude)
        } else {
            null
        }

        return TourDetails(
            id = id,
            title = title,
            description = description,
            creatorName = creatorName,
            creatorId = creatorId,
            thumbnailUrl = thumbnailUrl,
            imageUrls = imageUrls,
            location = location,
            startPoint = startPoint,
            distance = distance,
            duration = Duration.ofMinutes(durationMinutes.toLong()),
            difficulty = TourDifficulty.valueOf(difficulty),
            category = TourCategory.valueOf(category),
            rating = rating,
            reviewCount = reviewCount,
            price = price,
            isBookmarked = isBookmarked,
            waypointCount = waypointCount,
            highlights = highlights
        )
    }
}