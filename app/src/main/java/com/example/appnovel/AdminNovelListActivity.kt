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

        binding.toolbar.setNavigationOnClickListener { finish() }

        currentMode = intent.getStringExtra("MODE") ?: "MANAGE_NOVEL"

        setupRecyclerView()
        loadNovelsFromFirebase()

        binding.fabAddNovel.setOnClickListener {
            val role = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("role", "")
            if (role.equals("admin", ignoreCase = true)) {
                startActivity(Intent(this, AdminAddNovelActivity::class.java))
            } else {
                Toast.makeText(this, "Chỉ Admin mới có quyền thêm truyện mới", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = sharedPrefs.getString("role", "") ?: ""
        
        // Chế độ chọn truyện để quản lý chapter
        val isSelectionMode = currentMode == "SELECT_FOR_CHAPTERS"

        adapter = AdminNovelAdapter(
            novelList = mutableListOf(),
            isSelectionMode = isSelectionMode || role.equals("uploader", ignoreCase = true), // Uploader cũng dùng SelectionMode để ẩn Sửa/Xóa
            onItemClick = { novel ->
                // Cho phép uploader/admin vào quản lý chapters
                val intent = Intent(this, AdminChapterListActivity::class.java)
                intent.putExtra("NOVEL_ID", novel.id)
                intent.putExtra("NOVEL_TITLE", novel.title)
                startActivity(intent)
            },
            onEditClick = { novel ->
                if (role.equals("admin", ignoreCase = true)) {
                    val intent = Intent(this, AdminAddNovelActivity::class.java)
                    intent.putExtra("NOVEL_DATA", novel)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Bạn không có quyền sửa thông tin truyện", Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClick = { novel ->
                if (role.equals("admin", ignoreCase = true)) {
                    showDeleteConfirmDialog(novel)
                } else {
                    Toast.makeText(this, "Chỉ Admin mới được xóa truyện", Toast.LENGTH_SHORT).show()
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

        val query = if (role.equals("admin", ignoreCase = true)) {
            firestore.collection("novels")
        } else {
            // Uploader chỉ thấy truyện mình đăng
            firestore.collection("novels").whereEqualTo("uploaderId", userId)
        }

        query.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            if (value != null) {
                val list = value.documents.mapNotNull { doc ->
                    val novel = doc.toObject(Novel::class.java)
                    if (novel != null) {
                        val finalId = if (novel.id.isEmpty()) doc.id else novel.id
                        novel.copy(id = finalId)
                    } else null
                }
                adapter.updateList(list)
            }
        }
    }

    private fun showDeleteConfirmDialog(novel: Novel) {
        AlertDialog.Builder(this)
            .setTitle("Xóa truyện")
            .setMessage("Bạn có chắc muốn xóa truyện '${novel.title}'?")
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
