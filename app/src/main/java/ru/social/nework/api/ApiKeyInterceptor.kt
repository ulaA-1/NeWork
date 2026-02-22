package ru.social.nework.api

import okhttp3.Interceptor
import okhttp3.Response
import ru.social.nework.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Api-Key", BuildConfig.API_KEY)
            .build()
        return chain.proceed(request)
    }
}