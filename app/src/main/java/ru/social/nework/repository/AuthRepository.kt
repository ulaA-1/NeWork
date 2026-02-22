package ru.social.nework.repository

import ru.social.nework.ui.auth.AuthState

interface AuthRepository {
    val authState: AuthState
    suspend fun login(login: String, pass: String): Result<AuthState>
    suspend fun register(
        login: String,
        pass: String,
        name: String,
        avatarUri: String? = null
    ): Result<AuthState>

    fun logout()
}