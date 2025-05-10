package com.xwurfel.tourry.core.network.interceptor

import com.xwurfel.tourry.core.network.NetworkConnectivityMonitor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject


class NetworkConnectivityInterceptor @Inject constructor(
    private val networkConnectivityMonitor: NetworkConnectivityMonitor
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!networkConnectivityMonitor.isConnected.value) {
            throw NoConnectivityException()
        }
        return chain.proceed(chain.request())
    }
}

class NoConnectivityException : IOException("No internet connection available")