package com.example.appnovel

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appnovel.databinding.ActivitySearchBinding
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val novelList = mutableListOf<Novel>()
    private lateinit var adapter: NovelVerticalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = NovelVerticalAdapter(novelList)
        binding.rvSearchResult.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResult.adapter = adapter
    }

    private fun setupSearch() {
        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString().trim()
                if (keyword.isNotEmpty()) {
                    searchNovels(keyword)
                } else {
                    novelList.clear()
                    adapter.notifyDataSetChanged()
                    binding.tvNoResult.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchNovels(query: String) {
        // Firebase Firestore không hỗ trợ tìm kiếm mờ (like) mạnh như SQL
        // Ở đây ta dùng kỹ thuật tìm kiếm theo tiền tố hoặc lấy hết rồi lọc (vì list truyện không quá lớn)
        firestore.collection("novels")
            .get()
            .addOnSuccessListener { documents ->
                novelList.clear()
                val result = documents.toObjects(Novel::class.java)
                
                // Lọc dữ liệu theo tên hoặc tác giả
                val filtered = result.filter { 
                    it.title.contains(query, ignoreCase = true) || 
                    it.author.contains(query, ignoreCase = true) 
                }
                
                novelList.addAll(filtered)
                adapter.notifyDataSetChanged()
                
                binding.tvNoResult.visibility = if (novelList.isEmpty()) View.VISIBLE else View.GONE
            }
    }
}
