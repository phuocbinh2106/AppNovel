package com.example.appnovel

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appnovel.databinding.ActivityAdminChapterListBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminChapterListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminChapterListBinding
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: AdminChapterAdapter
    private var selectedNovelId: String = ""
    private var selectedNovelTitle: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminChapterListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        selectedNovelId = intent.getStringExtra("NOVEL_ID") ?: ""
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
        if (selectedNovelId.isNotEmpty()) {
            loadChaptersFromFirebase(selectedNovelId)
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminChapterAdapter(mutableListOf(),
            onEditClick = { chapter ->
                // MỞ MÀN HÌNH SỬA CHAPTER
                val intent = Intent(this, AdminAddChapterActivity::class.java)
                intent.putExtra("CHAPTER_DATA", chapter)
                startActivity(intent)
            },
            onDeleteClick = { chapter ->
                showDeleteConfirmDialog(chapter)
            }
        )
        binding.rvAdminChapters.layoutManager = LinearLayoutManager(this)
        binding.rvAdminChapters.adapter = adapter
    }

    private fun loadChaptersFromFirebase(novelId: String) {
        firestore.collection("chapters")
            .whereEqualTo("novelId", novelId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    loadChaptersFromSQLite(novelId)
                    return@addSnapshotListener
                }
                if (value != null) {
                    val chapters = value.toObjects(Chapter::class.java)
                    adapter.updateList(chapters)
                }
            }
    }

    private fun loadChaptersFromSQLite(novelId: String) {
        val chapters = mutableListOf<Chapter>()
        val cursor = dbHelper.getChaptersByNovelId(novelId)
        if (cursor.moveToFirst()) {
            do {
                chapters.add(Chapter(
                    id = cursor.getString(0),
                    novelId = cursor.getString(1),
                    title = cursor.getString(2) ?: "",
                    content = cursor.getString(3) ?: "",
                    coinPrice = cursor.getInt(4)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        adapter.updateList(chapters)
    }

    private fun showDeleteConfirmDialog(chapter: Chapter) {
        AlertDialog.Builder(this)
            .setTitle("Xóa chapter")
            .setMessage("Bạn có chắc muốn xóa '${chapter.title}' trên Cloud?")
            .setPositiveButton("Xóa") { _, _ ->
                firestore.collection("chapters").document(chapter.id).delete()
                    .addOnSuccessListener {
                        dbHelper.deleteChapter(chapter.id)
                        Toast.makeText(this, "Đã xóa thành công", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
