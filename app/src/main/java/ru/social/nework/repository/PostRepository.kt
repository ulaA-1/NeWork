package ru.social.nework.repository

import ru.social.nework.ui.feed.PostUi
import ru.social.nework.data.dto.Attachment

interface PostRepository {
    suspend fun getPosts(): List<PostUi>
    suspend fun likePost(id: Long)
    suspend fun deletePost(id: Long)
    suspend fun createPost(
        content: String,
        attachment: Attachment? = null
    ): PostUi
}