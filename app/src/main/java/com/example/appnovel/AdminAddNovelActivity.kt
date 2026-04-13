package com.example.appnovel

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AdminAddNovelActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance() // Thêm Firebase Storage

    private var selectedUploaderId: String = ""
    private var editingNovelId: String? = null

    private val genreList = listOf("Tiên Hiệp", "Huyền Huyễn", "Đô Thị", "Khoa Huyễn", "Hệ Thống", "Ngôn Tình", "Võng Du", "Hài Hước", "Linh Dị")

    private var imageUri: Uri? = null // Lưu Uri ảnh đã chọn từ điện thoại
    private var uploadedImageUrl: String = "" // Lưu link ảnh từ Firebase Storage

    private lateinit var imgNovelPreview: ImageView // Khai báo ImageView
    private lateinit var btnSave: Button

    // 1. Trình chọn ảnh từ thư viện
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageUri = uri
            imgNovelPreview.setImageURI(uri) // Hiển thị ảnh lên màn hình
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_novel)

        val tvHeader = findViewById<TextView>(R.id.tvHeaderTitle)
        val edtTitle = findViewById<EditText>(R.id.edtNovelTitle)
        val edtAuthor = findViewById<EditText>(R.id.edtNovelAuthor)
        val chipGroupGenre = findViewById<ChipGroup>(R.id.chipGroupGenre)
        val edtDesc = findViewById<EditText>(R.id.edtNovelDesc)
        val autoCompleteUploader = findViewById<AutoCompleteTextView>(R.id.autoCompleteUploader)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        btnSave = findViewById(R.id.btnAddNovel)
        imgNovelPreview = findViewById(R.id.imgNovelPreview)
        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        setupGenreChips(chipGroupGenre)

        // Sự kiện mở thư viện ảnh
        btnSelectImage.setOnClickListener {
            pickImage.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // KIỂM TRA TRẠNG THÁI EDIT
        val novel = intent.getSerializableExtra("NOVEL_DATA") as? Novel
        if (novel != null) {
            editingNovelId = novel.id
            tvHeader.text = "CHỈNH SỬA TRUYỆN"
            btnSave.text = "CẬP NHẬT TRUYỆN"
            edtTitle.setText(novel.title)
            edtAuthor.setText(novel.author)
            edtDesc.setText(novel.description)
            selectedUploaderId = novel.uploaderId
            selectExistingGenres(chipGroupGenre, novel.genres)

            // Lưu lại link ảnh cũ phòng trường hợp Admin không đổi ảnh mới
            uploadedImageUrl = novel.imageUrl

            // Ghi chú: Để hiển thị ảnh cũ lên imgNovelPreview, bạn cần dùng thư viện như Glide hoặc Picasso.
            // Nếu chưa có thư viện thì tạm thời nó sẽ hiện hình mặc định, nhưng link cũ vẫn được giữ an toàn.
        }

        loadUploaders(autoCompleteUploader)

        // XỬ LÝ NÚT LƯU
        btnSave.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            val author = edtAuthor.text.toString().trim()
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

            // Bắt buộc phải có ảnh (hoặc ảnh mới, hoặc link ảnh cũ khi Edit)
            if (imageUri == null && uploadedImageUrl.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ảnh bìa", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            Toast.makeText(this, "Đang xử lý dữ liệu...", Toast.LENGTH_SHORT).show()

            // NẾU CÓ CHỌN ẢNH MỚI -> UPLOAD LÊN STORAGE TRƯỚC
            if (imageUri != null) {
                val fileName = "covers/${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(fileName)

                storageRef.putFile(imageUri!!)
                    .addOnSuccessListener {
                        // Lấy link URL sau khi upload thành công
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            uploadedImageUrl = uri.toString()
                            // Có link ảnh rồi thì lưu vào Firestore
                            saveNovelToFirestore(title, author, desc, selectedGenres, novel?.status ?: "Đang ra")
                        }
                    }
                    .addOnFailureListener {
                        btnSave.isEnabled = true
                        Toast.makeText(this, "Lỗi khi tải ảnh lên!", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // NẾU KHÔNG ĐỔI ẢNH (Chỉ Edit chữ) -> LƯU LUÔN BẰNG LINK CŨ
                saveNovelToFirestore(title, author, desc, selectedGenres, novel?.status ?: "Đang ra")
            }
        }
    }

    // HÀM LƯU VÀO FIRESTORE (Tách ra cho gọn)
    private fun saveNovelToFirestore(title: String, author: String, desc: String, genres: List<String>, status: String) {
        val novelData = hashMapOf(
            "title" to title,
            "author" to author,
            "genres" to genres,
            "imageUrl" to uploadedImageUrl, // Dùng link ảnh vừa tạo (hoặc link cũ)
            "description" to desc,
            "status" to status,
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
                .addOnFailureListener {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show()
                }
        } else {
            val newDoc = firestore.collection("novels").document()
            novelData["id"] = newDoc.id
            // Truyện mới thì thêm timestamp tạo
            novelData["timestamp"] = System.currentTimeMillis()

            newDoc.set(novelData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Thêm truyện thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Lỗi thêm truyện!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupGenreChips(chipGroup: ChipGroup) {
        for (genre in genreList) {
            val chip = Chip(this)
            chip.text = genre
            chip.isCheckable = true

            chip.chipBackgroundColor = ContextCompat.getColorStateList(this, R.color.chip_background_selector)
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.chip_text_selector))

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
        // ... (Giữ nguyên đoạn code loadUploaders của bạn) ...
        firestore.collection("users")
            .whereIn("role", listOf("uploader", "admin"))
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Không tìm thấy Uploader nào!", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Chú ý: Đảm bảo class User của bạn đã được khai báo ở đâu đó trong app
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