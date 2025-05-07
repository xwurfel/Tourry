package com.xwurfel.tourry.data.base

import android.content.Context
import com.google.gson.Gson
import com.xwurfel.tourry.data.network.util.NetworkUtils
import com.xwurfel.tourry.data.sync.SyncActionType
import com.xwurfel.tourry.data.sync.SyncEntity
import com.xwurfel.tourry.data.sync.dao.SyncDao

abstract class BaseRepository(
    protected val context: Context,
    protected val syncDao: SyncDao,
    protected val gson: Gson
) {
    protected suspend fun addSyncAction(
        entityType: String,
        entityId: Long,
        actionType: SyncActionType,
        data: Any? = null
    ) {
        val jsonData = if (data != null) gson.toJson(data) else null

        val syncEntity = SyncEntity(
            entityType = entityType,
            entityId = entityId,
            actionType = actionType,
            actionData = jsonData
        )

        syncDao.insertSyncAction(syncEntity)
    }

    protected fun isNetworkAvailable(): Boolean {
        return NetworkUtils.isNetworkAvailable(context)
    }
}