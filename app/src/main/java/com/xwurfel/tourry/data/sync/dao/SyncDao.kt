package com.xwurfel.tourry.data.sync.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncAction(syncEntity: SyncEntity): Long

    @Query("SELECT * FROM sync_actions ORDER BY timestamp ASC")
    suspend fun getAllSyncActions(): List<SyncEntity>

    @Query("SELECT * FROM sync_actions WHERE actionType = :actionType ORDER BY timestamp ASC")
    suspend fun getSyncActionsByType(actionType: SyncActionType): List<SyncEntity>

    @Query("SELECT * FROM sync_actions WHERE entityType = :entityType ORDER BY timestamp ASC")
    suspend fun getSyncActionsByEntityType(entityType: String): List<SyncEntity>

    @Query("DELETE FROM sync_actions WHERE id = :id")
    suspend fun deleteSyncAction(id: Long)

    @Query("DELETE FROM sync_actions WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteSyncActionsForEntity(entityType: String, entityId: Long)
}