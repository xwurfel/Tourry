package com.xwurfel.tourry.core

import android.app.Application
import com.xwurfel.tourry.data.auth.TokenManager
import com.xwurfel.tourry.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TourryApplication : Application() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate() {
        super.onCreate()
        syncManager.scheduleSyncWork()
    }
}