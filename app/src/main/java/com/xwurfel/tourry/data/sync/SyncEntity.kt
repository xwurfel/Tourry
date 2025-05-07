package com.xwurfel.tourry.data.sync

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

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

class SyncTypeConverters {
    @TypeConverter
    fun fromSyncActionType(value: SyncActionType): String {
        return value.name
    }

    @TypeConverter
    fun toSyncActionType(value: String): SyncActionType {
        return SyncActionType.valueOf(value)
    }
}