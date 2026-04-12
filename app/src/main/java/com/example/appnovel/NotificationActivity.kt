package com.example.appnovel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appnovel.databinding.ActivityNotificationBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val notifyList = mutableListOf<Notification>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("userId", "") ?: ""

        setupRecyclerView()
        loadNotifications(userId)

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notifyList)
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun loadNotifications(userId: String) {
        if (userId.isEmpty()) return

        firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                if (value != null) {
                    notifyList.clear()
                    notifyList.addAll(value.toObjects(Notification::class.java))
                    adapter.notifyDataSetChanged()
                    binding.tvNoNotify.visibility = if (notifyList.isEmpty()) View.VISIBLE else View.GONE
                    
                    // Đánh dấu tất cả là đã đọc khi vào màn hình này
                    markAllAsRead(userId)
                }
            }
    }

    private fun markAllAsRead(userId: String) {
        firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    doc.reference.update("isRead", true)
                }
            }
    }

    inner class NotificationAdapter(private val list: List<Notification>) :
        RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(android.R.id.text1)
            val content: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.title.text = item.title
            holder.content.text = item.content
            holder.title.setTextColor(if (item.isRead) 0xFF9ca3af.toInt() else 0xFFFFFFFF.toInt())
        }

        override fun getItemCount() = list.size
    }
}
