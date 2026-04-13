package com.example.appnovel

import java.io.Serializable

data class Novel(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val genres: List<String> = emptyList(),
    val imageUrl: String = "",
    val description: String = "",
    val status: String = "Đang ra",
    val uploaderId: String = "0",
    val views: Long = 0L, // Trường lượt xem
    val lastChapterTimestamp: Long = 0L // Thời gian upload chương mới nhất
) : Serializable
