package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appnovel.databinding.ActivityChapterListBinding

class ChapterListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChapterListBinding
    private lateinit var db: DatabaseHelper

    private var novelId = ""
    private var novelTitle = ""
    private var novelCover = ""

    private var allChapters = mutableListOf<Chapter>()
    private var isAscending = true   // true = cũ → mới, false = mới → cũ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChapterListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)
        novelId    = intent.getStringExtra("NOVEL_ID") ?: ""
        novelTitle = intent.getStringExtra("NOVEL_TITLE") ?: ""
        novelCover = intent.getStringExtra("NOVEL_COVER") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadChapters()

        binding.btnToggleOrder.setOnClickListener {
            isAscending = !isAscending
            binding.btnToggleOrder.text = if (isAscending) "Đảo chương ↑" else "Đảo chương ↓"
            renderList()
        }

        binding.rvChapterList.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).also {
                it.setDrawable(getDrawable(android.R.drawable.divider_horizontal_dark)!!)
            }
        )
    }

    private fun loadChapters() {
        val cursor = db.getChaptersByNovelId(novelId)
        allChapters.clear()
        if (cursor.moveToFirst()) {
            do {
                allChapters.add(Chapter(
                    id        = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_ID)),
                    novelId   = novelId,
                    title     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_TITLE)),
                    content   = "",   // không cần content ở đây
                    coinPrice = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_COIN_PRICE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()

        binding.tvTotalChapter.text = "${allChapters.size} Chương"
        renderList()
    }

    private fun renderList() {
        val displayed = if (isAscending) allChapters else allChapters.reversed()
        binding.rvChapterList.layoutManager = LinearLayoutManager(this)
        binding.rvChapterList.adapter = ChapterListAdapter(displayed)
    }

    // ── Adapter ───────────────────────────────────────────────────────────────
    inner class ChapterListAdapter(private val list: List<Chapter>) :
        RecyclerView.Adapter<ChapterListAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle : TextView = view.findViewById(R.id.tvChapterTitle)
            val tvPrice : TextView = view.findViewById(R.id.tvChapterPrice)
            val tvLock  : TextView = view.findViewById(R.id.tvLock)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            LayoutInflater.from(parent.context).inflate(R.layout.item_chapter, parent, false)
        )

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ch = list[position]
            holder.tvTitle.text = ch.title

            if (ch.coinPrice > 0) {
                // Chapter có phí → hiện giá + icon khóa
                holder.tvPrice.text       = "${ch.coinPrice} xu"
                holder.tvPrice.visibility = View.VISIBLE
                holder.tvLock.visibility  = View.VISIBLE
            } else {
                // Chapter miễn phí → ẩn cả hai
                holder.tvPrice.visibility = View.GONE
                holder.tvLock.visibility  = View.GONE
            }

            holder.itemView.setOnClickListener {
                startActivity(
                    Intent(this@ChapterListActivity, ReadChapterActivity::class.java)
                        .putExtra("CHAPTER_ID",  ch.id)
                        .putExtra("NOVEL_ID",    novelId)
                        .putExtra("NOVEL_TITLE", novelTitle)
                        .putExtra("NOVEL_COVER", novelCover)
                )
            }
        }

        override fun getItemCount() = list.size
    }
}