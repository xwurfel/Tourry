package com.xwurfel.tourry.data.network.interceptor

import android.content.Context
import com.xwurfel.tourry.data.auth.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.url.toString().contains("auth/login") ||
            originalRequest.url.toString().contains("auth/register")
        ) {
            return chain.proceed(originalRequest)
        }

        val tokenManager = TokenManager(context)
        val token = tokenManager.getToken() ?: return chain.proceed(originalRequest)

        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}