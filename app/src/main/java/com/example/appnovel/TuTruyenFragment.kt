package com.example.appnovel

import android.R.id.list
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appnovel.databinding.FragmentTuTruyenBinding
import com.google.firebase.firestore.FirebaseFirestore

data class TuTruyenItem(
    val novelId: String,
    val title: String,
    val coverUrl: String,
    val lastChapter: String
)

class TuTruyenFragment : Fragment() {
    private var _binding: FragmentTuTruyenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTuTruyenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadTuTruyen()
    }

    override fun onResume() {
        super.onResume()
        loadTuTruyen()
    }

    private fun loadTuTruyen() {
        val userId = requireActivity()
            .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("userId", "") ?: ""

        if (userId.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            return
        }

        FirebaseFirestore.getInstance()
            .collection("follows")
            .whereEqualTo("userId", userId)
            // Bỏ orderBy để không cần index
            .addSnapshotListener { value, error ->
                if (error != null) {
                    android.util.Log.e("TuTruyen", "Error: ${error.message}")
                    binding.tvEmpty.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                val list = value?.documents?.mapNotNull { doc ->
                    TuTruyenItem(
                        novelId = doc.getString("novelId") ?: return@mapNotNull null,
                        title = doc.getString("novelTitle") ?: "",
                        coverUrl = doc.getString("novelCover") ?: "",
                        lastChapter = doc.getString("lastChapter") ?: "Chưa đọc"
                    )
                } ?: emptyList()

                if (list.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                    binding.recyclerView.adapter = TuTruyenAdapter(list)
                }
            }
    }

    private fun parseTuTruyen(json: String): List<TuTruyenItem> {
        return try {
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<TuTruyenItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(TuTruyenItem(
                    novelId = obj.getString("novelId"),
                    title = obj.getString("title"),
                    coverUrl = obj.getString("coverUrl"),
                    lastChapter = obj.getString("lastChapter")
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    inner class TuTruyenAdapter(private val list: List<TuTruyenItem>) :
        RecyclerView.Adapter<TuTruyenAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgCover: ImageView = view.findViewById(R.id.imgCover)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvChapter: TextView = view.findViewById(R.id.tvChapter)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_library_novel, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvTitle.text = item.title
            holder.tvChapter.text = "Vừa đọc: ${item.lastChapter}"
            Glide.with(holder.imgCover)
                .load(item.coverUrl)
                .placeholder(R.drawable.ic_book)
                .into(holder.imgCover)

            // Thêm click để mở NovelDetailActivity
            holder.itemView.setOnClickListener {
                val intent = android.content.Intent(
                    holder.itemView.context,
                    NovelDetailActivity::class.java
                )
                intent.putExtra("NOVEL_ID", item.novelId)
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}