package com.example.appnovel

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class AdminAddChapterActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var dbHelper: DatabaseHelper
    private var selectedNovelId: String = ""
    private var editingChapterId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_chapter)

        dbHelper = DatabaseHelper(this)

        val tvHeader = findViewById<TextView>(R.id.tvChapterHeaderTitle)
        val autoCompleteNovel = findViewById<AutoCompleteTextView>(R.id.autoCompleteNovel)
        val edtTitle = findViewById<TextInputEditText>(R.id.edtChapterTitle)
        val edtPrice = findViewById<TextInputEditText>(R.id.edtChapterPrice)
        val edtContent = findViewById<TextInputEditText>(R.id.edtChapterContent)
        val btnSave = findViewById<Button>(R.id.btnAddChapter)

        val chapter = intent.getSerializableExtra("CHAPTER_DATA") as? Chapter
        if (chapter != null) {
            editingChapterId = chapter.id
            selectedNovelId = chapter.novelId
            tvHeader.text = "CHỈNH SỬA CHAPTER"
            btnSave.text = "CẬP NHẬT CHAPTER"
            
            edtTitle.setText(chapter.title)
            edtPrice.setText(chapter.coinPrice.toString())
            edtContent.setText(chapter.content)
        }

        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = sharedPrefs.getString("role", "user") ?: "user"
        val userId = sharedPrefs.getString("userId", "") ?: ""

        loadNovelList(autoCompleteNovel, role, userId)

        btnSave.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val priceStr = edtPrice.text.toString().trim()
            val price = priceStr.toIntOrNull() ?: 0
            val content = edtContent.text.toString().trim()

            if (selectedNovelId.isEmpty() || title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            val currentTime = System.currentTimeMillis()
            
            val chapterData = hashMapOf(
                "novelId" to selectedNovelId,
                "title" to title,
                "content" to content,
                "coinPrice" to price,
                "timestamp" to (chapter?.timestamp ?: currentTime)
            )

            if (editingChapterId != null) {
                chapterData["id"] = editingChapterId!!
                firestore.collection("chapters").document(editingChapterId!!)
                    .set(chapterData)
                    .addOnSuccessListener {
                        updateNovelTimestamp(selectedNovelId, currentTime)
                        dbHelper.updateChapter(editingChapterId!!, title, content, price)
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            } else {
                val newDoc = firestore.collection("chapters").document()
                val newId = newDoc.id
                chapterData["id"] = newId
                
                newDoc.set(chapterData)
                    .addOnSuccessListener {
                        updateNovelTimestamp(selectedNovelId, currentTime)
                        dbHelper.addChapter(newId, selectedNovelId, title, content, price)
                        Toast.makeText(this, "Xuất bản thành công!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
        }
    }

    private fun updateNovelTimestamp(novelId: String, timestamp: Long) {
        firestore.collection("novels").document(novelId)
            .update("lastChapterTimestamp", timestamp)
    }

    private fun loadNovelList(autoComplete: AutoCompleteTextView, role: String, userId: String) {
        val novels = dbHelper.getNovelsByRole(role, userId)
        val titles = novels.map { it.title }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, titles)
        autoComplete.setAdapter(adapter)

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedNovelId = novels[position].id
        }

        if (editingChapterId != null) {
            val currentNovel = novels.find { it.id == selectedNovelId }
            autoComplete.setText(currentNovel?.title, false)
        }
    }
}
