package ru.social.nework.repository

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.social.nework.api.ApiService
import ru.social.nework.ui.auth.AppAuth
import ru.social.nework.ui.auth.AuthState
import ru.social.nework.data.dto.Token
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthRepository"

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val appAuth: AppAuth
) : AuthRepository {

    override val authState: AuthState
        get() = appAuth.authStateFlow.value

    override suspend fun login(login: String, pass: String): Result<AuthState> {
        Log.d(TAG, "Попытка входа: login=$login")

        return try {
            val response = apiService.authenticateUser(login, pass)

            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse != null) {
                    appAuth.setAuth(tokenResponse.id, tokenResponse.token)
                    Log.d(TAG, "Успешный вход: id=${tokenResponse.id}")
                    Result.success(authState)
                } else {
                    Log.e(TAG, "Тело ответа пустое")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Ошибка сервера: ${response.code()} - $errorBody")

                when (response.code()) {
                    400 -> Result.failure(Exception("Неверный логин или пароль"))
                    else -> Result.failure(Exception("Ошибка сервера: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при входе: ${e.message}", e)
            Result.failure(Exception("Ошибка сети: ${e.message}"))
        }
    }

    override suspend fun register(
        login: String,
        pass: String,
        name: String,
        avatarUri: String?
    ): Result<AuthState> {
        Log.d(TAG, "Попытка регистрации: login=$login, name=$name")

        return try {
            val loginPart = login.toRequestBody("text/plain".toMediaTypeOrNull())
            val passPart = pass.toRequestBody("text/plain".toMediaTypeOrNull())
            val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val filePart = avatarUri?.let { uri ->
                null
            }

            val response = apiService.registerUser(loginPart, passPart, namePart, filePart)

            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse != null) {
                    appAuth.setAuth(tokenResponse.id, tokenResponse.token)
                    Log.d(TAG, "Успешная регистрация: id=${tokenResponse.id}")
                    Result.success(authState)
                } else {
                    Log.e(TAG, "Тело ответа пустое")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Ошибка сервера: ${response.code()} - $errorBody")

                when (response.code()) {
                    400 -> Result.failure(Exception("Пользователь с таким логином уже зарегистрирован"))
                    else -> Result.failure(Exception("Ошибка сервера: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при регистрации: ${e.message}", e)
            Result.failure(Exception("Ошибка сети: ${e.message}"))
        }
    }

    override fun logout() {
        Log.d(TAG, "Выход из аккаунта")
        appAuth.clearAuth()
    }
}