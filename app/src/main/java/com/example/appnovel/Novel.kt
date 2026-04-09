package com.example.appnovel

// Đây là "khuôn mẫu" chứa dữ liệu của một cuốn truyện
data class Novel(
    val title: String,
    val author: String,
    val coverUrl: String,
    val status: String = "",      // Dùng cho phần Truyện nổi bật
    val chapter: String = "",     // Dùng cho phần Mới cập nhật
    val time: String = ""         // Dùng cho phần Mới cập nhật
)