package ru.social.nework.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.social.nework.auth.AppAuth
import ru.social.nework.dto.Coords
import ru.social.nework.dto.Job
import ru.social.nework.dto.MediaUpload
import ru.social.nework.dto.Post
import ru.social.nework.dto.User
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.error.AppError
import ru.social.nework.model.AttachmentModel
import ru.social.nework.model.FeedModelState
import ru.social.nework.repository.PostRepo
import ru.social.nework.repository.PostRepository
import ru.social.nework.util.AndroidUtils
import ru.social.nework.util.SingleLiveEvent
import java.io.File
import java.util.Calendar
import javax.inject.Inject

private val empty = Post(
    id = 0,
    authorId = 0,
    author = "",
    authorJob = "",
    authorAvatar = "",
    published = "",
    content = "",
    mentionIds = emptyList(),
    mentionedMe = false,
    likeOwnerIds = emptyList(),
    likedByMe = false,
    users = emptyMap()
)
private val noAttachment: AttachmentModel? = null

@HiltViewModel
open class PostViewModel @Inject constructor(
    @PostRepo private val repository: PostRepository,
    auth: AppAuth,
    ) : ViewModel() {

    //посты загрузились в репозиторий, сохранились в БД и попали в repository.data
    //во вьюмодели храним их с признаком ownedByMe
    @OptIn(ExperimentalCoroutinesApi::class)
    open var data: Flow<PagingData<Post>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            repository.data.map { pagingData ->
                pagingData.map { post ->
                    post.copy(ownedByMe = post.authorId == myId)
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val newerCount: Flow<Int> = data.flatMapLatest {
    repository.getNewerCount(repository.latestReadPostId())
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)
}


    private val _likers = MutableLiveData<List<User>>(emptyList())
    val likers: LiveData<List<User>>
        get() = _likers

    private val _likersLoaded = SingleLiveEvent<Unit>()
    val likersLoaded: LiveData<Unit>
        get() = _likersLoaded

    private val _mentioned = MutableLiveData<List<User>>(emptyList())
    val mentioned: LiveData<List<User>>
        get() = _mentioned

    private val _mentionedLoaded = SingleLiveEvent<Unit>()
    val mentionedLoaded: LiveData<Unit>
        get() = _mentionedLoaded

    val edited = MutableLiveData(empty)

    val currentPost = MutableLiveData<Post?>()

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated


    private val _attachment = MutableLiveData(noAttachment)
    val attachment: LiveData<AttachmentModel?>
        get() = _attachment

    private val _coords = MutableLiveData<Coords?>()
    val coords: LiveData<Coords?>
        get() = _coords

    private val _mentionedNewPost = MutableLiveData<List<Long>>(emptyList())
    val mentionedNewPost: LiveData<List<Long>>
        get() = _mentionedNewPost

    //признак, что в исходный пост внесли изменения
    private val _changed = MutableLiveData<Boolean>()
    val changed: LiveData<Boolean>
        get() = _changed

    private val _lastJob = MutableLiveData<Job>()
    val lastJob: LiveData<Job>
        get() = _lastJob


    init {
        loadPosts()
    }

    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            //repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun save() {
        edited.value?.let {
            val newPost = it.copy(
                published = AndroidUtils.calendarToUTCDate(Calendar.getInstance()),
                coords = _coords.value,
                mentionIds = _mentionedNewPost.value!!
            )
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    when (_attachment.value) {
                        null -> {
                            repository.save(newPost.copy(attachment = null))
                        } //пост без вложения
                        else -> {
                            //редактируется пост с уже загруженным вложением
                            if (_attachment.value?.url != null) {
                                repository.save(newPost)
                            } else {//новый пост или поменяли вложение
                                _attachment.value!!.file?.let {
                                    repository.saveWithAttachment(
                                        newPost,
                                        MediaUpload(_attachment.value!!.file!!),
                                        _attachment.value!!.attachmentType!!
                                    )
                                }
                            }
                        }
                    }
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    e.printStackTrace()
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        clearEdit()
    }

    fun edit(post: Post?) {
        if (post != null) {
            edited.value = post
        } else {
            clearEdit()
        }
    }

    private fun clearEdit() {
        edited.value = empty
        _attachment.value = null
        _coords.value = null
        _mentionedNewPost.value = emptyList()
        _changed.value = false

    }

    fun reset() {
        _changed.value = false
        currentPost.value = null
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
        _changed.value = true
    }

    fun changeLink(link: String) {
        val text = link.trim()
        if (edited.value?.link == text) {
            return
        }
        if (text == "") {
            edited.value = edited.value?.copy(link = null)
        } else {
            edited.value = edited.value?.copy(link = text)
        }
        _changed.value = true
    }

    fun changeAttachment(url: String?, uri: Uri?, file: File?, attachmentType: AttachmentType?) {
        if (uri == null) {
            if (url != null) { //редактирование поста с вложением
                _attachment.value = AttachmentModel(url, null, null, attachmentType)
            } else {
                _attachment.value = null //удалили вложение
            }
        } else {
            _attachment.value = AttachmentModel(null, uri, file, attachmentType)
        }
        _changed.value = true
    }

    fun changeCoords(coords: Coords?) {
        _coords.value = coords
        _changed.value = true
    }

    fun changeMentionedNewPost(list: List<Long>) {
        _mentionedNewPost.value = list
        _changed.value = true
    }

    fun chooseUser(user: User) {
        _mentionedNewPost.value = _mentionedNewPost.value?.plus(user.id)
        _changed.value = true
    }

    fun removeUser(user: User) {
        _mentionedNewPost.value = _mentionedNewPost.value?.filter { it != user.id }
        _changed.value = true
    }

    open fun likeById(post: Post) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.likeById(post)
            _dataState.value = FeedModelState()
            if (currentPost.value != null) {
                getPostById(post.id)
            }
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun removeById(post: Post) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.removeById(post)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun getLikers(post: Post) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            _likers.value = repository.getLikers(post)
            _likersLoaded.value = Unit
            _dataState.value = FeedModelState()

        } catch (e: Exception) {
            println(e.stackTrace)
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun getMentioned(post: Post) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            _mentioned.value = repository.getMentioned(post)
            _mentionedLoaded.value = Unit
            _dataState.value = FeedModelState()

        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun readNewPosts() = viewModelScope.launch {
        repository.readNewPosts()
    }

    fun getPostById(postId: Long) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            currentPost.value = repository.getPostById(postId)
            _dataState.value = FeedModelState()

        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun getLastJob(userId: Long) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            _lastJob.value = repository.getLastJob(userId)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun resetError(){
        _dataState.value = FeedModelState()
    }

}