package ru.social.nework.model

data class FeedModel<T>(
    val data: List<T> = emptyList(),
    val empty: Boolean = false,
)
