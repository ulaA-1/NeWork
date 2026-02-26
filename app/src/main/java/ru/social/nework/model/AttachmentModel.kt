package ru.social.nework.model

import android.net.Uri
import ru.social.nework.enumeration.AttachmentType
import java.io.File

data class AttachmentModel(val url: String? = null, val uri: Uri? = null, val file: File? = null, val attachmentType: AttachmentType? = null)
