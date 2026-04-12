package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
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
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            // Kiểm tra admin/uploader trong SQLite trước
            val dbHelper = DatabaseHelper(this)
            val localUser = dbHelper.loginUser(email, password)

            if (localUser != null && (localUser.role == "admin" || localUser.role == "uploader")) {
                // Đăng nhập admin/uploader qua SQLite
                getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
                    .putBoolean("isLoggedIn", true)
                    .putString("userId", localUser.id.toString())
                    .putString("username", localUser.username)
                    .putString("email", localUser.email)
                    .putString("role", localUser.role)
                    .putInt("coins", localUser.coins)
                    .apply()

                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            } else {
                // Đăng nhập user thường qua Firebase
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val username = doc.getString("username") ?: ""
                                val coins = (doc.getLong("coins") ?: 0L).toInt()

                                getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
                                    .putBoolean("isLoggedIn", true)
                                    .putString("userId", uid)
                                    .putString("username", username)
                                    .putString("email", email)
                                    .putString("role", "user")
                                    .putInt("coins", coins)
                                    .apply()

                                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                })
                                finish()
                            }
                            .addOnFailureListener {
                                btnLogin.isEnabled = true
                                Toast.makeText(this, "Lỗi lấy thông tin!", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        btnLogin.isEnabled = true
                        Toast.makeText(this, "Email hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Chức năng quên mật khẩu đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }
}