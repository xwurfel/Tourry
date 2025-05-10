package com.xwurfel.tourry.feature.tracking.api

import com.xwurfel.tourry.feature.tracking.api.model.CreateGroupRequest
import com.xwurfel.tourry.feature.tracking.api.model.GroupDto
import com.xwurfel.tourry.feature.tracking.api.model.GroupMemberDto
import com.xwurfel.tourry.feature.tracking.api.model.GroupMemberStatusUpdateRequest
import com.xwurfel.tourry.feature.tracking.api.model.JoinGroupRequest
import com.xwurfel.tourry.feature.tracking.api.model.UpdateGroupStatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GroupApi {
    @GET("groups/{id}")
    suspend fun getGroup(@Path("id") groupId: String): Response<GroupDto>

    @GET("groups")
    suspend fun getActiveGroups(@Query("status") status: String = "ACTIVE"): Response<List<GroupDto>>

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<GroupDto>

    @POST("groups/join")
    suspend fun joinGroupByCode(@Body request: JoinGroupRequest): Response<GroupDto>

    @PUT("groups/{id}/status")
    suspend fun updateGroupStatus(
        @Path("id") groupId: String,
        @Body request: UpdateGroupStatusRequest
    ): Response<GroupDto>

    @PUT("groups/{groupId}/members/{userId}/status")
    suspend fun updateMemberStatus(
        @Path("groupId") groupId: String,
        @Path("userId") userId: String,
        @Body request: GroupMemberStatusUpdateRequest
    ): Response<GroupMemberDto>

    @POST("groups/{id}/geofence/regroup")
    suspend fun requestRegroup(@Path("id") groupId: String): Response<Unit>
}