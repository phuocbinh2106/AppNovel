package com.example.appnovel

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminAddNovelActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var selectedUploaderId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_novel)

        dbHelper = DatabaseHelper(this)

        val edtTitle = findViewById<EditText>(R.id.edtNovelTitle)
        val edtAuthor = findViewById<EditText>(R.id.edtNovelAuthor)
        val edtImg = findViewById<EditText>(R.id.edtNovelImageUrl)
        val edtDesc = findViewById<EditText>(R.id.edtNovelDesc)
        val spinnerUploader = findViewById<Spinner>(R.id.spinnerUploader)
        val btnAdd = findViewById<Button>(R.id.btnAddNovel)

        // Load danh sách Uploaders vào Spinner
        val uploaders = dbHelper.getAllUploaders()
        val uploaderNames = uploaders.map { it.username }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, uploaderNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUploader.adapter = adapter

        spinnerUploader.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, pos: Int, p3: Long) {
                selectedUploaderId = uploaders[pos].id
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        btnAdd.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val author = edtAuthor.text.toString().trim()
            val img = edtImg.text.toString().trim()
            val desc = edtDesc.text.toString().trim()

            if (title.isEmpty() || author.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên và tác giả", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = dbHelper.addNovel(title, author, img, desc, selectedUploaderId)
            if (result != -1L) {
                Toast.makeText(this, "Thêm truyện thành công!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Lỗi khi thêm truyện", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
