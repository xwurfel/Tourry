package com.xwurfel.tourry.feature.tourbuilder.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.xwurfel.tourry.core.di.IoDispatcher
import com.xwurfel.tourry.core.network.interceptor.NoConnectivityException
import com.xwurfel.tourry.feature.tourbuilder.api.TourBuilderApi
import com.xwurfel.tourry.feature.tourbuilder.api.model.ContentDraftDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.LatLngDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.TourDraftDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.WaypointDraftDto
import com.xwurfel.tourry.feature.tourbuilder.api.model.WaypointOrderDto
import com.xwurfel.tourry.feature.tourbuilder.domain.model.ContentDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.model.ContentType
import com.xwurfel.tourry.feature.tourbuilder.domain.model.TourDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.model.WaypointDraft
import com.xwurfel.tourry.feature.tourbuilder.domain.repository.RouteInfo
import com.xwurfel.tourry.feature.tourbuilder.domain.repository.TourBuilderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tourDraftDataStore: DataStore<Preferences> by preferencesDataStore(name = "tour_draft")

@Singleton
class TourBuilderRepositoryImpl @Inject constructor(
    private val api: TourBuilderApi,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val gson: Gson
) : TourBuilderRepository {

    companion object {
        private val DRAFT_KEY = stringPreferencesKey("current_tour_draft")
    }

    override suspend fun createTour(tourDraft: TourDraft): Result<TourDraft> =
        withContext(ioDispatcher) {
            try {
                val response = api.createTour(tourDraft.toDto())

                if (response.isSuccessful) {
                    val createdTour = response.body()?.toDomainModel()
                        ?: return@withContext Result.failure(Exception("Failed to create tour: Response body is null"))
                    Result.success(createdTour)
                } else {
                    Result.failure(Exception("Failed to create tour: ${response.code()} ${response.message()}"))
                }
            } catch (e: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Your draft has been saved locally."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun updateTour(tourDraft: TourDraft): Result<TourDraft> =
        withContext(ioDispatcher) {
            if (tourDraft.id == null) {
                return@withContext Result.failure(Exception("Cannot update tour without ID"))
            }

            try {
                val response = api.updateTour(tourDraft.id, tourDraft.toDto())

                if (response.isSuccessful) {
                    val updatedTour = response.body()?.toDomainModel()
                        ?: return@withContext Result.failure(Exception("Failed to update tour: Response body is null"))
                    Result.success(updatedTour)
                } else {
                    Result.failure(Exception("Failed to update tour: ${response.code()} ${response.message()}"))
                }
            } catch (e: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Your changes have been saved locally."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTourDraft(tourId: String): Result<TourDraft> =
        withContext(ioDispatcher) {
            try {
                val response = api.getTourDraft(tourId)

                if (response.isSuccessful) {
                    val tourDraft = response.body()?.toDomainModel()
                        ?: return@withContext Result.failure(Exception("Failed to get tour: Response body is null"))
                    Result.success(tourDraft)
                } else {
                    Result.failure(Exception("Failed to get tour: ${response.code()} ${response.message()}"))
                }
            } catch (e: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Please check your network and try again."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getMySavedTours(): Result<List<TourDraft>> = withContext(ioDispatcher) {
        try {
            val response = api.getMySavedTours()

            if (response.isSuccessful) {
                val tours = response.body()?.map { it.toDomainModel() } ?: emptyList()
                Result.success(tours)
            } else {
                Result.failure(Exception("Failed to get saved tours: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTour(tourId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = api.deleteTour(tourId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete tour: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addWaypoint(
        tourId: String,
        waypoint: WaypointDraft
    ): Result<WaypointDraft> = withContext(ioDispatcher) {
        try {
            val response = api.addWaypoint(tourId, waypoint.toDto())

            if (response.isSuccessful) {
                val createdWaypoint = response.body()?.toDomainModel()
                    ?: return@withContext Result.failure(Exception("Failed to add waypoint: Response body is null"))
                Result.success(createdWaypoint)
            } else {
                Result.failure(Exception("Failed to add waypoint: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Your changes have been saved locally."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWaypoint(
        tourId: String,
        waypoint: WaypointDraft
    ): Result<WaypointDraft> = withContext(ioDispatcher) {
        if (waypoint.id == null) {
            return@withContext Result.failure(Exception("Cannot update waypoint without ID"))
        }

        try {
            val response = api.updateWaypoint(tourId, waypoint.id, waypoint.toDto())

            if (response.isSuccessful) {
                val updatedWaypoint = response.body()?.toDomainModel()
                    ?: return@withContext Result.failure(Exception("Failed to update waypoint: Response body is null"))
                Result.success(updatedWaypoint)
            } else {
                Result.failure(Exception("Failed to update waypoint: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Your changes have been saved locally."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWaypoint(tourId: String, waypointId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                val response = api.deleteWaypoint(tourId, waypointId)

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete waypoint: ${response.code()} ${response.message()}"))
                }
            } catch (e: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Please check your network and try again."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun reorderWaypoints(
        tourId: String,
        waypointIds: List<String>
    ): Result<List<WaypointDraft>> = withContext(ioDispatcher) {
        try {
            val orderRequest = WaypointOrderDto(waypointIds)
            val response = api.reorderWaypoints(tourId, orderRequest)

            if (response.isSuccessful) {
                val waypoints = response.body()?.map { it.toDomainModel() } ?: emptyList()
                Result.success(waypoints)
            } else {
                Result.failure(Exception("Failed to reorder waypoints: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Your changes have been saved locally."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addContent(
        tourId: String,
        waypointId: String,
        content: ContentDraft
    ): Result<ContentDraft> = withContext(ioDispatcher) {
        try {
            val response = api.addContent(tourId, waypointId, content.toDto())

            if (response.isSuccessful) {
                val createdContent = response.body()?.toDomainModel()
                    ?: return@withContext Result.failure(Exception("Failed to add content: Response body is null"))
                Result.success(createdContent)
            } else {
                Result.failure(Exception("Failed to add content: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Your changes have been saved locally."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateContent(
        tourId: String,
        waypointId: String,
        content: ContentDraft
    ): Result<ContentDraft> = withContext(ioDispatcher) {
        if (content.id == null) {
            return@withContext Result.failure(Exception("Cannot update content without ID"))
        }

        try {
            val response = api.updateContent(tourId, waypointId, content.id, content.toDto())

            if (response.isSuccessful) {
                val updatedContent = response.body()?.toDomainModel()
                    ?: return@withContext Result.failure(Exception("Failed to update content: Response body is null"))
                Result.success(updatedContent)
            } else {
                Result.failure(Exception("Failed to update content: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Your changes have been saved locally."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContent(
        tourId: String,
        waypointId: String,
        contentId: String
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = api.deleteContent(tourId, waypointId, contentId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete content: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadImage(file: File): Result<String> = withContext(ioDispatcher) {
        try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = api.uploadImage(body)

            if (response.isSuccessful) {
                val url = response.body()?.url
                    ?: return@withContext Result.failure(Exception("Failed to upload image: Response body is null"))
                Result.success(url)
            } else {
                Result.failure(Exception("Failed to upload image: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadAudio(file: File): Result<String> = withContext(ioDispatcher) {
        try {
            val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = api.uploadAudio(body)

            if (response.isSuccessful) {
                val url = response.body()?.url
                    ?: return@withContext Result.failure(Exception("Failed to upload audio: Response body is null"))
                Result.success(url)
            } else {
                Result.failure(Exception("Failed to upload audio: ${response.code()} ${response.message()}"))
            }
        } catch (e: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateRoute(waypoints: List<LatLng>): Result<RouteInfo> =
        withContext(ioDispatcher) {
            if (waypoints.size < 2) {
                return@withContext Result.failure(Exception("At least two waypoints are required to calculate a route"))
            }

            try {
                val waypointDtos = waypoints.map { LatLngDto(it.latitude, it.longitude) }
                val response = api.calculateRoute(waypointDtos)

                if (response.isSuccessful) {
                    val routeInfo = response.body()?.let {
                        RouteInfo(
                            distance = it.distance,
                            duration = Duration.ofMinutes(it.durationMinutes.toLong()),
                            path = it.path.map { point -> LatLng(point.latitude, point.longitude) }
                        )
                    }
                        ?: return@withContext Result.failure(Exception("Failed to calculate route: Response body is null"))
                    Result.success(routeInfo)
                } else {
                    Result.failure(Exception("Failed to calculate route: ${response.code()} ${response.message()}"))
                }
            } catch (e: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Please check your network and try again."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun getDraftInProgress(): Flow<TourDraft?> {
        return context.tourDraftDataStore.data.map { preferences ->
            val draftJson = preferences[DRAFT_KEY]
            if (draftJson != null) {
                try {
                    gson.fromJson(draftJson, TourDraft::class.java)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    override suspend fun saveDraftLocally(tourDraft: TourDraft) {
        context.tourDraftDataStore.edit { preferences ->
            preferences[DRAFT_KEY] = gson.toJson(tourDraft)
        }
    }

    override suspend fun clearLocalDraft() {
        context.tourDraftDataStore.edit { preferences ->
            preferences.remove(DRAFT_KEY)
        }
    }

    // Extension functions to convert between domain models and DTOs
    private fun TourDraft.toDto(): TourDraftDto {
        return TourDraftDto(
            id = id,
            title = title,
            description = description,
            location = location,
            category = category.name,
            difficulty = difficulty.name,
            price = price,
            isPublic = isPublic,
            waypoints = waypoints.map { it.toDto() },
            thumbnailUrl = thumbnailUrl,
            imageUrls = imageUrls,
            highlights = highlights,
            estimatedDurationMinutes = estimatedDuration.toMinutes().toInt()
        )
    }

    private fun TourDraftDto.toDomainModel(): TourDraft {
        return TourDraft(
            id = id,
            title = title,
            description = description,
            location = location,
            category = try {
                com.xwurfel.tourry.feature.discovery.domain.model.TourCategory.valueOf(category)
            } catch (e: Exception) {
                com.xwurfel.tourry.feature.discovery.domain.model.TourCategory.CULTURAL
            },
            difficulty = try {
                com.xwurfel.tourry.feature.discovery.domain.model.TourDifficulty.valueOf(difficulty)
            } catch (e: Exception) {
                com.xwurfel.tourry.feature.discovery.domain.model.TourDifficulty.MODERATE
            },
            price = price,
            isPublic = isPublic,
            waypoints = waypoints.map { it.toDomainModel() },
            thumbnailUrl = thumbnailUrl,
            imageUrls = imageUrls,
            highlights = highlights,
            estimatedDuration = Duration.ofMinutes(estimatedDurationMinutes.toLong())
        )
    }

    private fun WaypointDraft.toDto(): WaypointDraftDto {
        return WaypointDraftDto(
            id = id,
            title = title,
            description = description,
            latitude = position.latitude,
            longitude = position.longitude,
            order = order,
            durationMinutes = durationMinutes,
            geofenceRadius = geofenceRadius,
            contents = contents.map { it.toDto() }
        )
    }

    private fun WaypointDraftDto.toDomainModel(): WaypointDraft {
        return WaypointDraft(
            id = id,
            title = title,
            description = description,
            position = LatLng(latitude, longitude),
            order = order,
            durationMinutes = durationMinutes,
            geofenceRadius = geofenceRadius,
            contents = contents.map { it.toDomainModel() }
        )
    }

    private fun ContentDraft.toDto(): ContentDraftDto {
        return ContentDraftDto(
            id = id,
            title = title,
            description = description,
            type = type.name,
            mediaUrl = mediaUrl,
            order = order
        )
    }

    private fun ContentDraftDto.toDomainModel(): ContentDraft {
        return ContentDraft(
            id = id,
            title = title,
            description = description,
            type = try {
                ContentType.valueOf(type)
            } catch (e: Exception) {
                ContentType.TEXT
            },
            mediaUrl = mediaUrl,
            order = order
        )
    }
}