package com.example.appnovel

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NovelHorizontalAdapter(private val novelList: List<Novel>) :
    RecyclerView.Adapter<NovelHorizontalAdapter.NovelViewHolder>() {

    class NovelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCover: ImageView = itemView.findViewById(R.id.imgCover)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_novel_horizontal, parent, false)
        return NovelViewHolder(view)
    }

    override fun onBindViewHolder(holder: NovelViewHolder, position: Int) {
        val currentNovel = novelList[position]
        holder.tvTitle.text = currentNovel.title
        holder.tvStatus.text = currentNovel.status

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
