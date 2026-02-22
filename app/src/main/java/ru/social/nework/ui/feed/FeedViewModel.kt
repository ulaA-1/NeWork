package ru.social.nework.ui.feed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.social.nework.repository.PostRepository
import javax.inject.Inject

private const val TAG = "FeedViewModel"

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _posts = MutableLiveData<List<PostUi>>()
    val posts: LiveData<List<PostUi>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        Log.d(TAG, "ViewModel создана")
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            Log.d(TAG, "loadPosts() запущен")
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Вызываем repository.getPosts()")
                val posts = repository.getPosts()
                Log.d(TAG, "repository вернул ${posts.size} постов")
                _posts.value = posts
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка: ${e.message}", e)
                _error.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "loadPosts() завершен")
            }
        }
    }

    fun likePost(post: PostUi) {
        viewModelScope.launch {
            try {
                repository.likePost(post.id)
                loadPosts()
            } catch (e: Exception) {
                _error.value = "Ошибка лайка: ${e.message}"
            }
        }
    }

    fun deletePost(post: PostUi) {
        viewModelScope.launch {
            try {
                repository.deletePost(post.id)
                loadPosts()
            } catch (e: Exception) {
                _error.value = "Ошибка удаления: ${e.message}"
            }
        }
    }
}