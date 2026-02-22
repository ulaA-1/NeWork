package ru.social.nework.ui.feed

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import ru.social.nework.repository.PostRepository
import ru.social.nework.util.sizeBytes
import javax.inject.Inject

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val repository: PostRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    data class State(
        val attachment: AttachmentDraft? = null,
        val isSaving: Boolean = false,
    )

    sealed class Event {
        object Success : Event()
        data class Error(val message: String) : Event()
    }

    private val _state = MutableLiveData(State())
    val state: LiveData<State> = _state

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    fun setAttachment(attachment: AttachmentDraft) {
        _state.value = _state.value?.copy(attachment = attachment)
    }

    fun clearAttachment() {
        _state.value = _state.value?.copy(attachment = null)
    }

    fun consumeEvent() {
        _event.value = null
    }

    fun save(text: String, onSuccessNavigateBack: () -> Unit) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            _event.value = Event.Error("empty")
            return
        }

        viewModelScope.launch {
            try {
                _state.value = _state.value?.copy(isSaving = true)

                val draft = _state.value?.attachment

                val attachmentUri: Uri? = when (draft) {
                    is AttachmentDraft.Image -> draft.uri
                    is AttachmentDraft.Audio -> draft.uri
                    is AttachmentDraft.Video -> draft.uri
                    null -> null
                }

                if (attachmentUri != null) {
                    val size = attachmentUri.sizeBytes(context)
                    if (size > 15L * 1024L * 1024L) {
                        _event.value = Event.Error("too_big")
                        _state.value = _state.value?.copy(isSaving = false)
                        return@launch
                    }
                }

                val attachmentType: String? = when (draft) {
                    is AttachmentDraft.Image -> "IMAGE"
                    is AttachmentDraft.Audio -> "AUDIO"
                    is AttachmentDraft.Video -> "VIDEO"
                    null -> null
                }

                repository.createPost(
                    content = trimmed,
                    //attachmentUri = attachmentUri,
                    //attachmentType = attachmentType
                )

                _state.value = _state.value?.copy(isSaving = false)
                _event.value = Event.Success
                onSuccessNavigateBack()
            } catch (e: Exception) {
                _state.value = _state.value?.copy(isSaving = false)
                _event.value = Event.Error(e.message ?: "unknown")
            }
        }
    }
}