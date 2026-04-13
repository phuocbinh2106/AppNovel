package com.example.appnovel

import android.content.Intent
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.main_novel_vertical, parent, false)
        return NovelViewHolder(view)
    }

    override fun onBindViewHolder(holder: NovelViewHolder, position: Int) {
        val currentNovel = novelList[position]
        holder.tvTitle.text = currentNovel.title
        holder.tvAuthor.text = "Tác giả: ${currentNovel.author}"
        holder.tvChapter.text = "Trạng thái: ${currentNovel.status}"
        
        // Cập nhật thời gian thực
        holder.tvTime.text = getTimeAgo(currentNovel.lastChapterTimestamp)

        Glide.with(holder.itemView.context)
            .load(currentNovel.imageUrl)
            .placeholder(R.color.bg_image_placeholder)
            .into(holder.imgCover)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, NovelDetailActivity::class.java)
            intent.putExtra("NOVEL_ID", currentNovel.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = novelList.size

    private fun getTimeAgo(timestamp: Long): String {
        if (timestamp == 0L) return "Chưa cập nhật"
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Vừa xong"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days < 30 -> "$days ngày trước"
            else -> {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }
}
