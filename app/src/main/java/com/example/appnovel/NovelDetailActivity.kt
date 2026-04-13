package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
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
        
        binding.btnReadFirst.setOnClickListener {
            db.getChaptersByNovelId(novelId).use { cursor ->
                if (cursor.moveToFirst()) {
                    openChapter(cursor.getString(0))
                } else {
                    Toast.makeText(this, "Truyện chưa có chương nào", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnOpenRating.setOnClickListener {
            if (userId.isEmpty()) {
                Toast.makeText(this, "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("RETURN_TO_NOVEL_ID", novelId)
                startActivity(intent)
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
                binding.tvTitle.text = novel.title
                binding.tvAuthor.text = novel.author
                binding.tvDesc.text = novel.description
                binding.tvStatus.text = novel.status
                binding.tvViews.text = String.format("%,d lượt xem", novel.views)
                
                db.getChaptersByNovelId(novelId).use { cursor ->
                    binding.tvChapterCount.text = String.format("%d Chương", cursor.count)
                }
                
                Glide.with(this).load(novel.imageUrl).into(binding.imgCover)

                binding.chipGroupGenre.removeAllViews()
                for (genre in novel.genres) {
                    val chip = Chip(this)
                    chip.text = genre
                    chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#262626"))
                    chip.setTextColor(Color.WHITE)
                    chip.textSize = 12f
                    binding.chipGroupGenre.addView(chip)
                }
            }
    }

    private fun loadAverageRating() {
        firestore.collection("ratings")
            .whereEqualTo("novelId", novelId)
            .get()
            .addOnSuccessListener { docs ->
                val count = docs.size()
                binding.tvRatingCount.text = String.format("(%d)", count)
                
                if (count > 0) {
                    var totalScore = 0f
                    for (doc in docs) {
                        totalScore += doc.getDouble("score")?.toFloat() ?: 0f
                    }
                    val avg = totalScore / count
                    binding.tvRatingScore.text = String.format(Locale.US, "%.1f", avg)
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

            val ratingData = hashMapOf(
                "userId" to userId,
                "novelId" to novelId,
                "score" to rating,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("ratings")
                .document("${userId}_${novelId}")
                .set(ratingData)
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
        firestore.collection("ratings")
            .document("${userId}_${novelId}")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.btnOpenRating.text = "Sửa đánh giá"
                    val score = doc.getDouble("score")?.toFloat() ?: 0f
                    binding.ratingBarUser.rating = score
                }
            }
    }

    private fun loadChapters() {
        val cursor = db.getChaptersByNovelId(novelId)
        val chapters = mutableListOf<Chapter>()
        if (cursor.moveToFirst()) {
            do {
                chapters.add(Chapter(cursor.getString(0), novelId, cursor.getString(2) ?: "", "", cursor.getInt(4)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        binding.rvChapters.layoutManager = LinearLayoutManager(this)
        binding.rvChapters.adapter = ChapterAdapter(chapters.takeLast(3).reversed())
    }

    private fun checkFollowingStatus() {
        if (userId.isEmpty()) return
        firestore.collection("follows")
            .document("${userId}_${novelId}")
            .addSnapshotListener { snap, _ ->
                isFollowing = snap?.exists() == true
                updateFollowButtonUI()
            }
    }

    private fun updateFollowButtonUI() {
        if (isFollowing) {
            binding.btnFollow.imageTintList = ColorStateList.valueOf(Color.parseColor("#facc15"))
        } else {
            binding.btnFollow.imageTintList = ColorStateList.valueOf(Color.parseColor("#9ca3af"))
        }

        binding.btnFollow.setOnClickListener {
            toggleFollowOnCloud()
        }
    }

    private fun toggleFollowOnCloud() {
        if (userId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập để theo dõi", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("RETURN_TO_NOVEL_ID", novelId)
            startActivity(intent)
            return
        }

        val docRef = firestore.collection("follows").document("${userId}_${novelId}")
        
        if (isFollowing) {
            docRef.delete().addOnSuccessListener {
                Toast.makeText(this, "Đã bỏ theo dõi", Toast.LENGTH_SHORT).show()
            }
        } else {
            val novel = db.getAllNovels().find { it.id == novelId } ?: return
            val followData = hashMapOf(
                "userId" to userId,
                "novelId" to novelId,
                "novelTitle" to novel.title,
                "timestamp" to System.currentTimeMillis()
            )
            docRef.set(followData).addOnSuccessListener {
                Toast.makeText(this, "Đã thêm vào Tủ truyện", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class ChapterAdapter(private val list: List<Chapter>) : RecyclerView.Adapter<ChapterAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(android.R.id.text1)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val chapter = list[position]
            holder.tvTitle.text = chapter.title
            holder.tvTitle.setTextColor(Color.WHITE)
            holder.itemView.setOnClickListener { openChapter(chapter.id) }
        }
        override fun getItemCount() = list.size
    }

    private fun openChapter(id: String) {
        startActivity(Intent(this, ReadChapterActivity::class.java).putExtra("CHAPTER_ID", id).putExtra("NOVEL_ID", novelId))
    }
}
