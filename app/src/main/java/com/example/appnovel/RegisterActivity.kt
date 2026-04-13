package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtRegEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtRegPassword)
        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnSubmitRegister)
        val tvLoginNow = findViewById<TextView>(R.id.tvLoginNow)

        btnBack.setOnClickListener { finish() }
        tvLoginNow.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPass = edtConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ Email và Mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPass) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = email.substringBefore("@")
            btnRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    // CHUẨN BỊ DỮ LIỆU ĐẦY ĐỦ ĐỂ ĐẨY LÊN CLOUD
                    val userMap = hashMapOf(
                        "id" to uid,
                        "username" to username,
                        "email" to email,
                        "coins" to 0,
                        "role" to "user" // Mặc định tài khoản mới là user
                    )

                    db.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
                                .putBoolean("isLoggedIn", true)
                                .putString("userId", uid)
                                .putString("username", username)
                                .putString("email", email)
                                .putString("role", "user")
                                .putInt("coins", 0)
                                .apply()

                            Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                            finish()
                        }
                        .addOnFailureListener {
                            btnRegister.isEnabled = true
                            Toast.makeText(this, "Lỗi lưu dữ liệu người dùng!", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnRegister.isEnabled = true
                    Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
