package com.example.appnovel

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore

class AdminAddNovelActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private var selectedUploaderId: String = ""
    private var editingNovelId: String? = null
    
    private val genreList = listOf("Tiên Hiệp", "Huyền Huyễn", "Đô Thị", "Khoa Học Viễn Tưởng", "Hệ Thống", "Ngôn Tình", "Trùng Sinh", "Hài Hước", "Kinh Dị")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_novel)

        val tvHeader = findViewById<TextView>(R.id.tvHeaderTitle)
        val edtTitle = findViewById<EditText>(R.id.edtNovelTitle)
        val edtAuthor = findViewById<EditText>(R.id.edtNovelAuthor)
        val chipGroupGenre = findViewById<ChipGroup>(R.id.chipGroupGenre)
        val edtImg = findViewById<EditText>(R.id.edtNovelImageUrl)
        val edtDesc = findViewById<EditText>(R.id.edtNovelDesc)
        val autoCompleteUploader = findViewById<AutoCompleteTextView>(R.id.autoCompleteUploader)
        val btnSave = findViewById<Button>(R.id.btnAddNovel)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        setupGenreChips(chipGroupGenre)

        val novel = intent.getSerializableExtra("NOVEL_DATA") as? Novel
        if (novel != null) {
            editingNovelId = novel.id
            tvHeader.text = "CHỈNH SỬA TRUYỆN"
            btnSave.text = "CẬP NHẬT TRUYỆN"
            edtTitle.setText(novel.title)
            edtAuthor.setText(novel.author)
            edtImg.setText(novel.imageUrl)
            edtDesc.setText(novel.description)
            selectedUploaderId = novel.uploaderId
            selectExistingGenres(chipGroupGenre, novel.genres)
        }

        loadUploaders(autoCompleteUploader)

        btnSave.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val author = edtAuthor.text.toString().trim()
            val img = edtImg.text.toString().trim()
            val desc = edtDesc.text.toString().trim()
            
            val selectedGenres = mutableListOf<String>()
            for (i in 0 until chipGroupGenre.childCount) {
                val chip = chipGroupGenre.getChildAt(i) as Chip
                if (chip.isChecked) selectedGenres.add(chip.text.toString())
            }

            if (title.isEmpty() || author.isEmpty() || selectedUploaderId.isEmpty() || selectedGenres.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin và chọn ít nhất 1 thể loại", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            
            val novelData = hashMapOf(
                "title" to title,
                "author" to author,
                "genres" to selectedGenres,
                "imageUrl" to img,
                "description" to desc,
                "status" to (novel?.status ?: "Đang ra"),
                "uploaderId" to selectedUploaderId
            )

            if (editingNovelId != null) {
                novelData["id"] = editingNovelId!!
                firestore.collection("novels").document(editingNovelId!!)
                    .set(novelData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            } else {
                val newDoc = firestore.collection("novels").document()
                novelData["id"] = newDoc.id
                newDoc.set(novelData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Thêm truyện thành công!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
        }
    }

    private fun setupGenreChips(chipGroup: ChipGroup) {
        for (genre in genreList) {
            val chip = Chip(this)
            chip.text = genre
            chip.isCheckable = true
            
            // Áp dụng bộ chọn màu nền và màu chữ mới
            chip.chipBackgroundColor = ContextCompat.getColorStateList(this, R.color.chip_background_selector)
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_selector))
            
            // Thêm viền mỏng cho chip
            chip.chipStrokeWidth = 1f
            chip.chipStrokeColor = ContextCompat.getColorStateList(this, R.color.chip_stroke)
            
            chipGroup.addView(chip)
        }
    }

    private fun selectExistingGenres(chipGroup: ChipGroup, genres: List<String>) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (genres.contains(chip.text.toString())) {
                chip.isChecked = true
            }
        }
    }

    private fun loadUploaders(autoComplete: AutoCompleteTextView) {
        firestore.collection("users")
            .whereIn("role", listOf("uploader", "admin"))
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Không tìm thấy Uploader nào!", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val uploaders = documents.map { doc ->
                    User(doc.id, doc.getString("username") ?: "Không tên", doc.getString("email") ?: "", role = doc.getString("role") ?: "user")
                }
                
                val names = uploaders.map { it.username }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
                autoComplete.setAdapter(adapter)

                autoComplete.setOnItemClickListener { _, _, position, _ ->
                    selectedUploaderId = uploaders[position].id
                }

                if (editingNovelId != null) {
                    val currentUploader = uploaders.find { it.id == selectedUploaderId }
                    autoComplete.setText(currentUploader?.username, false)
                }
            }
    }
}
