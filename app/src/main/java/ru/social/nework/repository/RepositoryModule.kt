package ru.social.nework.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
@Qualifier
annotation class PostRepo

@Qualifier
annotation class PostRepoUserWall

@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {
    @Binds
    @Singleton
    @PostRepo
    abstract fun bindPostRepo(impl: PostRepositoryImpl): PostRepository
    @Binds
    @Singleton
    @PostRepoUserWall
    abstract fun bindPostRepoUserWall(impl: PostRepositoryUserWallImpl): PostRepository
    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository
    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds
    @Singleton
    abstract fun bindJobRepository(impl: JobRepositoryImpl): JobRepository
}
