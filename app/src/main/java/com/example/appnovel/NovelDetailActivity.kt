package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appnovel.databinding.ActivityNovelDetailBinding
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

class NovelDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNovelDetailBinding
    private lateinit var db: DatabaseHelper
    private val firestore = FirebaseFirestore.getInstance()
    private var novelId = -1
    private var isFollowing = false
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovelDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)
        novelId = intent.getIntExtra("NOVEL_ID", -1)
        userId = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("userId", "") ?: ""

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadNovelDetail()
        loadChapters()
        checkFollowing()
    }

    private fun loadNovelDetail() {
        val novel = db.getAllNovels().find { it.id == novelId } ?: return
        binding.tvTitle.text = novel.title
        binding.tvAuthor.text = novel.author
        binding.tvDesc.text = novel.description
        binding.tvStatus.text = novel.status
        Glide.with(this).load(novel.imageUrl).into(binding.imgCover)
        supportActionBar?.title = novel.title
    }

    private fun loadChapters() {
        val cursor = db.getChaptersByNovelId(novelId)
        val chapters = mutableListOf<Chapter>()
        if (cursor.moveToFirst()) {
            do {
                chapters.add(Chapter(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_ID)),
                    novelId = novelId,
                    title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_TITLE)),
                    content = "",
                    coinPrice = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_COIN_PRICE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()

        // Lấy danh sách chương đã mở khóa — đọc trực tiếp ở Activity
        val unlockedPrefs = getSharedPreferences("UnlockedChapters_$userId", Context.MODE_PRIVATE)
        val unlockedJson = unlockedPrefs.getString("chapters", "[]") ?: "[]"
        val unlockedIds = try {
            val arr = org.json.JSONArray(unlockedJson)
            (0 until arr.length()).map { arr.getInt(it) }
        } catch (e: Exception) { emptyList() }

        binding.rvChapters.layoutManager = LinearLayoutManager(this)
        binding.rvChapters.adapter = ChapterAdapter(chapters, unlockedIds)
    }

    private fun checkFollowing() {
        val prefs = getSharedPreferences("TuTruyen", Context.MODE_PRIVATE)
        val json = prefs.getString("tu_truyen", "[]") ?: "[]"
        val arr = JSONArray(json)
        isFollowing = (0 until arr.length()).any {
            arr.getJSONObject(it).getString("novelId") == novelId.toString()
        }
        updateFollowButton()
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            binding.btnFollow.text = "Đang theo dõi"
            binding.btnFollow.setBackgroundColor(android.graphics.Color.parseColor("#374151"))
        } else {
            binding.btnFollow.text = "Theo dõi"
            binding.btnFollow.setBackgroundColor(android.graphics.Color.parseColor("#4ade80"))
        }

        binding.btnFollow.setOnClickListener {
            val novel = db.getAllNovels().find { it.id == novelId } ?: return@setOnClickListener
            val prefs = getSharedPreferences("TuTruyen", Context.MODE_PRIVATE)
            val json = prefs.getString("tu_truyen", "[]") ?: "[]"
            val arr = JSONArray(json)

            if (isFollowing) {
                // Bỏ theo dõi
                val newArr = JSONArray()
                for (i in 0 until arr.length()) {
                    if (arr.getJSONObject(i).getString("novelId") != novelId.toString()) {
                        newArr.put(arr.getJSONObject(i))
                    }
                }
                prefs.edit().putString("tu_truyen", newArr.toString()).apply()
                isFollowing = false
                Toast.makeText(this, "Đã bỏ theo dõi", Toast.LENGTH_SHORT).show()
            } else {
                // Theo dõi
                val obj = JSONObject().apply {
                    put("novelId", novelId.toString())
                    put("title", novel.title)
                    put("coverUrl", novel.imageUrl)
                    put("lastChapter", "Chưa đọc")
                }
                arr.put(obj)
                prefs.edit().putString("tu_truyen", arr.toString()).apply()
                isFollowing = true
                Toast.makeText(this, "Đã theo dõi", Toast.LENGTH_SHORT).show()
            }
            updateFollowButton()
        }
    }

    inner class ChapterAdapter(
        private val list: List<Chapter>,
        private val unlockedIds: List<Int>
    ) : RecyclerView.Adapter<ChapterAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvChapterTitle)
            val tvPrice: TextView = view.findViewById(R.id.tvChapterPrice)
            val tvLock: TextView = view.findViewById(R.id.tvLock)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chapter, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val chapter = list[position]
            holder.tvTitle.text = chapter.title

            val isLocked = chapter.coinPrice > 0 && !unlockedIds.contains(chapter.id)

            if (isLocked) {
                holder.tvPrice.visibility = View.VISIBLE
                holder.tvPrice.text = "${chapter.coinPrice} xu"
                holder.tvLock.visibility = View.VISIBLE
            } else {
                holder.tvPrice.visibility = View.GONE
                holder.tvLock.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                if (isLocked) {
                    showUnlockDialog(chapter)
                } else {
                    openChapter(chapter)
                }
            }
        }

        override fun getItemCount() = list.size
    }

    private fun showUnlockDialog(chapter: Chapter) {
        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val currentCoins = userPrefs.getInt("coins", 0)

        if (currentCoins < chapter.coinPrice) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Không đủ xu")
                .setMessage("Bạn cần ${chapter.coinPrice} xu để mở khóa chương này.\nHiện tại bạn có $currentCoins xu.")
                .setPositiveButton("Nạp xu") { _, _ ->
                    startActivity(Intent(this, NapXuActivity::class.java))
                }
                .setNegativeButton("Hủy", null)
                .show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Mở khóa chương")
            .setMessage("Mở khóa '${chapter.title}' với ${chapter.coinPrice} xu?")
            .setPositiveButton("Mở khóa") { _, _ ->
                unlockChapter(chapter)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun unlockChapter(chapter: Chapter) {
        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val currentCoins = userPrefs.getInt("coins", 0)
        val newCoins = currentCoins - chapter.coinPrice

        // Trừ xu trong Firestore
        firestore.collection("users").document(userId)
            .update("coins", newCoins.toLong())
            .addOnSuccessListener {
                // Cập nhật local
                userPrefs.edit().putInt("coins", newCoins).apply()

                // Lưu chương đã mở khóa
                val unlockedPrefs = getSharedPreferences("UnlockedChapters_$userId", Context.MODE_PRIVATE)
                val json = unlockedPrefs.getString("chapters", "[]") ?: "[]"
                val arr = JSONArray(json)
                arr.put(chapter.id)
                unlockedPrefs.edit().putString("chapters", arr.toString()).apply()

                Toast.makeText(this, "Mở khóa thành công!", Toast.LENGTH_SHORT).show()
                openChapter(chapter)
                loadChapters() // Refresh list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi mở khóa, thử lại!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openChapter(chapter: Chapter) {
        startActivity(
            Intent(this, ReadChapterActivity::class.java)
                .putExtra("CHAPTER_ID", chapter.id)
                .putExtra("NOVEL_ID", novelId)
        )
    }
}