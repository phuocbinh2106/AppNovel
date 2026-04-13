package com.example.appnovel

import java.io.Serializable

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val novelId: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
) : Serializable
