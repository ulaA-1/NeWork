package ru.social.nework.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.social.nework.dto.Attachment
import ru.social.nework.dto.Coords
import ru.social.nework.dto.Post
import ru.social.nework.enumeration.AttachmentType


@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String? = "",
    val authorAvatar: String? = "",
    @Embedded
    val coords: PointEmbeddable?,
    val content: String,
    val published: String,
    val link: String? = null,
    @TypeConverters
    val mentionIds: List<Long>,
    val mentionedMe: Boolean,
    @TypeConverters
    val likeOwnerIds: List<Long>,
    val likedByMe: Boolean,
    val read: Boolean = true,
    @Embedded
    var attachment: AttachmentEmbeddable?,
    val likes: Int = 0
) {
    fun toDto() = Post(id, authorId, author, authorJob, authorAvatar, coords?.toDto(), content, published, link, mentionIds, mentionedMe, likeOwnerIds, likedByMe,  attachment?.toDto(), emptyMap(), likes = likes)

    companion object {
        fun fromDto(dto: Post) =
            PostEntity(
                dto.id,
                dto.authorId,
                dto.author,
                dto.authorJob,
                dto.authorAvatar,
                coords = PointEmbeddable.fromDto(dto.coords),
                dto.content,
                dto.published,
                dto.link,
                dto.mentionIds,
                dto.mentionedMe,
                dto.likeOwnerIds,
                dto.likedByMe,
                attachment = AttachmentEmbeddable.fromDto(dto.attachment),
                likes = dto.likeOwnerIds.size
            )

        fun fromDtoNew(dto: Post) =
            PostEntity(
                dto.id,
                dto.authorId,
                dto.author,
                dto.authorJob,
                dto.authorAvatar,
                coords = PointEmbeddable.fromDto(dto.coords),
                dto.content,
                dto.published,
                dto.link,
                dto.mentionIds,
                dto.mentionedMe,
                dto.likeOwnerIds,
                dto.likedByMe,
                read = false,
                attachment = AttachmentEmbeddable.fromDto(dto.attachment),
                likes = dto.likeOwnerIds.size
            )

    }
}

data class PointEmbeddable(
    var latitude: Double,
    var longitude: Double,
){
    fun toDto() = Coords(latitude, longitude)

    companion object {
        fun fromDto(dto: Coords?) = dto?.let {
            PointEmbeddable(it.lat, it.long)
        }
    }
}
data class AttachmentEmbeddable(
    var url: String,
    var type: AttachmentType,
) {
    fun toDto() = Attachment(url, type)

    companion object {
        fun fromDto(dto: Attachment?) = dto?.let {
            AttachmentEmbeddable(it.url, it.type)
        }
    }
}
fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity.Companion::fromDto)
fun List<Post>.toEntityNew(): List<PostEntity> = map(PostEntity.Companion::fromDtoNew)

class Converters {
    @TypeConverter
    fun listToString(list: List<Long>?): String? {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun stringToList(json: String?): List<Long?>? {
        return Gson().fromJson(json, object : TypeToken<List<Long?>?>() {}.type)
    }
}
