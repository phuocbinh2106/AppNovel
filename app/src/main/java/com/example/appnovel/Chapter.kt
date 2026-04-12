package com.example.appnovel

import java.io.Serializable

data class Chapter(
    val id: Int,
    val novelId: Int,
    val title: String,
    val content: String
) : Serializable
