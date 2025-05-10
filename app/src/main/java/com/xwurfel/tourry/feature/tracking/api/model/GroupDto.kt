package com.xwurfel.tourry.feature.tracking.api.model

import com.google.gson.annotations.SerializedName

data class GroupDto(
    val id: String,
    @SerializedName("tour_id")
    val tourId: String,
    @SerializedName("guide_id")
    val guideId: String,
    val name: String,
    @SerializedName("join_code")
    val joinCode: String,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("started_at")
    val startedAt: String?,
    @SerializedName("ended_at")
    val endedAt: String?,
    @SerializedName("current_waypoint_index")
    val currentWaypointIndex: Int,
    val members: List<GroupMemberDto>
)

data class GroupMemberDto(
    @SerializedName("user_id")
    val userId: String,
    val name: String,
    @SerializedName("profile_picture_url")
    val profilePictureUrl: String?,
    val role: String,
    val status: String,
    @SerializedName("joined_at")
    val joinedAt: String?
)

data class CreateGroupRequest(
    @SerializedName("tour_id")
    val tourId: String,
    val name: String
)

data class JoinGroupRequest(
    @SerializedName("join_code")
    val joinCode: String
)

data class UpdateGroupStatusRequest(
    val status: String,
    @SerializedName("current_waypoint_index")
    val currentWaypointIndex: Int? = null
)

data class GroupMemberStatusUpdateRequest(
    val status: String
)