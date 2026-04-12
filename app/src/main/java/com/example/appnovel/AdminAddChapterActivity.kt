package com.example.appnovel

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminAddChapterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var selectedNovelId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_chapter)

        dbHelper = DatabaseHelper(this)

        val spinnerNovel = findViewById<Spinner>(R.id.spinnerNovel)
        val edtTitle = findViewById<EditText>(R.id.edtChapterTitle)
        val edtContent = findViewById<EditText>(R.id.edtChapterContent)
        val btnAdd = findViewById<Button>(R.id.btnAddChapter)

        // Lấy thông tin user hiện tại để lọc truyện theo phân quyền
        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = sharedPrefs.getString("role", "user") ?: "user"
        val userId = sharedPrefs.getInt("userId", -1)

        // Load danh sách truyện mà người này có quyền quản lý
        val novels = dbHelper.getNovelsByRole(role, userId)
        val novelTitles = novels.map { it.title }

        if (novels.isEmpty()) {
            Toast.makeText(this, "Bạn chưa được cấp quyền quản lý bộ truyện nào", Toast.LENGTH_LONG).show()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, novelTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNovel.adapter = adapter

        spinnerNovel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, pos: Int, p3: Long) {
                selectedNovelId = novels[pos].id
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btnAdd.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val content = edtContent.text.toString().trim()

            if (selectedNovelId == -1) {
                Toast.makeText(this, "Vui lòng chọn bộ truyện", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ tên chương và nội dung", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = dbHelper.addChapter(selectedNovelId, title, content)
            if (result != -1L) {
                Toast.makeText(this, "Xuất bản chapter thành công!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Lỗi khi lưu chapter", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
