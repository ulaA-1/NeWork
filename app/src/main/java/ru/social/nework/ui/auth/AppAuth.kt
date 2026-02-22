package ru.social.nework.ui.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val ID_KEY = "id"
    private val TOKEN_KEY = "token"

    private val _authStateFlow: MutableStateFlow<AuthState>
    val authStateFlow: StateFlow<AuthState>

    init {
        val savedId = prefs.getLong(ID_KEY, 0)
        val savedToken = prefs.getString(TOKEN_KEY, null)
        _authStateFlow = if (savedId != 0L && !savedToken.isNullOrBlank()) {
            MutableStateFlow(AuthState(savedId, savedToken))
        } else {
            prefs.edit().clear().apply()
            MutableStateFlow(AuthState())
        }
        authStateFlow = _authStateFlow.asStateFlow()
    }

    val currentToken: String?
        get() = authStateFlow.value.token

    val myId: Long
        get() = authStateFlow.value.id

    @Synchronized
    fun setAuth(id: Long, token: String) {
        _authStateFlow.value = AuthState(id, token)
        prefs.edit()
            .putLong(ID_KEY, id)
            .putString(TOKEN_KEY, token)
            .apply()
    }

    @Synchronized
    fun clearAuth() {
        _authStateFlow.value = AuthState()
        prefs.edit().clear().apply()
    }
}