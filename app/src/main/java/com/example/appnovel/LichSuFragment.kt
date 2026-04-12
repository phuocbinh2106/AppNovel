package com.example.appnovel

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
import com.example.appnovel.databinding.FragmentLichSuBinding

data class LichSuItem(
    val novelId: String,
    val title: String,
    val coverUrl: String,
    val lastChapter: String,
    val lastChapterId: String,
    val timestamp: Long
)

class LichSuFragment : Fragment() {
    private var _binding: FragmentLichSuBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLichSuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadLichSu()
    }

    override fun onResume() {
        super.onResume()
        loadLichSu()
    }

    private fun loadLichSu() {
        val prefs = requireActivity()
            .getSharedPreferences("LichSuDoc", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("lich_su", "[]") ?: "[]"
        val list = parseLichSu(json)

        if (list.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = LichSuAdapter(list)
        }
    }

    private fun parseLichSu(json: String): List<LichSuItem> {
        return try {
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<LichSuItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(LichSuItem(
                    novelId = obj.getString("novelId"),
                    title = obj.getString("title"),
                    coverUrl = obj.getString("coverUrl"),
                    lastChapter = obj.getString("lastChapter"),
                    lastChapterId = obj.getString("lastChapterId"),
                    timestamp = obj.getLong("timestamp")
                ))
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    inner class LichSuAdapter(private val list: List<LichSuItem>) :
        RecyclerView.Adapter<LichSuAdapter.ViewHolder>() {

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
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}