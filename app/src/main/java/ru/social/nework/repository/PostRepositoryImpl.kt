package ru.social.nework.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import ru.social.nework.api.ApiService
import ru.social.nework.data.dto.Attachment
import ru.social.nework.data.dto.PostRequest
import ru.social.nework.ui.auth.AppAuth
import ru.social.nework.ui.feed.PostUi
import ru.social.nework.ui.feed.toUi
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PostRepository"

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val appAuth: AppAuth
) : PostRepository {

    override suspend fun getPosts(): List<PostUi> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getPosts() вызван")

        val response = apiService.getAllPosts()
        Log.d(TAG, "response.code() = ${response.code()}")

        if (!response.isSuccessful) {
            val msg = response.errorBody()?.string()
            Log.e(TAG, "Ошибка сервера: ${response.code()} - $msg")
            throw HttpException(response)
        }

        val body = response.body().orEmpty()
        Log.d(TAG, "Получено ${body.size} постов")

        val myId = appAuth.myId

        withContext(Dispatchers.Default) {
            body.map { dto -> dto.toUi(myId) }
        }
    }

    override suspend fun likePost(id: Long): Unit = withContext(Dispatchers.IO) {
        val response = apiService.likePost(id)
        if (!response.isSuccessful) throw Exception("Ошибка лайка: ${response.code()}")
    }

    override suspend fun deletePost(id: Long): Unit = withContext(Dispatchers.IO) {
        val response = apiService.deletePost(id)
        if (!response.isSuccessful) throw Exception("Ошибка удаления: ${response.code()}")
    }

    override suspend fun createPost(
        content: String,
        attachment: Attachment?
    ): PostUi = withContext(Dispatchers.IO) {

        val request = PostRequest(
            content = content,
            attachment = attachment
        )

        val response = apiService.savePost(request)

        if (!response.isSuccessful) {
            throw HttpException(response)
        }

        val body = response.body() ?: throw RuntimeException("Empty body")

        val myId = appAuth.myId
        body.copy(
            ownedByMe = body.authorId == myId
        ).toUi(myId)
    }
}