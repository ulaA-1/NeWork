package ru.social.nework.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.social.nework.entity.EventEntity

@Dao
interface EventDao {

    @Query("SELECT * FROM EventEntity where read = 1 ORDER BY id DESC")
    fun getAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM EventEntity ORDER BY id DESC")
    fun pagingSource(): PagingSource<Int, EventEntity>

    @Query("SELECT COUNT(*) == 0 FROM EventEntity")
    suspend fun isEmpty(): Boolean

    @Query("SELECT MAX(id) FROM EventEntity where read = 1")
    suspend fun latestReadEventId(): Long?

    @Query("SELECT COUNT(*) FROM EventEntity where read = 0")
    suspend fun newerCount(): Int

    @Query("SELECT COUNT(*) FROM EventEntity")
    suspend fun eventsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(events: List<EventEntity>)

    @Query("UPDATE EventEntity SET read = 1 WHERE read = 0")
    suspend fun readNewEvents()

    @Query("UPDATE EventEntity SET content = :content WHERE id = :id")
    suspend fun updateContentById(id: Long, content: String)

    suspend fun save(event: EventEntity) =
        if (event.id == 0L) insert(event) else updateContentById(event.id, event.content)

    @Query("""
        UPDATE EventEntity SET
        likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
        likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END,
        likeOwnerIds = :likeOwnerIds
        WHERE id = :id
        """)
    suspend fun likeById(id: Long, likeOwnerIds: List<Long>)

    @Query("DELETE FROM EventEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query("""
        UPDATE EventEntity SET
        participants = participants + CASE WHEN participatedByMe THEN -1 ELSE 1 END,
        participatedByMe = CASE WHEN participatedByMe THEN 0 ELSE 1 END,
        participantsIds = :participantsIds
        WHERE id = :id
        """)
    suspend fun participateById(id: Long, participantsIds: List<Long>)

    @Query("DELETE FROM EventEntity")
    suspend fun removeAll()
}