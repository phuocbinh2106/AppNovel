package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appnovel.databinding.ActivityAdminNovelListBinding
import com.google.firebase.firestore.FirebaseFirestore

class AdminNovelListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminNovelListBinding
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: AdminNovelAdapter
    private var currentMode: String? = "MANAGE_NOVEL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminNovelListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentMode = intent.getStringExtra("MODE") ?: "MANAGE_NOVEL"

        setupRecyclerView()
        loadNovelsFromFirebase()

        binding.fabAddNovel.setOnClickListener {
            val role = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("role", "")
            if (role == "admin") {
                startActivity(Intent(this, AdminAddNovelActivity::class.java))
            } else {
                Toast.makeText(this, "Chỉ Admin mới có quyền thêm truyện mới", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        // KIỂM TRA CHẾ ĐỘ CHỌN TRUYỆN
        val isSelectionMode = currentMode == "SELECT_FOR_CHAPTERS"

        adapter = AdminNovelAdapter(
            novelList = mutableListOf(),
            isSelectionMode = isSelectionMode, // Truyền chế độ vào Adapter
            onItemClick = { novel ->
                if (isSelectionMode) {
                    // Chế độ Quản lý Chapter: Click vào dòng -> Mở danh sách chapter
                    val intent = Intent(this, AdminChapterListActivity::class.java)
                    intent.putExtra("NOVEL_ID", novel.id)
                    intent.putExtra("NOVEL_TITLE", novel.title)
                    startActivity(intent)
                }
            },
            onEditClick = { novel ->
                val intent = Intent(this, AdminAddNovelActivity::class.java)
                intent.putExtra("NOVEL_DATA", novel)
                startActivity(intent)
            },
            onDeleteClick = { novel ->
                val role = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("role", "")
                if (role == "admin") {
                    showDeleteConfirmDialog(novel)
                }
            }
        )
        binding.rvAdminNovels.layoutManager = LinearLayoutManager(this)
        binding.rvAdminNovels.adapter = adapter
    }

    private fun loadNovelsFromFirebase() {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = sharedPrefs.getString("role", "user") ?: "user"
        val userId = sharedPrefs.getString("userId", "") ?: ""

        val query = if (role == "admin") {
            firestore.collection("novels")
        } else {
            firestore.collection("novels").whereEqualTo("uploaderId", userId)
        }

        query.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            if (value != null) {
                val list = value.toObjects(Novel::class.java)
                adapter.updateList(list)
            }
        }
    }

    private fun showDeleteConfirmDialog(novel: Novel) {
        AlertDialog.Builder(this)
            .setTitle("Xóa truyện")
            .setMessage("Bạn có chắc muốn xóa truyện này?")
            .setPositiveButton("Xóa") { _, _ ->
                firestore.collection("novels").document(novel.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã xóa thành công", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
