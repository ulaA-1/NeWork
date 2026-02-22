package ru.social.nework.data.dto

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class PostResponse(
    val id: Long,
    val author: String,
    val authorId: Long,
    val authorJob: String? = null,
    val authorAvatar: String? = null,
    val content: String,
    val published: String,
    val link: String? = null,
    val likeOwnerIds: List<Long> = emptyList(),
    val likedByMe: Boolean = false,
    val mentionIds: List<Long> = emptyList(),
    val mentionedMe: Boolean = false,
    val ownedByMe: Boolean = false,
    val attachment: Attachment? = null
) {
    val formattedDate: String
        get() = try {
            val instant = Instant.parse(published)
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (e: Exception) {
            published
        }
}