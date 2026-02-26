package ru.social.nework.dto

data class Comment(
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val published: String,
    val likeOwnerIds: Set<Long> = emptySet(),
    val likedByMe: Boolean = false,
    val attachment: Attachment? = null,
)