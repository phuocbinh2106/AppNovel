package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class NovelDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNovelDetailBinding
    private lateinit var db: DatabaseHelper
    private val firestore = FirebaseFirestore.getInstance()
    private var novelId: String = ""
    private var isFollowing = false
    private lateinit var userId: String
    private var currentNovel: Novel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovelDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)
        novelId = intent.getStringExtra("NOVEL_ID") ?: ""

        val sharedPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId = sharedPrefs.getString("userId", "") ?: ""

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        supportActionBar?.setDisplayShowTitleEnabled(false)

        increaseViewCount()
        loadNovelDetail()
        loadChapters()
        checkFollowingStatus()
        checkUserRating()
        setupRatingLogic()
        loadAverageRating()
        loadFollowCount()

        // ── Đọc từ chương đầu ──
        binding.btnReadFirst.setOnClickListener {
            db.getChaptersByNovelId(novelId).use { cursor ->
                if (cursor.moveToFirst()) {
                    openChapter(cursor.getString(0))
                } else {
                    Toast.makeText(this, "Truyện chưa có chương nào", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ── Mở danh sách chương riêng ──
        binding.btnViewAllChapters.setOnClickListener {
            startActivity(
                Intent(this, ChapterListActivity::class.java)
                    .putExtra("NOVEL_ID",    novelId)
                    .putExtra("NOVEL_TITLE", binding.tvTitle.text.toString())
                    .putExtra("NOVEL_COVER", binding.imgCover.tag?.toString() ?: "")
            )
        }

        binding.btnFollow.setOnClickListener { toggleFollowOnCloud() }

        binding.btnOpenRating.setOnClickListener {
            if (userId.isEmpty()) {
                Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java)
                    .putExtra("RETURN_TO_NOVEL_ID", novelId))
                return@setOnClickListener
            }
            binding.layoutRatingInput.visibility = View.VISIBLE
        }

        binding.btnCancelRating.setOnClickListener {
            binding.layoutRatingInput.visibility = View.GONE
        }
    }

    private fun increaseViewCount() {
        if (novelId.isEmpty()) return
        firestore.collection("novels").document(novelId)
            .update("views", FieldValue.increment(1))
    }

    private fun loadNovelDetail() {
        firestore.collection("novels").document(novelId).get()
            .addOnSuccessListener { doc ->
                val novel = doc.toObject(Novel::class.java) ?: return@addOnSuccessListener
                currentNovel = novel

                binding.tvTitle.text = novel.title
                binding.tvAuthor.text = novel.author
                binding.tvDesc.text = novel.description
                binding.tvStatus.text = novel.status
                binding.tvViews.text = formatCount(novel.views)

                db.getChaptersByNovelId(novelId).use { cursor ->
                    val count = cursor.count
                    binding.tvChapterCount.text = count.toString()
                    binding.tvChapterCountLabel.text = "$count Chương"
                    if (cursor.moveToFirst()) {
                        val firstChapterTitle = cursor.getString(2) ?: "Chương 1"
                        binding.btnReadFirst.text = firstChapterTitle
                    }
                }

                Glide.with(this).load(novel.imageUrl).into(binding.imgCover)
                binding.imgCover.tag = novel.imageUrl

                binding.chipGroupGenre.removeAllViews()
                for (genre in novel.genres) {
                    val chip = Chip(this)
                    chip.text = genre
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#262626"))
                    chip.setTextColor(Color.WHITE)
                    chip.textSize = 12f
                    binding.chipGroupGenre.addView(chip)
                }

                loadSimilarNovels(novel.genres)
            }
    }

    private fun loadAverageRating() {
        firestore.collection("ratings")
            .whereEqualTo("novelId", novelId)
            .get()
            .addOnSuccessListener { docs ->
                val count = docs.size()
                binding.tvRatingCount.text = "($count)"
                binding.tvRatingCountLabel.text = "$count lượt"
                if (count > 0) {
                    var totalScore = 0f
                    for (doc in docs) totalScore += doc.getDouble("score")?.toFloat() ?: 0f
                    binding.tvRatingScore.text = String.format(Locale.US, "%.1f", totalScore / count)
                }
            }
    }

    private fun setupRatingLogic() {
        binding.btnSubmitRating.setOnClickListener {
            val rating = binding.ratingBarUser.rating
            if (rating == 0f) {
                Toast.makeText(this, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            firestore.collection("ratings")
                .document("${userId}_${novelId}")
                .set(hashMapOf("userId" to userId, "novelId" to novelId,
                    "score" to rating, "timestamp" to System.currentTimeMillis()))
                .addOnSuccessListener {
                    Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show()
                    binding.layoutRatingInput.visibility = View.GONE
                    binding.btnOpenRating.text = "Sửa đánh giá"
                    loadAverageRating()
                }
        }
    }

    private fun checkUserRating() {
        if (userId.isEmpty()) return
        firestore.collection("ratings").document("${userId}_${novelId}").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.btnOpenRating.text = "Sửa đánh giá"
                    binding.ratingBarUser.rating = doc.getDouble("score")?.toFloat() ?: 0f
                }
            }
    }

    private fun loadChapters() {
        val cursor = db.getChaptersByNovelId(novelId)
        val chapters = mutableListOf<Chapter>()
        if (cursor.moveToFirst()) {
            do {
                chapters.add(Chapter(
                    cursor.getString(0), novelId,
                    cursor.getString(2) ?: "", "", cursor.getInt(4)
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        binding.rvChapters.layoutManager = LinearLayoutManager(this)
        // Preview 3 chương mới nhất
        binding.rvChapters.adapter = ChapterAdapter(chapters.takeLast(3).reversed())
    }

    private fun checkFollowingStatus() {
        if (userId.isEmpty()) return
        firestore.collection("follows").document("${userId}_${novelId}")
            .addSnapshotListener { snap, _ ->
                isFollowing = snap?.exists() == true
                updateFollowButtonUI()
            }
    }

    private fun updateFollowButtonUI() {
        binding.btnFollow.imageTintList = ColorStateList.valueOf(
            if (isFollowing) Color.parseColor("#facc15") else Color.parseColor("#9ca3af")
        )
    }

    private fun toggleFollowOnCloud() {
        if (userId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để theo dõi", Toast.LENGTH_SHORT).show()
            return
        }
        val docRef = firestore.collection("follows").document("${userId}_${novelId}")
        if (isFollowing) {
            docRef.delete().addOnSuccessListener {
                Toast.makeText(this, "Đã bỏ theo dõi", Toast.LENGTH_SHORT).show()
                loadFollowCount()
            }
        } else {
            val novel = currentNovel ?: return
            docRef.set(hashMapOf("userId" to userId, "novelId" to novelId,
                "novelTitle" to novel.title, "novelCover" to novel.imageUrl,
                "timestamp" to System.currentTimeMillis()))
                .addOnSuccessListener {
                    Toast.makeText(this, "Đã thêm vào Tủ truyện", Toast.LENGTH_SHORT).show()
                    loadFollowCount()
                }
        }
    }

    private fun loadFollowCount() {
        firestore.collection("follows").whereEqualTo("novelId", novelId).get()
            .addOnSuccessListener { docs ->
                binding.tvFollowCount.text = formatCount(docs.size().toLong())
            }
    }

    private fun loadSimilarNovels(genres: List<String>) {
        if (genres.isEmpty()) return
        firestore.collection("novels").whereArrayContainsAny("genres", genres).limit(6).get()
            .addOnSuccessListener { docs ->
                val list = docs.toObjects(Novel::class.java).filter { it.id != novelId }
                if (list.isNotEmpty()) {
                    binding.layoutSimilar.visibility = View.VISIBLE
                    binding.rvSimilar.layoutManager =
                        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    binding.rvSimilar.adapter = NovelHorizontalAdapter(list)
                }
            }
    }

    // ── Preview 3 chương mới — dùng item_chapter.xml có sẵn ──────────────────
    inner class ChapterAdapter(private val list: List<Chapter>) :
        RecyclerView.Adapter<ChapterAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle : TextView = view.findViewById(R.id.tvChapterTitle)
            val tvPrice : TextView = view.findViewById(R.id.tvChapterPrice)
            val tvLock  : TextView = view.findViewById(R.id.tvLock)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chapter, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val chapter = list[position]
            holder.tvTitle.text = chapter.title

            if (chapter.coinPrice > 0) {
                holder.tvPrice.text       = "${chapter.coinPrice} xu"
                holder.tvPrice.visibility = View.VISIBLE
                holder.tvLock.visibility  = View.VISIBLE
            } else {
                holder.tvPrice.visibility = View.GONE
                holder.tvLock.visibility  = View.GONE
            }

            holder.itemView.setOnClickListener { openChapter(chapter.id) }
        }

        override fun getItemCount() = list.size
    }

    private fun openChapter(id: String) {
        startActivity(
            Intent(this, ReadChapterActivity::class.java)
                .putExtra("CHAPTER_ID",   id)
                .putExtra("NOVEL_ID",     novelId)
                .putExtra("NOVEL_TITLE",  binding.tvTitle.text.toString())
                .putExtra("NOVEL_COVER",  binding.imgCover.tag?.toString() ?: "")
        )
    }

    private fun formatCount(count: Long): String = when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000     -> String.format("%.1fk", count / 1_000.0)
        else               -> count.toString()
    }
}