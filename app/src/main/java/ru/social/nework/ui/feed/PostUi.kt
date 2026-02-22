package ru.social.nework.ui.feed

enum class AttachmentType { IMAGE, VIDEO, AUDIO }

data class PostUi(
    val id: Long,
    val author: String,
    val published: String,
    val authorAvatar: String?,
    val content: String,
    val likes: Int,
    val likedByMe: Boolean,
    val ownedByMe: Boolean,
    val linkUrl: String? = null,
    val attachmentUrl: String? = null,
    val attachmentType: AttachmentType? = null,
)
