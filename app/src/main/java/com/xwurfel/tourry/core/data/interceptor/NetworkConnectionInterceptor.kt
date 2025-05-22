package com.xwurfel.tourry.core.data.interceptor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import com.xwurfel.tourry.core.data.error.network.NetworkError
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class NetworkConnectionInterceptor(private val context: Context) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isConnected) throw NetworkError.ConnectionError
        val builder = chain.request().newBuilder()
        return chain.proceed(builder.build())
    }

    private val isConnected: Boolean
        get() {
            val connectivityManager =
                context.getSystemService<ConnectivityManager>() ?: return false

            return connectivityManager.activeNetwork
                ?.let { connectivityManager.getNetworkCapabilities(it) }
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
}