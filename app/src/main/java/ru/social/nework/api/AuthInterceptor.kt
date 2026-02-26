package ru.social.nework.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.social.nework.auth.AppAuth
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AuthInterceptor {
    @Provides
    @Singleton
    fun provideApiService(auth: AppAuth): ApiService {
        return retrofit(okhttp(authInterceptor(auth), apiInterceptor(), loggingInterceptor()))
            .create(ApiService::class.java)
    }
}