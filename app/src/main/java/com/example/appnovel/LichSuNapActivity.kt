package com.example.appnovel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appnovel.databinding.ActivityLichSuNapBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class TransactionItem(
    val orderId: String,
    val amount: Int,
    val coinsAdded: Int,
    val method: String,
    val date: com.google.firebase.Timestamp?
)

class LichSuNapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLichSuNapBinding
    private val db = FirebaseFirestore.getInstance()
    private val list = mutableListOf<TransactionItem>()
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLichSuNapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = TransactionAdapter(list)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadTransactions()
    }

    private fun loadTransactions() {
        val userId = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("userId", "") ?: ""

        android.util.Log.d("LichSu", "Loading transactions for userId=$userId")

        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                android.util.Log.d("LichSu", "Found ${docs.size()} transactions")
                binding.progressBar.visibility = View.GONE
                list.clear()
                for (doc in docs) {
                    android.util.Log.d("LichSu", "Doc: ${doc.data}")
                    list.add(
                        TransactionItem(
                            orderId = doc.getString("orderId") ?: "",
                            amount = (doc.getLong("amount") ?: 0).toInt(),
                            coinsAdded = (doc.getLong("coinsAdded") ?: 0).toInt(),
                            method = doc.getString("method") ?: "",
                            date = doc.getTimestamp("date")
                        )
                    )
                }
                if (list.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("LichSu", "Error: ${e.message}")
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
    }

    inner class TransactionAdapter(private val items: List<TransactionItem>) :
        RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvMethod: TextView = view.findViewById(R.id.tvMethod)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
            val tvCoins: TextView = view.findViewById(R.id.tvCoinsAdded)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvMethod.text = if (item.method == "BANK") "Ngân hàng" else "MoMo"
            holder.tvAmount.text = "-${formatMoney(item.amount)} VNĐ"
            holder.tvCoins.text = "+${formatMoney(item.coinsAdded)} Xu"
            holder.tvOrderId.text = "Mã: ${item.orderId}"
            holder.tvDate.text = item.date?.toDate()?.let {
                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(it)
            } ?: ""
        }

        override fun getItemCount() = items.size
    }
    private fun formatMoney(amount: Int) =
        String.format("%,d", amount).replace(',', '.')
}