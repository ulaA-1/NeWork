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
import ru.social.nework.dao.WallRemoteKeyDao
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
class PostRepositoryUserWallImpl @Inject constructor(
    private val appDb: AppDb,
    private val dao: PostDao,
    private val apiService: ApiService,
    private val auth: AppAuth,
    private val wallRemoteKeyDao: WallRemoteKeyDao,
    ) :
    PostRepositoryBaseImpl(dao, apiService) {

    var userId: Long = 0

    override lateinit var data: Flow<PagingData<Post>>
    @OptIn(ExperimentalPagingApi::class)
    override fun setUser(userId: Long) {
        this.userId = userId
        data = Pager(
            config = PagingConfig(pageSize = 25),
            remoteMediator = WallRemoteMediator(apiService, appDb, dao, wallRemoteKeyDao, auth, userId),
            pagingSourceFactory = {
                dao.pagingSourceUserWall(userId)
            }
        ).flow.map { pagingData ->
            pagingData.map(PostEntity::toDto)
        }
    }


    override suspend fun getAll() {
        try {
            val response =
                if(isMyWall()){
                    apiService.getMyWall()
                } else{
                    apiService.getUserWall(userId)
                }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(post: Post): Post {
        try {
            likeByIdLocal(post)
            val response = if (!post.likedByMe) {
                apiService.likeUserPostById(userId, post.id)
            } else {
                apiService.dislikeUserPostById(userId, post.id)
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

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)
            val response =
                if(isMyWall()){
                    apiService.getMyWallNewer(id)
                } else{
                    apiService.getUserWallNewer(userId, id)
                }
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
        return dao.latestUserReadPostId(userId) ?: 0L
    }


    fun isMyWall(): Boolean{
        return userId == auth.authStateFlow.value.id
    }
}