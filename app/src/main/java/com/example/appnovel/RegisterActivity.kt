package com.example.appnovel

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        dbHelper = DatabaseHelper(this)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtRegEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtRegPassword)
        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnSubmitRegister)
        val tvLoginNow = findViewById<TextView>(R.id.tvLoginNow)

        btnBack.setOnClickListener {
            finish()
        }

        tvLoginNow.setOnClickListener {
            finish()
        }

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

            if(password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = email.substringBefore("@")

            when (dbHelper.registerUser(username, email, password)) {
                "success" -> {
                    Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                    finish()
                }
                "email_exists" -> {
                    Toast.makeText(this, "Email đã tồn tại", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Đã có lỗi xảy ra, thử lại sau", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}