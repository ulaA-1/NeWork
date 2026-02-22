package ru.social.nework.ui.feed

import ru.social.nework.data.dto.PostResponse
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val UI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

fun PostResponse.toUi(myId: Long): PostUi {
    val formattedDate = try {
        OffsetDateTime.parse(published).format(UI_DATE_FORMAT)
    } catch (e: Exception) {
        published
    }

    val owned = (myId != 0L && authorId == myId)

    return PostUi(
        id = id,
        author = author,
        authorAvatar = authorAvatar,
        published = formattedDate,
        content = content,
        likes = likeOwnerIds.size,
        likedByMe = likedByMe,
        ownedByMe = owned,
        linkUrl = link,
        attachmentUrl = attachment?.url,
        attachmentType = when (attachment?.type?.uppercase()) {
            "IMAGE" -> AttachmentType.IMAGE
            "VIDEO" -> AttachmentType.VIDEO
            "AUDIO" -> AttachmentType.AUDIO
            else -> null
        }
    )
}