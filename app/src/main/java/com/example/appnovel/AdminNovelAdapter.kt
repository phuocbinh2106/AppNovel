package com.example.appnovel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdminNovelAdapter(
    private var novelList: List<Novel>,
    private val onItemClick: (Novel) -> Unit, // Click vào cả dòng
    private val onEditClick: (Novel) -> Unit,
    private val onDeleteClick: (Novel) -> Unit
) : RecyclerView.Adapter<AdminNovelAdapter.NovelViewHolder>() {

    class NovelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgNovel: ImageView = view.findViewById(R.id.imgNovel)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NovelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_novel, parent, false)
        return NovelViewHolder(view)
    }

    override fun onBindViewHolder(holder: NovelViewHolder, position: Int) {
        val novel = novelList[position]
        holder.tvTitle.text = novel.title
        holder.tvAuthor.text = novel.author
        holder.tvStatus.text = novel.status

        Glide.with(holder.itemView.context)
            .load(novel.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.imgNovel)

        // Click vào cả dòng truyện
        holder.itemView.setOnClickListener { onItemClick(novel) }

        holder.btnEdit.setOnClickListener { onEditClick(novel) }
        holder.btnDelete.setOnClickListener { onDeleteClick(novel) }
    }

    override fun getItemCount(): Int = novelList.size

    fun updateList(newList: List<Novel>) {
        this.novelList = newList
        notifyDataSetChanged()
    }
}
