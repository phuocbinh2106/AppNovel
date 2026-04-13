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

        Glide.with(holder.itemView.context)
            .load(currentNovel.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imgCover)

        // SỰ KIỆN CLICK: Mở trang chi tiết truyện
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, NovelDetailActivity::class.java)
            intent.putExtra("NOVEL_ID", currentNovel.id) // Truyền String ID
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = novelList.size
}
