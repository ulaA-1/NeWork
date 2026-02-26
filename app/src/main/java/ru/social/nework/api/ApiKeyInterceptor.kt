package ru.social.nework.api

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import ru.social.nework.BuildConfig
import ru.social.nework.auth.AppAuth

fun loggingInterceptor() = HttpLoggingInterceptor()
    .apply {
        if (BuildConfig.DEBUG) {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

fun authInterceptor(auth: AppAuth) = fun(chain: Interceptor.Chain): Response {
    auth.authStateFlow.value.token?.let { token ->
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", token)
            .build()
        return chain.proceed(newRequest)
    }
    return chain.proceed(chain.request())
}

fun apiInterceptor() = fun(chain: Interceptor.Chain): Response = chain.run {
    proceed(
        request()
            .newBuilder()
            .addHeader("Api-Key", BuildConfig.API_KEY)
            .build()
    )
}
