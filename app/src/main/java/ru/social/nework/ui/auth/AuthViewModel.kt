package ru.social.nework.ui.auth

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.social.nework.R
import ru.social.nework.repository.AuthRepository
import javax.inject.Inject

private const val TAG = "AuthViewModel"

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val authState: AuthState) : AuthUiState()
    data class Error(@StringRes val messageRes: Int) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableLiveData<AuthUiState>(AuthUiState.Initial)
    val loginState: LiveData<AuthUiState> = _loginState

    private val _registerState = MutableLiveData<AuthUiState>(AuthUiState.Initial)
    val registerState: LiveData<AuthUiState> = _registerState

    fun login(login: String, pass: String) {
        Log.d(TAG, "login() called")

        if (login.isBlank()) {
            _loginState.value = AuthUiState.Error(R.string.error_login_empty)
            return
        }
        if (pass.isBlank()) {
            _loginState.value = AuthUiState.Error(R.string.error_password_empty)
            return
        }

        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading

            val result = authRepository.login(login, pass)

            result.onSuccess { authState ->
                _loginState.value = AuthUiState.Success(authState)
            }.onFailure { e ->
                val errorRes = when (e) {
                    is HttpException -> if (e.code() == 400) R.string.error_invalid_credentials else R.string.error_unknown
                    else -> R.string.error_unknown
                }
                _loginState.value = AuthUiState.Error(errorRes)
            }
        }
    }

    fun register(
        login: String,
        pass: String,
        repeatPass: String,
        name: String,
        avatarUri: String? = null
    ) {
        if (login.isBlank()) {
            _registerState.value = AuthUiState.Error(R.string.error_login_empty); return
        }
        if (name.isBlank()) {
            _registerState.value = AuthUiState.Error(R.string.error_name_empty); return
        }
        if (pass.isBlank()) {
            _registerState.value = AuthUiState.Error(R.string.error_password_empty); return
        }
        if (pass != repeatPass) {
            _registerState.value = AuthUiState.Error(R.string.error_passwords_not_match); return
        }

        viewModelScope.launch {
            _registerState.value = AuthUiState.Loading

            val result = authRepository.register(login, pass, name, avatarUri)

            result.onSuccess { authState ->
                _registerState.value = AuthUiState.Success(authState)
            }.onFailure { e ->
                val errorRes = when (e) {
                    is HttpException -> if (e.code() == 400) R.string.error_user_already_exists else R.string.error_unknown
                    else -> R.string.error_unknown
                }
                _registerState.value = AuthUiState.Error(errorRes)
            }
        }
    }

    fun logout() = authRepository.logout()
}