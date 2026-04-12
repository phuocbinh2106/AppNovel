package com.example.appnovel

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appnovel.databinding.ActivityAdminChapterListBinding

class AdminChapterListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminChapterListBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: AdminChapterAdapter
    private var selectedNovelId: Int = -1
    private var selectedNovelTitle: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminChapterListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        selectedNovelId = intent.getIntExtra("NOVEL_ID", -1)
        selectedNovelTitle = intent.getStringExtra("NOVEL_TITLE")

        binding.tvToolbarTitle.text = selectedNovelTitle?.uppercase() ?: "CHAPTERS"

        setupRecyclerView()

        binding.fabAddChapter.setOnClickListener {
            val intent = Intent(this, AdminAddChapterActivity::class.java)
            intent.putExtra("NOVEL_ID", selectedNovelId)
            startActivity(intent)
        }
        
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        if (selectedNovelId != -1) {
            loadChapters(selectedNovelId)
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminChapterAdapter(mutableListOf(),
            onEditClick = { chapter ->
                // TODO: Chức năng sửa chapter
                Toast.makeText(this, "Sửa: ${chapter.title}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { chapter ->
                showDeleteConfirmDialog(chapter)
            }
        )
        binding.rvAdminChapters.layoutManager = LinearLayoutManager(this)
        binding.rvAdminChapters.adapter = adapter
    }

    private fun loadChapters(novelId: Int) {
        val chapters = mutableListOf<Chapter>()
        val cursor = dbHelper.getChaptersByNovelId(novelId)
        if (cursor.moveToFirst()) {
            do {
                chapters.add(Chapter(
                    id = cursor.getInt(0),
                    novelId = cursor.getInt(1),
                    title = cursor.getString(2) ?: "",
                    content = cursor.getString(3) ?: ""
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        adapter.updateList(chapters)
    }

    private fun showDeleteConfirmDialog(chapter: Chapter) {
        AlertDialog.Builder(this)
            .setTitle("Xóa chapter")
            .setMessage("Bạn có chắc muốn xóa '${chapter.title}'?")
            .setPositiveButton("Xóa") { _, _ ->
                if (dbHelper.deleteChapter(chapter.id)) {
                    Toast.makeText(this, "Đã xóa thành công", Toast.LENGTH_SHORT).show()
                    loadChapters(selectedNovelId)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
