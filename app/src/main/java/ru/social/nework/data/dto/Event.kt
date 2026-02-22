package ru.social.nework.data.dto

data class Event(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val datetime: String,
    val published: String,
    val type: String,
    val likeOwnerIds: List<Long>,
    val likedByMe: Boolean,
    val speakerIds: List<Long>,
    val participantIds: List<Long>,
    val participatedByMe: Boolean,
    val attachment: Attachment?,
    val link: String?
)