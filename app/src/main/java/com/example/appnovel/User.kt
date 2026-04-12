package com.example.appnovel

import java.io.Serializable

data class User(
    val id: String = "", // Đổi sang String để khớp với Firebase UID
    val username: String = "",
    val email: String = "",
    val coins: Int = 0,
    val role: String = "user"
) : Serializable
