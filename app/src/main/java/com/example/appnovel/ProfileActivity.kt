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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var imgAvatar: ImageView
    private lateinit var btnChangeAvatar: FrameLayout
    private lateinit var tvChangeAvatar: TextView
    private lateinit var edtUsername: android.widget.EditText
    private lateinit var tvEmail: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnDeleteAccount: MaterialButton

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
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
        toolbar.setNavigationOnClickListener { finish() }

        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val currentEmail = sharedPrefs.getString("email", "") ?: ""
        val currentUsername = sharedPrefs.getString("username", "") ?: ""
        val avatarUri = sharedPrefs.getString("avatarUri", null)

        edtUsername.setText(currentUsername)
        tvEmail.text = currentEmail
        if (!avatarUri.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(avatarUri)
                .placeholder(R.drawable.ic_account) // Thay bằng tên icon mặc định của bạn
                .error(R.drawable.ic_account)
                .circleCrop() // Tự động bo tròn ảnh
                .into(imgAvatar)
        } else {
            imgAvatar.setImageResource(R.drawable.ic_account)
        }

        val openGallery = {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }
        btnChangeAvatar.setOnClickListener { openGallery() }
        tvChangeAvatar.setOnClickListener { openGallery() }

        // Lưu username
        btnSave.setOnClickListener {
            val newUsername = edtUsername.text.toString().trim()
            if (newUsername.isEmpty()) {
                edtUsername.error = "Biệt danh không được để trống"
                return@setOnClickListener
            }

            val userId = sharedPrefs.getString("userId", null) ?: return@setOnClickListener
            btnSave.isEnabled = false

            db.collection("users").document(userId)
                .update("username", newUsername)
                .addOnSuccessListener {
                    sharedPrefs.edit().putString("username", newUsername).apply()
                    Toast.makeText(this, "Cập nhật biệt danh thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Cập nhật thất bại, thử lại sau.", Toast.LENGTH_SHORT).show()
                }
        }

        // Xóa tài khoản
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Xóa tài khoản")
                .setMessage("Bạn có chắc muốn xóa tài khoản này không?\nHành động này không thể hoàn tác!")
                .setPositiveButton("Xóa") { _, _ ->
                    val user = auth.currentUser ?: return@setPositiveButton
                    val userId = sharedPrefs.getString("userId", null) ?: return@setPositiveButton

                    // Xóa document Firestore trước
                    db.collection("users").document(userId)
                        .delete()
                        .addOnSuccessListener {
                            // Xóa tài khoản Firebase Auth
                            user.delete()
                                .addOnSuccessListener {
                                    sharedPrefs.edit().clear().apply()
                                    Toast.makeText(this, "Tài khoản đã được xóa.", Toast.LENGTH_SHORT).show()
                                    startActivity(
                                        Intent(this, MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Xóa thất bại, cần đăng nhập lại để xóa tài khoản.", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Xóa thất bại, thử lại sau.", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }
}