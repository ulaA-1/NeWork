package ru.social.nework.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
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
import ru.social.nework.dto.Event
import ru.social.nework.dto.Job
import ru.social.nework.dto.MediaUpload
import ru.social.nework.dto.User
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.enumeration.EventType
import ru.social.nework.error.AppError
import ru.social.nework.model.AttachmentModel
import ru.social.nework.model.FeedModelState
import ru.social.nework.repository.EventRepository
import ru.social.nework.util.AndroidUtils
import ru.social.nework.util.SingleLiveEvent
import java.io.File
import java.util.Calendar
import javax.inject.Inject

private val empty = Event(
    id = 0,
    authorId = 0,
    author = "",
    authorJob = "",
    authorAvatar = "",
    datetime = AndroidUtils.calendarToUTCDate(Calendar.getInstance()),
    published = "",
    content = "",
    likeOwnerIds = emptyList(),
    likedByMe = false,
    participantsIds = emptyList(),
    participatedByMe = false,
    speakerIds = emptyList(),
    type = EventType.ONLINE,
    users = emptyMap()
)
private val noAttachment: AttachmentModel? = null
private const val emptyDateTime = ""
private val defaultType = EventType.ONLINE

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: EventRepository,
    auth: AppAuth,
    application: Application
) : AndroidViewModel(application) {

    private val cached = repository
        .data
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: Flow<PagingData<Event>> = auth.authStateFlow
        .flatMapLatest { (myId, _) ->
            cached.map { pagingData ->
                pagingData.map { event ->
                    event.copy(ownedByMe = event.authorId == myId)
                }
            }
        }
    @OptIn(ExperimentalCoroutinesApi::class)
    val newerCount: Flow<Int> = data.flatMapLatest {
        repository.getNewerCount(repository.latestReadEventId())
            .catch { e -> throw AppError.from(e) }
            .flowOn(Dispatchers.Default)
    }

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _likers = MutableLiveData<List<User>>(emptyList())
    val likers: LiveData<List<User>>
        get() = _likers

    private val _likersLoaded = SingleLiveEvent<Unit>()
    val likersLoaded: LiveData<Unit>
        get() = _likersLoaded

    private val _speakers = MutableLiveData<List<User>>(emptyList())
    val speakers: LiveData<List<User>>
        get() = _speakers

    private val _speakersLoaded = SingleLiveEvent<Unit>()
    val speakersLoaded: LiveData<Unit>
        get() = _speakersLoaded

    private val _participants = MutableLiveData<List<User>>(emptyList())
    val participants: LiveData<List<User>>
        get() = _participants

    private val _participantsLoaded = SingleLiveEvent<Unit>()
    val participantsLoaded: LiveData<Unit>
        get() = _participantsLoaded

    val edited = MutableLiveData(empty)

    val currentEvent = MutableLiveData<Event?>()

    private val _eventCreated = SingleLiveEvent<Unit>()
    val eventCreated: LiveData<Unit>
        get() = _eventCreated

    private val _attachment = MutableLiveData(noAttachment)
    val attachment: LiveData<AttachmentModel?>
        get() = _attachment

    private val _coords = MutableLiveData<Coords?>()
    val coords: LiveData<Coords?>
        get() = _coords

    private val _speakersNewEvent = MutableLiveData<List<Long>>(emptyList())
    val speakersNewEvent: LiveData<List<Long>>
        get() = _speakersNewEvent

    private val _changed = MutableLiveData<Boolean>()
    val changed: LiveData<Boolean>
        get() = _changed

    private val _datetime = MutableLiveData(emptyDateTime)
    val datetime: LiveData<String>
        get() = _datetime

    private val _eventType = MutableLiveData(defaultType)
    val eventType: LiveData<EventType>
        get() = _eventType

    private val _lastJob = MutableLiveData<Job>()
    val lastJob: LiveData<Job>
        get() = _lastJob

    init {
        loadEvents()
    }

    fun loadEvents() = viewModelScope.launch {
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
            val newEvent = it.copy(
                datetime = _datetime.value!!,
                published = AndroidUtils.calendarToUTCDate(Calendar.getInstance()),
                coords = _coords.value,
                speakerIds = _speakersNewEvent.value!!,
                type = _eventType.value!!
            )
            _eventCreated.value = Unit
            viewModelScope.launch {
                try {
                    when (_attachment.value) {
                        null -> {
                            repository.save(newEvent.copy(attachment = null))
                        } //событие без вложения
                        else -> {
                            //редактируется событие с уже загруженным вложением
                            if (_attachment.value?.url != null) {
                                repository.save(newEvent)
                            } else {//новое событие или поменяли вложение
                                _attachment.value!!.file?.let {
                                    repository.saveWithAttachment(
                                        newEvent,
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

    fun edit(event: Event?) {
        if (event != null) {
            edited.value = event
        } else {
            clearEdit()
        }
    }

    private fun clearEdit() {
        edited.value = empty
        _attachment.value = null
        _coords.value = null
        _changed.value = false
        _datetime.value = emptyDateTime
        _eventType.value = defaultType
    }

    fun reset() {
        _changed.value = false
        currentEvent.value = null
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
            if (url != null) { //редактирование события с вложением
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

    fun changeDateTime(dateTime: String) {
        _datetime.value = dateTime
        _changed.value = true
    }

    fun changeType(eventType: EventType) {
        _eventType.value = eventType
        _changed.value = true
    }

    fun changeSpeakersNewEvent(list: List<Long>) {
        _speakersNewEvent.value = list
    }

    fun chooseUser(user: User) {
        _speakersNewEvent.value = speakersNewEvent.value?.plus(user.id)
        _changed.value = true
    }

    fun removeUser(user: User) {
        _speakersNewEvent.value = speakersNewEvent.value?.filter { it != user.id }
        _changed.value = true
    }

    fun likeById(event: Event) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.likeById(event)
            _dataState.value = FeedModelState()
            if (currentEvent.value != null) {
                getEventById(event.id)
            }
        } catch (e: Exception) {

            _dataState.value = FeedModelState(error = true)
        }
    }

    fun participateById(event: Event) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.participateById(event)
            _dataState.value = FeedModelState()
            if (currentEvent.value != null) {
                getEventById(event.id)
            }
        } catch (e: Exception) {

            _dataState.value = FeedModelState(error = true)
        }
    }

    fun removeById(event: Event) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.removeById(event)
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun getLikers(event: Event) = viewModelScope.launch {
        try {
            _likers.value = repository.getLikers(event)
            _likersLoaded.value = Unit

        } catch (e: Exception) {
            println(e.stackTrace)
        }
    }

    fun getSpeakers(event: Event) = viewModelScope.launch {
        try {
            _speakers.value = repository.getSpeakers(event)
            _speakersLoaded.value = Unit

        } catch (e: Exception) {
            println(e.stackTrace)
        }
    }

    fun getParticipants(event: Event) = viewModelScope.launch {
        try {
            _participants.value = repository.getParticipants(event)
            _participantsLoaded.value = Unit

        } catch (e: Exception) {
            println(e.stackTrace)
        }
    }

    fun readNewEvents() = viewModelScope.launch {
        repository.readNewEvents()
    }

    fun getEventById(eventId: Long) = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            currentEvent.value = repository.getEventById(eventId)
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