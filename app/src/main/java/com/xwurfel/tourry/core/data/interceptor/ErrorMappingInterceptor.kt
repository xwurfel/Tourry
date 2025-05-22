package com.xwurfel.tourry.core.data.interceptor

import com.xwurfel.tourry.core.data.error.network.NetworkErrorConverter
import okhttp3.Interceptor
import okhttp3.Response

class ErrorMappingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request().newBuilder().build())
        if (!response.isSuccessful) {
            val exception = NetworkErrorConverter.networkErrorFrom(response)
            throw exception
        }
        return response
    }
}