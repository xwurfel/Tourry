package com.xwurfel.tourry.data.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_actions")
data class SyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val actionType: SyncActionType,
    val timestamp: Long = System.currentTimeMillis(),
    val actionData: String? = null // JSON data for the entity
)

enum class SyncActionType {
    CREATE,
    UPDATE,
    DELETE
}