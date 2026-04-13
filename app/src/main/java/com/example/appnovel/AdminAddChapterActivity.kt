package com.example.appnovel

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.io.BufferedReader
import java.io.InputStreamReader

class AdminAddChapterActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var dbHelper: DatabaseHelper
    private var selectedNovelId: String = ""
    private var editingChapterId: String? = null

    // 1. Khai báo trình chọn file để lấy file .txt
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            readTextFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_chapter)

        dbHelper = DatabaseHelper(this)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        val tvHeader = findViewById<TextView>(R.id.tvChapterHeaderTitle)
        val edtTitle = findViewById<TextInputEditText>(R.id.edtChapterTitle)
        val edtPrice = findViewById<TextInputEditText>(R.id.edtChapterPrice)
        val edtContent = findViewById<TextInputEditText>(R.id.edtChapterContent)
        val btnSave = findViewById<Button>(R.id.btnAddChapter)

        // Tìm nút Tải file TXT mà chúng ta vừa thêm vào giao diện
        val btnUploadTxt = findViewById<Button>(R.id.btnUploadTxt)

        // 2. Gắn sự kiện mở trình chọn file
        btnUploadTxt.setOnClickListener {
            pickFileLauncher.launch("text/plain")
        }

        // Lấy novelId từ Intent — đã được chọn từ màn hình trước
        selectedNovelId = intent.getStringExtra("NOVEL_ID") ?: ""

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

        btnSave.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val price = edtPrice.text.toString().toIntOrNull() ?: 0
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
                    .addOnFailureListener {
                        btnSave.isEnabled = true
                        Toast.makeText(this, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show()
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
                    .addOnFailureListener {
                        btnSave.isEnabled = true
                        Toast.makeText(this, "Lỗi xuất bản!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // 3. Hàm xử lý đọc chữ từ file txt và đổ vào ô Content
    private fun readTextFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream))
                val text = reader.use { it.readText() }

                // Đổ chữ vào EditText
                val edtContent = findViewById<TextInputEditText>(R.id.edtChapterContent)
                edtContent.setText(text)

                Toast.makeText(this, "Tải nội dung thành công!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khi đọc file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNovelTimestamp(novelId: String, timestamp: Long) {
        firestore.collection("novels").document(novelId)
            .update("lastChapterTimestamp", timestamp)
    }
}