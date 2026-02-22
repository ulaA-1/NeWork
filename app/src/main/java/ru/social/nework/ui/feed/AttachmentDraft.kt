package ru.social.nework.ui.feed

import android.net.Uri

sealed class AttachmentDraft {
    data class Image(val uri: Uri) : AttachmentDraft()
    data class Audio(val uri: Uri) : AttachmentDraft()
    data class Video(val uri: Uri) : AttachmentDraft()
}