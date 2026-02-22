package ru.social.nework.util

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

fun Uri.asMultipart(context: Context, partName: String = "file"): MultipartBody.Part {
    val contentResolver = context.contentResolver

    val mime = contentResolver.getType(this) ?: "application/octet-stream"

    val bytes = contentResolver.openInputStream(this)?.use { it.readBytes() }
        ?: error("Can't open input stream for uri=$this")

    val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(
        partName,
        "upload",
        requestBody
    )
}

fun Uri.sizeBytes(context: Context): Long {
    return context.contentResolver.openFileDescriptor(this, "r")?.use { it.statSize } ?: 0L
}