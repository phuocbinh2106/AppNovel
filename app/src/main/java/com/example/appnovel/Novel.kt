package com.example.appnovel

import java.io.Serializable

data class Novel(
    val id: Int = 0,
    val title: String,
    val author: String,
    val imageUrl: String, // Đã thống nhất tên biến
    val description: String = "",
    val status: String = ""
) : Serializable
