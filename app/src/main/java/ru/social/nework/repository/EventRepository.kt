package ru.social.nework.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import ru.social.nework.dto.Event
import ru.social.nework.dto.Job
import ru.social.nework.dto.Media
import ru.social.nework.dto.MediaUpload
import ru.social.nework.dto.User
import ru.social.nework.enumeration.AttachmentType

interface EventRepository {

    val data: Flow<PagingData<Event>>

    suspend fun getAll()
    suspend fun save(event: Event)
    suspend fun saveWithAttachment(event: Event, upload: MediaUpload, attachmentType: AttachmentType)
    suspend fun saveLocal(event: Event)
    suspend fun getEventById(eventId: Long): Event
    suspend fun likeById(event: Event): Event
    suspend fun likeByIdLocal(event: Event)
    suspend fun participateById(event: Event): Event
    suspend fun participateByIdLocal(event: Event)
    suspend fun removeById(event: Event)
    suspend fun getLikers(event: Event): List<User>
    suspend fun getSpeakers(event: Event): List<User>
    suspend fun getParticipants(event: Event): List<User>
    suspend fun upload(upload: MediaUpload): Media
    suspend fun getUser(userId: Long): User
    suspend fun readNewEvents()
    fun getNewerCount(id: Long): Flow<Int>
    suspend fun latestReadEventId(): Long
    suspend fun getLastJob(userId: Long): Job?
}