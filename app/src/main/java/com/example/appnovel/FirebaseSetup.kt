package com.example.appnovel

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseSetup {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Danh sách tài khoản cần tạo
    private val defaultAccounts = listOf(
        User("", "Admin Novel", "admin@gmail.com", 999999, "admin"),
        User("", "Uploader 1", "uploader1@gmail.com", 0, "uploader"),
        User("", "Uploader 2", "uploader2@gmail.com", 0, "uploader"),
        User("", "Uploader 3", "uploader3@gmail.com", 0, "uploader")
    )

    fun initAdminAccounts(onComplete: (String) -> Unit) {
        var count = 0
        for (user in defaultAccounts) {
            auth.createUserWithEmailAndPassword(user.email, "123123")
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: ""
                    val userData = mapOf(
                        "id" to uid,
                        "username" to user.username,
                        "email" to user.email,
                        "coins" to user.coins,
                        "role" to user.role
                    )
                    db.collection("users").document(uid).set(userData)
                        .addOnSuccessListener {
                            count++
                            Log.d("FirebaseSetup", "Created: ${user.email}")
                            if (count == defaultAccounts.size) onComplete("Thành công: Đã tạo ${defaultAccounts.size} tài khoản")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseSetup", "Failed: ${user.email} - ${e.message}")
                    count++
                    if (count == defaultAccounts.size) onComplete("Hoàn tất (Có lỗi hoặc tài khoản đã tồn tại)")
                }
        }
    }
}
