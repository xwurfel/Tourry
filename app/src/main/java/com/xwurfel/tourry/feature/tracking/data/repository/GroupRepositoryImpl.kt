package com.xwurfel.tourry.feature.tracking.data.repository

import com.xwurfel.tourry.core.di.IoDispatcher
import com.xwurfel.tourry.core.network.interceptor.NoConnectivityException
import com.xwurfel.tourry.feature.tracking.api.GroupApi
import com.xwurfel.tourry.feature.tracking.api.model.CreateGroupRequest
import com.xwurfel.tourry.feature.tracking.api.model.GroupDto
import com.xwurfel.tourry.feature.tracking.api.model.GroupMemberStatusUpdateRequest
import com.xwurfel.tourry.feature.tracking.api.model.JoinGroupRequest
import com.xwurfel.tourry.feature.tracking.api.model.UpdateGroupStatusRequest
import com.xwurfel.tourry.feature.tracking.domain.model.GroupMember
import com.xwurfel.tourry.feature.tracking.domain.model.GroupRole
import com.xwurfel.tourry.feature.tracking.domain.model.GroupStatus
import com.xwurfel.tourry.feature.tracking.domain.model.MemberLocation
import com.xwurfel.tourry.feature.tracking.domain.model.MemberStatus
import com.xwurfel.tourry.feature.tracking.domain.model.TourGroup
import com.xwurfel.tourry.feature.tracking.domain.repository.GroupRepository
import com.xwurfel.tourry.feature.tracking.firebase.LocationSyncManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupApi: GroupApi,
    private val locationSyncManager: LocationSyncManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GroupRepository {

    private val dateFormatter = DateTimeFormatter.ISO_INSTANT

    override suspend fun getGroup(groupId: String): Result<TourGroup> = withContext(ioDispatcher) {
        try {
            val response = groupApi.getGroup(groupId)
            if (response.isSuccessful) {
                val groupDto = response.body()
                    ?: return@withContext Result.failure(Exception("Group not found"))
                Result.success(groupDto.toDomain())
            } else {
                Result.failure(Exception("Failed to get group: ${response.code()} ${response.message()}"))
            }
        } catch (_: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createGroup(tourId: String, name: String): Result<TourGroup> =
        withContext(ioDispatcher) {
            try {
                val request = CreateGroupRequest(tourId = tourId, name = name)
                val response = groupApi.createGroup(request)

                if (response.isSuccessful) {
                    val groupDto = response.body()
                        ?: return@withContext Result.failure(Exception("Failed to create group: Response body is null"))
                    Result.success(groupDto.toDomain())
                } else {
                    Result.failure(Exception("Failed to create group: ${response.code()} ${response.message()}"))
                }
            } catch (_: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Please check your network and try again."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun joinGroup(joinCode: String): Result<TourGroup> =
        withContext(ioDispatcher) {
            try {
                val request = JoinGroupRequest(joinCode = joinCode)
                val response = groupApi.joinGroupByCode(request)

                if (response.isSuccessful) {
                    val groupDto = response.body()
                        ?: return@withContext Result.failure(Exception("Failed to join group: Response body is null"))
                    Result.success(groupDto.toDomain())
                } else {
                    Result.failure(Exception("Failed to join group: ${response.code()} ${response.message()}"))
                }
            } catch (_: NoConnectivityException) {
                Result.failure(Exception("No internet connection. Please check your network and try again."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun updateGroupStatus(
        groupId: String,
        status: GroupStatus,
        currentWaypointIndex: Int?
    ): Result<TourGroup> = withContext(ioDispatcher) {
        try {
            val request = UpdateGroupStatusRequest(
                status = status.name,
                currentWaypointIndex = currentWaypointIndex
            )
            val response = groupApi.updateGroupStatus(groupId, request)

            if (response.isSuccessful) {
                val groupDto = response.body()
                    ?: return@withContext Result.failure(Exception("Failed to update group status: Response body is null"))
                Result.success(groupDto.toDomain())
            } else {
                Result.failure(Exception("Failed to update group status: ${response.code()} ${response.message()}"))
            }
        } catch (_: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMemberStatus(
        groupId: String,
        userId: String,
        status: MemberStatus
    ): Result<GroupMember> = withContext(ioDispatcher) {
        try {
            val request = GroupMemberStatusUpdateRequest(status = status.name)
            val response = groupApi.updateMemberStatus(groupId, userId, request)

            if (response.isSuccessful) {
                val memberDto = response.body()
                    ?: return@withContext Result.failure(Exception("Failed to update member status: Response body is null"))

                Result.success(memberDto.toDomain())
            } else {
                Result.failure(Exception("Failed to update member status: ${response.code()} ${response.message()}"))
            }
        } catch (_: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun requestRegroup(groupId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = groupApi.requestRegroup(groupId)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to request regroup: ${response.code()} ${response.message()}"))
            }
        } catch (_: NoConnectivityException) {
            Result.failure(Exception("No internet connection. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun updateLocation(
        groupId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ) {
        val location = MemberLocation(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            timestamp = Instant.now(),
            isInGeofence = true // This will be updated by geofence check service
        )

        locationSyncManager.updateLocation(groupId, userId, location)
    }

    override fun stopLocationUpdates(groupId: String, userId: String) {
        locationSyncManager.stopSharingLocation(groupId, userId)
    }

    override fun observeGroupLocations(groupId: String): Flow<Map<String, MemberLocation>> {
        return locationSyncManager.observeGroupLocations(groupId)
    }

    override fun observeMemberLocation(
        groupId: String,
        userId: String
    ): Flow<MemberLocation?> {
        return locationSyncManager.observeMemberLocation(groupId, userId)
    }

    private fun GroupDto.toDomain(): TourGroup {
        return TourGroup(
            id = this.id,
            tourId = this.tourId,
            guideId = this.guideId,
            name = this.name,
            joinCode = this.joinCode,
            status = GroupStatus.valueOf(this.status),
            createdAt = Instant.from(dateFormatter.parse(this.createdAt)),
            startedAt = this.startedAt?.let { Instant.from(dateFormatter.parse(it)) },
            endedAt = this.endedAt?.let { Instant.from(dateFormatter.parse(it)) },
            currentWaypointIndex = this.currentWaypointIndex,
            members = this.members.map { it.toDomain() }
        )
    }

    private fun com.xwurfel.tourry.feature.tracking.api.model.GroupMemberDto.toDomain(): GroupMember {
        return GroupMember(
            userId = this.userId,
            name = this.name,
            profilePictureUrl = this.profilePictureUrl,
            role = GroupRole.valueOf(this.role),
            status = MemberStatus.valueOf(this.status),
            lastLocation = null, // This will be updated from Firebase
            joinedAt = this.joinedAt?.let { Instant.from(dateFormatter.parse(it)) }
        )
    }
}