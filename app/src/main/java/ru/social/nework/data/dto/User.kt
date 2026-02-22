package ru.social.nework.data.dto

data class User(
    val id: Long,
    val login: String,
    val name: String,
    val avatar: String?
)