package ru.social.nework.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.social.nework.api.ApiService
import ru.social.nework.auth.AppAuth
import ru.social.nework.dao.PostDao
import ru.social.nework.dao.PostRemoteKeyDao
import ru.social.nework.db.AppDb
import ru.social.nework.dto.Post
import ru.social.nework.entity.PostEntity
import ru.social.nework.entity.toEntity
import ru.social.nework.entity.toEntityNew
import ru.social.nework.error.ApiError
import ru.social.nework.error.AppError
import ru.social.nework.error.NetworkError
import ru.social.nework.error.UnknownError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PostRepositoryImpl @Inject constructor(
    appDb: AppDb,
    private val dao: PostDao,
    private val apiService: ApiService,
    private val auth: AppAuth,
    postRemoteKeyDao: PostRemoteKeyDao,
): PostRepositoryBaseImpl(dao, apiService) {

    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 25),
        remoteMediator = PostRemoteMediator(apiService, appDb, dao, postRemoteKeyDao),
        pagingSourceFactory = dao::pagingSource,
    ).flow.map { pagingData ->
        pagingData.map(PostEntity::toDto)
    }

    override suspend fun getAll() {
        try {
            val response = apiService.getAllPosts()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        }
    }

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
                delay(10_000L)
                val response = apiService.getNewerPosts(id)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }

                val body = response.body() ?: throw ApiError(response.code(), response.message())
                //записываем новые посты с признаком read = false
                dao.insert(body.toEntityNew())
                emit(body.size)
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    override suspend fun latestReadPostId(): Long {
        return dao.latestReadPostId() ?: 0L
    }


    override suspend fun likeById(post: Post) : Post {
        try {
            likeByIdLocal(post)
            val response = if (!post.likedByMe) {
                apiService.likePostById(post.id)
            } else {
                apiService.dislikePostById(post.id)
            }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            likeByIdLocal(post)
            throw NetworkError
        } catch (e: Exception) {
            likeByIdLocal(post)
            throw UnknownError
        }
    }

    override suspend fun likeByIdLocal(post: Post) {
        return if(post.likedByMe){
            val list = post.likeOwnerIds.filter{
                it != auth.authStateFlow.value.id
            }
            dao.likeById(post.id, list)
        } else{
            val list = post.likeOwnerIds.plus(auth.authStateFlow.value.id)
            dao.likeById(post.id, list)
        }

    }



}