package com.example.appnovel

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RankingAdapter(private val list: List<Novel>) :
    RecyclerView.Adapter<RankingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRankNumber)
        val imgCover: ImageView = view.findViewById(R.id.imgCover)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvViews: TextView = view.findViewById(R.id.tvViews)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        
        holder.tvRank.text = (position + 1).toString()
        // Đổi màu top 3 cho đẹp
        when(position) {
            0 -> holder.tvRank.setTextColor(0xFFFFD700.toInt()) // Vàng
            1 -> holder.tvRank.setTextColor(0xFFC0C0C0.toInt()) // Bạc
            2 -> holder.tvRank.setTextColor(0xFFCD7F32.toInt()) // Đồng
            else -> holder.tvRank.setTextColor(0xFFAAAAAA.toInt())
        }

        holder.tvTitle.text = item.title
        holder.tvAuthor.text = item.author
        holder.tvViews.text = "${formatViews(item.views)} views"

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .placeholder(R.color.bg_image_placeholder)
            .into(holder.imgCover)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, NovelDetailActivity::class.java)
            intent.putExtra("NOVEL_ID", item.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = list.size

    private fun formatViews(views: Long): String {
        return if (views >= 1000) "${views / 1000}K" else views.toString()
    }
}
