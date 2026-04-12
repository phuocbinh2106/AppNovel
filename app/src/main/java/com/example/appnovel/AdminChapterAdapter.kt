package com.example.appnovel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminChapterAdapter(
    private var chapterList: List<Chapter>,
    private val onEditClick: (Chapter) -> Unit,
    private val onDeleteClick: (Chapter) -> Unit
) : RecyclerView.Adapter<AdminChapterAdapter.ChapterViewHolder>() {

    class ChapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChapterTitle: TextView = view.findViewById(R.id.tvChapterTitle)
        val tvChapterPreview: TextView = view.findViewById(R.id.tvChapterPreview)
        val btnEdit: ImageView = view.findViewById(R.id.btnEditChapter)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteChapter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = chapterList[position]
        holder.tvChapterTitle.text = chapter.title
        holder.tvChapterPreview.text = if (chapter.content.length > 50) chapter.content.substring(0, 50) + "..." else chapter.content

        holder.btnEdit.setOnClickListener { onEditClick(chapter) }
        holder.btnDelete.setOnClickListener { onDeleteClick(chapter) }
    }

    override fun getItemCount(): Int = chapterList.size

    fun updateList(newList: List<Chapter>) {
        this.chapterList = newList
        notifyDataSetChanged()
    }
}
