package com.example.appnovel

data class Transaction(
    val id: Int,
    val userId: Int,
    val amountVnd: Int,
    val coinsAdded: Int,
    val date: String
)
