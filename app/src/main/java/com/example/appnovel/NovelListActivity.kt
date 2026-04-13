package com.example.appnovel

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appnovel.databinding.ActivityNovelListBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NovelListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNovelListBinding
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo binding
        binding = ActivityNovelListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cấu hình Toolbar
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        val title = intent.getStringExtra("TITLE") ?: "DANH SÁCH TRUYỆN"
        binding.tvToolbarTitle.text = title

        setupRecyclerView()
        loadNovels()
    }

    private fun setupRecyclerView() {
        binding.rvNovelList.layoutManager = LinearLayoutManager(this)
    }

    private fun loadNovels() {
        firestore.collection("novels")
            .orderBy("lastChapterTimestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { value ->
                if (value != null) {
                    val list = value.toObjects(Novel::class.java)
                    binding.rvNovelList.adapter = NovelVerticalAdapter(list)
                }
            }
            .addOnFailureListener {
                // Xử lý lỗi nếu cần
            }
    }
}
