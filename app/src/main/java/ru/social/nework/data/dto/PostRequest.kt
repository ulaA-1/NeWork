package ru.social.nework.data.dto

data class PostRequest(
    val id: Long = 0,
    val content: String,
    val link: String? = null,
    val attachment: Attachment? = null,
    val mentionIds: List<Long> = emptyList()
)