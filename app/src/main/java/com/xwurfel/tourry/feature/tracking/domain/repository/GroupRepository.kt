package com.xwurfel.tourry.feature.tracking.domain.repository

import com.xwurfel.tourry.feature.tracking.domain.model.GroupMember
import com.xwurfel.tourry.feature.tracking.domain.model.GroupStatus
import com.xwurfel.tourry.feature.tracking.domain.model.MemberLocation
import com.xwurfel.tourry.feature.tracking.domain.model.MemberStatus
import com.xwurfel.tourry.feature.tracking.domain.model.TourGroup
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun getGroup(groupId: String): Result<TourGroup>

    suspend fun createGroup(tourId: String, name: String): Result<TourGroup>

    suspend fun joinGroup(joinCode: String): Result<TourGroup>

    suspend fun updateGroupStatus(
        groupId: String,
        status: GroupStatus,
        currentWaypointIndex: Int? = null
    ): Result<TourGroup>

    suspend fun updateMemberStatus(
        groupId: String,
        userId: String,
        status: MemberStatus
    ): Result<GroupMember>

    suspend fun requestRegroup(groupId: String): Result<Unit>

    fun updateLocation(
        groupId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    )

    fun stopLocationUpdates(groupId: String, userId: String)

    fun observeGroupLocations(groupId: String): Flow<Map<String, MemberLocation>>

    fun observeMemberLocation(groupId: String, userId: String): Flow<MemberLocation?>
}