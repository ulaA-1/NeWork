package ru.social.nework.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.social.nework.ui.auth.AppAuth
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAppAuth(
        app: android.app.Application
    ): AppAuth {
        return AppAuth(app.applicationContext)
    }
}