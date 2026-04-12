package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appnovel.databinding.ActivityThanhVienBinding

data class  CanhGioi(
    val level: Int,
    val ten: String,
    val kn: Long,
    val mau: String
)

class ThanhVienActivity : AppCompatActivity() {
    private lateinit var binding: ActivityThanhVienBinding

    private val danhSachCanhGioi = listOf(
        CanhGioi(36, "Hóa Thần Sơ Kỳ",       300_000_000, "#8B4513"),
        CanhGioi(35, "Xung Kích Hóa Thần",    100_000_000, "#8B4513"),
        CanhGioi(34, "Rèn Luyện Thần Thức",    80_000_000, "#8B4513"),
        CanhGioi(33, "Nhục Thân Đàn Hóa",      60_000_000, "#8B4513"),
        CanhGioi(32, "Nguyên Anh Hậu Kỳ",      50_000_000, "#6B2D6B"),
        CanhGioi(31, "Nguyên Anh Trung Kỳ",    30_000_000, "#6B2D6B"),
        CanhGioi(30, "Nguyên Anh Sơ Kỳ",       10_000_000, "#6B2D6B"),
        CanhGioi(29, "Phá Đan Ngưng Anh",      14_000_000, "#6B2D6B"),
        CanhGioi(28, "Kim Đan Đại Viên Mãn",    7_000_000, "#1a4a4a"),
        CanhGioi(27, "Kim Đan Hậu Kỳ",          5_000_000, "#1a4a4a"),
        CanhGioi(26, "Kim Đan Trung Kỳ",        3_000_000, "#1a4a4a"),
        CanhGioi(25, "Kim Đan Sơ Kỳ",           1_000_000, "#1a4a4a"),
        CanhGioi(24, "Kết Đan",                   400_000, "#4a6b3a"),
        CanhGioi(23, "Đạo Cơ Đại Viên Mãn",      300_000, "#4a6b3a"),
        CanhGioi(22, "Đạo Cơ Hậu Kỳ",            200_000, "#4a6b3a"),
        CanhGioi(21, "Đạo Cơ Trung Kỳ",          100_000, "#4a6b3a"),
        CanhGioi(20, "Đạo Cơ Sơ Kỳ",              70_000, "#1e3a5f"),
        CanhGioi(19, "Ngưng Kết Đạo Cơ",          30_000, "#1e3a5f"),
        CanhGioi(18, "Luyện Khí Tầng 12",          30_000, "#1e3a5f"),
        CanhGioi(17, "Luyện Khí Tầng 11",          27_000, "#1e3a5f"),
        CanhGioi(16, "Luyện Khí Tầng 10",          25_000, "#6b2020"),
        CanhGioi(15, "Luyện Khí Tầng 9",           20_000, "#6b2020"),
        CanhGioi(14, "Luyện Khí Tầng 8",           17_000, "#6b2020"),
        CanhGioi(13, "Luyện Khí Tầng 7",           14_000, "#6b2020"),
        CanhGioi(12, "Luyện Khí Tầng 6",           10_000, "#1e4a2a"),
        CanhGioi(11, "Luyện Khí Tầng 5",            8_000, "#1e4a2a"),
        CanhGioi(10, "Luyện Khí Tầng 4",            7_000, "#1e4a2a"),
        CanhGioi(9,  "Luyện Khí Tầng 3",            5_200, "#1e4a2a"),
        CanhGioi(8,  "Luyện Khí Tầng 2",            2_600, "#1e3a5f"),
        CanhGioi(7,  "Luyện Khí Tầng 1",            1_600, "#1e3a5f"),
        CanhGioi(6,  "Tiên Thiên Võ Giả",           1_200, "#1e3a5f"),
        CanhGioi(5,  "Hậu Thiên Võ Giả",            1_200, "#1e3a5f"),
        CanhGioi(4,  "Nhất Lưu Võ Giả",               800, "#1e3a5f"),
        CanhGioi(3,  "Nhị Lưu Võ Giả",                500, "#1e3a5f"),
        CanhGioi(2,  "Tam Lưu Võ Giả",                300, "#1e3a5f"),
        CanhGioi(1,  "Bắt Nhập Lưu Võ Giả",           100, "#1a1a2e")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThanhVienBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "User") ?: "User"
        val email = sharedPref.getString("email", "") ?: ""
        val coins = sharedPref.getInt("coins", 0)

        binding.tvUsername.text = username
        binding.tvEmail.text = email
        binding.tvCoins.text = formatMoney(coins)

        binding.btnNapThem.setOnClickListener {
            startActivity(Intent(this, NapXuActivity::class.java))
        }

        // Tìm cảnh giới hiện tại dựa theo coins
        val currentLevel = danhSachCanhGioi
            .filter { coins >= it.kn }
            .maxByOrNull { it.kn }
            ?: danhSachCanhGioi.last()

        val adapter = CanhGioiAdapter(danhSachCanhGioi, currentLevel.level)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Scroll tới cảnh giới hiện tại
        val currentPos = danhSachCanhGioi.indexOfFirst { it.level == currentLevel.level }
        if (currentPos != -1) {
            binding.recyclerView.post {
                binding.recyclerView.scrollToPosition(currentPos)
            }
        }
    }

    inner class CanhGioiAdapter(
        private val list: List<CanhGioi>,
        private val currentLevel: Int
    ) : RecyclerView.Adapter<CanhGioiAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvLevel: TextView = view.findViewById(R.id.tvLevel)
            val tvTen: TextView = view.findViewById(R.id.tvTen)
            val tvKn: TextView = view.findViewById(R.id.tvKn)
            val container: View = view.findViewById(R.id.container)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_canh_gioi, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvLevel.text = item.level.toString()
            holder.tvTen.text = item.ten
            holder.tvKn.text = "KN : ${formatMoney(item.kn.toInt())}"

            val bgColor = when {
                item.level <= 4 -> "#2a2a2a"
                else -> item.mau
            }

            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(android.graphics.Color.parseColor(bgColor))

                if (item.level == currentLevel) {
                    // Viền trắng highlight
                    setStroke(4, android.graphics.Color.WHITE)
                } else {
                    setStroke(0, android.graphics.Color.TRANSPARENT)
                }
            }
            holder.container.background = drawable

            if (item.level == currentLevel) {
                holder.tvTen.setTypeface(null, android.graphics.Typeface.BOLD)
                holder.tvLevel.setTypeface(null, android.graphics.Typeface.BOLD)
                holder.tvKn.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                holder.tvTen.setTypeface(null, android.graphics.Typeface.NORMAL)
                holder.tvLevel.setTypeface(null, android.graphics.Typeface.NORMAL)
                holder.tvKn.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }

        override fun getItemCount() = list.size
    }

    private fun formatMoney(amount: Int) =
        String.format("%,d", amount).replace(',', '.')

    private fun formatMoney(amount: Long) =
        String.format("%,d", amount).replace(',', '.')
}