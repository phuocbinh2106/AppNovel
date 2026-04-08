package com.example.appnovel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NovelVerticalAdapter(private val novelList: List<Novel>) :
    RecyclerView.Adapter<NovelVerticalAdapter.NovelViewHolder>() {

    class NovelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCover: ImageView = itemView.findViewById(R.id.imgCover)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        val tvChapter: TextView = itemView.findViewById(R.id.tvChapter)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelViewHolder {
        // Gọi file giao diện dọc mà bạn đã tạo từ trước
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_novel_vertical, parent, false)
        return NovelViewHolder(view)
    }

    override fun onBindViewHolder(holder: NovelViewHolder, position: Int) {
        val currentNovel = novelList[position]

        holder.tvTitle.text = currentNovel.title
        holder.tvAuthor.text = "Tác giả: ${currentNovel.author}"
        holder.tvChapter.text = currentNovel.chapter
        holder.tvTime.text = currentNovel.time

        // Load ảnh bìa bo góc bằng Glide
        Glide.with(holder.itemView.context)
            .load(currentNovel.coverUrl)
            .into(holder.imgCover)
    }

    override fun getItemCount(): Int {
        return novelList.size
    }
}