package ru.social.nework.repository

import kotlinx.coroutines.flow.Flow
import ru.social.nework.dto.User

interface UserRepository {
    val data: Flow<List<User>>

    suspend fun getAll()
}