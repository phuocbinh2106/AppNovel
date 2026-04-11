package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var imgAvatar : ImageView
    private lateinit var btnChangeAvatar : FrameLayout
    private lateinit var tvChangeAvatar : TextView
    private lateinit var edtUsername : android.widget.EditText
    private lateinit var tvEmail : TextView
    private lateinit var btnSave : MaterialButton
    private lateinit var btnDeleteAccount : MaterialButton

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                imgAvatar.setImageURI(it)
                getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
                    .putString("avatarUri", it.toString())
                    .apply()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        dbHelper = DatabaseHelper(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        imgAvatar = findViewById(R.id.imgAvatar)
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar)
        tvChangeAvatar = findViewById(R.id.tvChangeAvatar)
        edtUsername = findViewById(R.id.edtUsername)
        tvEmail = findViewById(R.id.tvEmail)
        btnSave = findViewById(R.id.btnSave)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val currentUsername = sharedPrefs.getString("username", "") ?: ""
        val currentEmail = sharedPrefs.getString("email", "") ?: ""
        val avatarUri = sharedPrefs.getString("avatarUri", null)

        edtUsername.setText(currentUsername)
        tvEmail.text = currentEmail
        avatarUri?.let {
            imgAvatar.setImageURI(Uri.parse(it))
        }

        val openGallery = {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }
        btnChangeAvatar.setOnClickListener {
            openGallery()
        }
        tvChangeAvatar.setOnClickListener {
            openGallery()
        }

        btnSave.setOnClickListener {
            val newUsername = edtUsername.text.toString().trim()

            if (newUsername.isEmpty()) {
                edtUsername.error = "Biệt danh không được đề trống"
                return@setOnClickListener
            }

            val success = dbHelper.updateUsername(currentEmail, newUsername)
            if (success) {
                sharedPrefs.edit()
                    .putString("username", newUsername)
                    .apply()
                Toast.makeText(this, "Cập nhật biệt danha thành công!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Cập nhật thất bại, thử lại sau.", Toast.LENGTH_SHORT).show()
            }
        }

        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa tài khoản")
                .setMessage("Bạn có chắc muốn xóa tài khoản này không?\nHành động này không thể hoàn tác!")
                .setPositiveButton("Xóa") { _, _ ->
                    val deleted = dbHelper.deleteAccount(currentEmail)
                    if (deleted) {
                        sharedPrefs.edit().clear().apply()
                        Toast.makeText(this, "Tài khoản đã được xóa.", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Xóa thất bại, thử lại sau.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }
}