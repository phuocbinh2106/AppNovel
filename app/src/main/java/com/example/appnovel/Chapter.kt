package com.example.appnovel

import java.io.Serializable

data class Chapter(
    val id: String = "",      // Đổi sang String
    val novelId: String = "", // Đổi sang String
    val title: String = "",
    val content: String = "",
    val coinPrice: Int = 0,
    val timestamp: Long = 0L  // Thêm để sắp xếp chương
) : Serializable
