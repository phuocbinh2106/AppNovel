package com.example.appnovel

sealed class RedeemResult {
    data class Success(val coinsAdded: Int, val newTotal:Int) : RedeemResult()
    object NOT_FOUND : RedeemResult()
    object ALREADY_USED : RedeemResult()
    object ERROR : RedeemResult()
}
