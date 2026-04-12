package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appnovel.databinding.ActivityAdminNovelListBinding

class AdminNovelListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminNovelListBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: AdminNovelAdapter
    private var currentMode: String? = "MANAGE_NOVEL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminNovelListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        currentMode = intent.getStringExtra("MODE") ?: "MANAGE_NOVEL"

        setupRecyclerView()

        binding.fabAddNovel.setOnClickListener {
            startActivity(Intent(this, AdminAddNovelActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadNovels()
    }

    private fun setupRecyclerView() {
        adapter = AdminNovelAdapter(mutableListOf(),
            onItemClick = { novel ->
                if (currentMode == "SELECT_FOR_CHAPTERS") {
                    // Nếu đang ở mode chọn truyện để quản lý chapter -> Mở danh sách chapter
                    val intent = Intent(this, AdminChapterListActivity::class.java)
                    intent.putExtra("NOVEL_ID", novel.id)
                    intent.putExtra("NOVEL_TITLE", novel.title)
                    startActivity(intent)
                }
            },
            onEditClick = { novel ->
                // TODO: Xử lý sửa truyện
                Toast.makeText(this, "Sửa: ${novel.title}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { novel ->
                showDeleteConfirmDialog(novel)
            }
        )
        binding.rvAdminNovels.layoutManager = LinearLayoutManager(this)
        binding.rvAdminNovels.adapter = adapter
    }

    private fun loadNovels() {
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = sharedPrefs.getString("role", "user") ?: "user"
        val userId = sharedPrefs.getInt("userId", -1)

        val novels = dbHelper.getNovelsByRole(role, userId)
        adapter.updateList(novels)
    }

    private fun showDeleteConfirmDialog(novel: Novel) {
        AlertDialog.Builder(this)
            .setTitle("Xóa truyện")
            .setMessage("Bạn có chắc chắn muốn xóa truyện '${novel.title}'?\nTất cả chapter cũng sẽ bị xóa!")
            .setPositiveButton("Xóa") { _, _ ->
                if (dbHelper.deleteNovel(novel.id)) {
                    Toast.makeText(this, "Đã xóa thành công", Toast.LENGTH_SHORT).show()
                    loadNovels()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
