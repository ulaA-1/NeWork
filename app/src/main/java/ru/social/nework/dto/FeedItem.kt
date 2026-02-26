package ru.social.nework.dto

import android.content.Context
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.enumeration.EventType
import ru.social.nework.util.TextDrawable

sealed class FeedItem{
    abstract val id: Long
}

data class User(
    override val id: Long,
    val login: String,
    val name: String,
    val avatar: String?
) : FeedItem(){
    fun userIcon(context: Context): TextDrawable{
        val char = if(name != "") name.first() else ""
        return TextDrawable(context, char.toString())
    }

}

data class Post(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String? = "",
    val authorAvatar: String? = "",
    val coords: Coords? = null,
    val content: String,
    val published: String,
    val link: String? = null,
    val mentionIds: List<Long>,
    val mentionedMe: Boolean,
    val likeOwnerIds: List<Long>,
    val likedByMe: Boolean,
    val attachment: Attachment? = null,
    val users: Map<String, UserPreview>,
    val ownedByMe: Boolean = false,
    val likes: Int = 0
                ) : FeedItem()
{
    fun toPostApi() = PostApi(id, authorId, author, authorJob, authorAvatar, coords, content, published, link, mentionIds, mentionedMe, likeOwnerIds, likedByMe,  attachment, users)
                }

data class PostApi(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String? = "",
    val authorAvatar: String? = "",
    val coords: Coords? = null,
    val content: String,
    val published: String,
    val link: String? = null,
    val mentionIds: List<Long>,
    val mentionedMe: Boolean,
    val likeOwnerIds: List<Long>,
    val likedByMe: Boolean,
    val attachment: Attachment? = null,
    val users: Map<String, UserPreview>
) : FeedItem()



data class Event(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String? = "",
    val authorAvatar: String? = "",
    val content: String,
    val datetime: String,
    val published: String,
    val coords: Coords? = null,
    val type: EventType,
    val likeOwnerIds: List<Long>,
    val likedByMe: Boolean,
    val speakerIds: List<Long>,
    val participantsIds: List<Long>,
    val participatedByMe: Boolean,
    val attachment: Attachment? = null,
    val link: String? = null,
    val users: Map<String, UserPreview>,
    val ownedByMe: Boolean = false,
    val likes: Int = 0,
    val participants: Int = 0
) : FeedItem(){

    fun toEventApi() = EventApi(id, authorId, author, authorJob, authorAvatar, content, datetime, published, coords, type, likeOwnerIds, likedByMe, speakerIds, participantsIds, participatedByMe, attachment, link, users)
}

data class EventApi(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String? = "",
    val authorAvatar: String? = "",
    val content: String,
    val datetime: String,
    val published: String,
    val coords: Coords? = null,
    val type: EventType,
    val likeOwnerIds: List<Long>,
    val likedByMe: Boolean,
    val speakerIds: List<Long>,
    val participantsIds: List<Long>,
    val participatedByMe: Boolean,
    val attachment: Attachment? = null,
    val link: String? = null,
    val users: Map<String, UserPreview>,
) : FeedItem()

data class Job(
    override val id: Long,
    val name: String,
    val position: String,
    val start: String,
    val finish: String?,
    val link: String?,
    val userId: Long,
    val ownedByMe: Boolean = false,
): FeedItem()

data class Attachment(
    val url: String,
    val type: AttachmentType,
    var isPlaying: Boolean = false,
    var isLoading: Boolean = false,
    var progress: Int = 0

)

data class Coords(
    val lat: Double,
    val long: Double
)

data class UserPreview(
    val name: String,
    val avatar: String?
)