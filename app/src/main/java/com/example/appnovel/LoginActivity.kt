package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val username = doc.getString("username") ?: "User"
                                val coins = (doc.getLong("coins") ?: 0L).toInt()
                                val role = doc.getString("role") ?: "user"

                                getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
                                    .putBoolean("isLoggedIn", true)
                                    .putString("userId", uid)
                                    .putString("username", username)
                                    .putString("email", email)
                                    .putString("role", role)
                                    .putInt("coins", coins)
                                    .apply()

                                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                                navigateAfterLogin()
                            }
                        }
                        .addOnFailureListener {
                            btnLogin.isEnabled = true
                            Toast.makeText(this, "Lỗi kết nối server!", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    btnLogin.isEnabled = true
                    Toast.makeText(this, "Email hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show()
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun navigateAfterLogin() {
        val returnNovelId = intent.getStringExtra("RETURN_TO_NOVEL_ID")
        
        if (!returnNovelId.isNullOrEmpty()) {
            // Quay lại trang chi tiết truyện
            val intent = Intent(this, NovelDetailActivity::class.java)
            intent.putExtra("NOVEL_ID", returnNovelId)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } else {
            // Về trang chủ
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
        finish()
    }
}
