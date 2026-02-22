package ru.social.nework.api

import okhttp3.Interceptor
import okhttp3.Response
import ru.social.nework.ui.auth.AppAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val appAuth: AppAuth
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val path = request.url.encodedPath
        val noAuthNeeded =
            path == "/api/posts" ||
                    path.startsWith("/api/posts/") ||
                    path.endsWith("/api/users/authentication") ||
                    path.endsWith("/api/users/registration")

        if (noAuthNeeded) return chain.proceed(request)

        val token = appAuth.authStateFlow.value.token
        if (token.isNullOrBlank()) {
            return chain.proceed(request)
        }

        val newRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}