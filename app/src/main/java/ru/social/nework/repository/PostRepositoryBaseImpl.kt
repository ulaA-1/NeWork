package ru.social.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.social.nework.api.ApiService
import ru.social.nework.dao.PostDao
import ru.social.nework.dto.Attachment
import ru.social.nework.dto.Job
import ru.social.nework.dto.Media
import ru.social.nework.dto.MediaUpload
import ru.social.nework.dto.Post
import ru.social.nework.dto.User
import ru.social.nework.entity.PostEntity
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.error.ApiError
import ru.social.nework.error.AppError
import ru.social.nework.error.NetworkError
import ru.social.nework.error.UnknownError
import ru.social.nework.model.UserAvatar
import java.io.IOException

abstract class PostRepositoryBaseImpl(
    private val dao: PostDao,
    private val apiService: ApiService,
): PostRepository {

    abstract override val data: Flow<PagingData<Post>>


    abstract override suspend fun getAll()

    override fun setUser(userId: Long) {
    }

    override suspend fun save(post: Post) {
        try {
            val response = apiService.savePost(post.toPostApi())
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            e.printStackTrace()
            throw UnknownError
        }
    }

    override suspend fun saveWithAttachment(
        post: Post,
        upload: MediaUpload,
        attachmentType: AttachmentType
    ) {
        try {
            val media = upload(upload)
            val postWithAttachment = post.copy(attachment = Attachment(media.url, attachmentType))
            save(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )

            val response = apiService.upload(media)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun readNewPosts() {
        dao.readNewPosts()
    }

    abstract override fun getNewerCount(id: Long): Flow<Int>

    abstract override suspend fun latestReadPostId(): Long


    override suspend fun getPostById(postId: Long): Post {
        try {
            val response = apiService.getPostById(postId)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            val postEntity = PostEntity.fromDto(body)
            dao.insert(postEntity)
            return postEntity.toDto()
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override suspend fun saveLocal(post: Post) {
        dao.insert(PostEntity.fromDto(post))
    }

    abstract override suspend fun likeById(post: Post) : Post

    abstract override suspend fun likeByIdLocal(post: Post)

    override suspend fun removeById(post: Post) {
        val postRemoved = post.copy()
        try {
            dao.removeById(post.id)
            val response = apiService.removePostById(post.id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            saveLocal(postRemoved)
            throw NetworkError
        } catch (e: Exception) {
            saveLocal(postRemoved)
            throw UnknownError
        }
    }

    override suspend fun getLikersAvatars(post: Post): Set<UserAvatar>{
        var setAvatars = emptySet<UserAvatar>()
        if(post.likeOwnerIds.isNotEmpty()){
            post.likeOwnerIds.forEach{
                setAvatars = setAvatars.plus(UserAvatar(it, getUserAvatar(it)))
            }
        }
        return setAvatars
    }

    override suspend fun getMentionedAvatars(post: Post): Set<UserAvatar> {
        var setAvatars = emptySet<UserAvatar>()
        if(post.mentionIds.isNotEmpty()){
            post.mentionIds.forEach{
                setAvatars = setAvatars.plus(UserAvatar(it, getUserAvatar(it)))
            }
        }
        return setAvatars
    }

    override suspend fun getLikers(post: Post): List<User> {
        var likers = emptyList<User>()
        if(post.likeOwnerIds.isNotEmpty()){
            post.likeOwnerIds.forEach{
                likers = likers.plus(getUser(it))
            }
        }
        return likers
    }

    override suspend fun getMentioned(post: Post): List<User> {
        var mentioned = emptyList<User>()
        if(post.mentionIds.isNotEmpty()){
            post.mentionIds.forEach{
                mentioned = mentioned.plus(getUser(it))
            }
        }
        return mentioned
    }

    override suspend fun getUserAvatar(userId: Long): String {
        try {
            val response = apiService.getUserById(userId)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            if(body.avatar == null){
                return ""
            }
            return body.avatar
        } catch (e: IOException) {
            throw NetworkError
        }
    }
    override suspend fun getUser(userId: Long): User {
        try {
            val response = apiService.getUserById(userId)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        }
    }

    override suspend fun getLastJob(userId: Long): Job?{
        try {
            val response = apiService.getUserJobs(userId)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            val job = body.filter { it.finish == null }
                .sortedByDescending { it.id }
                .first()
            return job
        } catch (e: NoSuchElementException){
            return null
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

}