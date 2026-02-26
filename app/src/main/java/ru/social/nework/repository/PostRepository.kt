package ru.social.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.social.nework.dto.Job
import ru.social.nework.dto.Media
import ru.social.nework.dto.MediaUpload
import ru.social.nework.dto.Post
import ru.social.nework.dto.User
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.model.UserAvatar

interface PostRepository {
    val data: Flow<PagingData<Post>>
    suspend fun getAll()
    suspend fun save(post: Post)
    suspend fun saveWithAttachment(post: Post, upload: MediaUpload, attachmentType: AttachmentType)
    suspend fun saveLocal(post: Post)
    suspend fun getPostById(postId: Long): Post
    suspend fun likeById(post: Post): Post
    suspend fun likeByIdLocal(post: Post)
    suspend fun removeById(post: Post)
    suspend fun getUserAvatar(userId: Long):String
    suspend fun getLikersAvatars(post: Post): Set<UserAvatar>
    suspend fun getMentionedAvatars(post: Post): Set<UserAvatar>
    suspend fun getUser(userId: Long): User
    suspend fun getLikers(post: Post): List<User>
    suspend fun getMentioned(post: Post): List<User>
    suspend fun upload(upload: MediaUpload): Media
    suspend fun readNewPosts()
    fun getNewerCount(id: Long): Flow<Int>
    suspend fun latestReadPostId(): Long
    suspend fun getLastJob(userId: Long): Job?
    fun setUser(userId: Long)
}