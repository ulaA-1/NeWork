package ru.social.nework.ui.auth

data class AuthState(
    val id: Long = 0,
    val token: String? = null
) {
    val isAuthenticated: Boolean
        get() = id != 0L && !token.isNullOrBlank()
}