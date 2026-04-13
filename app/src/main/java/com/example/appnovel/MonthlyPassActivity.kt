package com.example.appnovel

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.appnovel.databinding.ActivityMonthlyPassBinding
import com.example.appnovel.databinding.ItemVipPackageBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Date

// 1. Model dữ liệu
data class VipPackage(
    val id: String,
    val name: String,
    val shortName: String,
    val price: Int,
    val months: Int,
    val exp: Int,
    val vipLevel: Int,
    val iconRes: Int
)

class MonthlyPassActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonthlyPassBinding
    private val db = FirebaseFirestore.getInstance()

    private val packages = listOf(
        VipPackage("vip1", "GÓI THÀNH VIÊN THANH ĐỒNG", "Thanh Đồng", 100000, 1, 4000, 0,R.drawable.ic_vip_bronze),
        VipPackage("vip2", "GÓI THÀNH VIÊN BẠCH NGÂN", "Bạch Ngân", 560000, 6, 25000, 0,R.drawable.ic_vip_gold),
        VipPackage("vip3", "GÓI THÀNH VIÊN HOÀNG KIM", "Hoàng Kim", 1100000, 12, 50000, 0,R.drawable.ic_vip_platium),
        VipPackage("vip4", "GÓI THÀNH VIÊN BẠCH KIM", "Bạch Kim", 2000000, 24, 100000, 1,R.drawable.ic_vip_diamond),
        VipPackage("vip5", "GÓI THÀNH VIÊN HOÀNG ĐẾ", "Hoàng Đế", 6000000, 80, 400000, 2,R.drawable.ic_vip_master),
        VipPackage("vip6", "GÓI THÀNH VIÊN CHÍ TÔN", "Chí Tôn", 12000000, 200, 1000000, 3,R.drawable.ic_vip_chiton)
    )

    private var selectedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonthlyPassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupClickListeners()
    }

    private fun setupViewPager() {
        binding.viewPagerVip.adapter = VipAdapter(packages)

        // Hiệu ứng scale thẻ ở giữa cho giống hệt hình
        binding.viewPagerVip.offscreenPageLimit = 3
        binding.viewPagerVip.setPageTransformer { page, position ->
            val r = 1 - Math.abs(position)
            page.scaleY = 0.85f + r * 0.15f
            page.alpha = 0.5f + r * 0.5f
        }

        binding.viewPagerVip.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectedIndex = position
                updateBenefitsText(packages[position])
            }
        })
    }

    private fun updateBenefitsText(pkg: VipPackage) {
        val formatter = DecimalFormat("#,###")
        val durationStr = if(pkg.months >= 12) "${pkg.months / 12} năm" else "${pkg.months} tháng"

        val text = "Gói Thành Viên ${pkg.shortName}, được phép sử dụng các dịch vụ sau:\n" +
                "- ${formatter.format(pkg.exp)} kinh nghiệm tu luyện cộng dồn\n" +
                "- Nếu bạn là khách thập phương, update thẳng lên Vip ${pkg.vipLevel}\n" +
                "- Đọc full truyện Vip (nhỏ hơn hoặc bằng đẳng cấp vip của bạn) trong $durationStr\n" +
                "- Có thể đọc truyện full truyện vip\n" +
                "- Cho phép tải truyện về đọc offline"
        binding.tvVipBenefits.text = text
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnNapTien.setOnClickListener {
            startActivity(Intent(this, NapXuActivity::class.java))
        }

        binding.btnTiepTuc.setOnClickListener {
            showConfirmDialog(packages[selectedIndex])
        }
    }

    private fun showConfirmDialog(pkg: VipPackage) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirm_vip)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // 1. Ánh xạ các view trong Dialog
        val imgBadge = dialog.findViewById<ImageView>(R.id.imgBadgeInDialog)
        val tvName = dialog.findViewById<TextView>(R.id.tvDialogName)
        val tvDuration = dialog.findViewById<TextView>(R.id.tvDialogDuration)
        val tvBenefits = dialog.findViewById<TextView>(R.id.tvDialogBenefits)
        val tvPrice = dialog.findViewById<TextView>(R.id.tvDialogPrice)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnBuy = dialog.findViewById<Button>(R.id.btnBuy)

        // 2. Đổ dữ liệu của gói được chọn vào Dialog
        val formatter = DecimalFormat("#,###")

        // Đổi hình logo theo gói
        imgBadge.setImageResource(pkg.iconRes)
        imgBadge.imageTintList = null // Quan trọng: Xóa màu vàng mặc định để hiện màu thật của ảnh (Bronze, Silver, Gold...)

        tvName.text = pkg.shortName
        tvDuration.text = "( Hiệu lực ${pkg.months} tháng )"

        // Lấy nội dung quyền lợi đang hiển thị trên màn hình chính để đưa vào dialog
        tvBenefits.text = binding.tvVipBenefits.text

        tvPrice.text = "Giá: ${formatter.format(pkg.price)} Xu"

        // 3. Xử lý nút bấm
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnBuy.setOnClickListener {
            dialog.dismiss()
            processPurchase(pkg)
        }

        dialog.show()
    }

    private fun processPurchase(pkg: VipPackage) {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("userId", "") ?: ""
        if (userId.isEmpty()) return

        val currentCoins = sharedPref.getInt("coins", 0)

        if (currentCoins < pkg.price) {
            Toast.makeText(this, "Bạn không đủ xu! Vui lòng nạp thêm.", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentVipUntil = snapshot.getTimestamp("vipUntil")?.toDate() ?: Date()

            // Lấy EXP hiện tại của User trên Firebase (Nếu chưa có thì mặc định là 0)
            val currentExp = snapshot.getLong("exp") ?: 0L

            // Nếu vẫn còn VIP, cộng dồn tháng. Nếu hết, tính từ ngày hôm nay.
            val startFrom = if (currentVipUntil.after(Date())) currentVipUntil else Date()
            val calendar = Calendar.getInstance()
            calendar.time = startFrom
            calendar.add(Calendar.MONTH, pkg.months)

            // Cập nhật các dữ liệu cơ bản
            transaction.update(userRef, "coins", currentCoins - pkg.price)
            transaction.update(userRef, "vipUntil", Timestamp(calendar.time))
            transaction.update(userRef, "vipLevel", pkg.vipLevel)

            // --- 2 DÒNG ĐƯỢC THÊM MỚI ---
            // 1. Lưu tên gói để trang Account hiển thị chính xác (VD: "Gói: Thanh Đồng")
            transaction.update(userRef, "vipName", pkg.shortName)

            // 2. Cộng lượng EXP khổng lồ của gói VIP vào EXP hiện tại để user thăng cấp Cảnh Giới
            transaction.update(userRef, "exp", currentExp + pkg.exp)

        }.addOnSuccessListener {
            Toast.makeText(this, "Kích hoạt gói ${pkg.shortName} thành công!", Toast.LENGTH_LONG).show()
            sharedPref.edit().putInt("coins", currentCoins - pkg.price).apply()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Adapter cho ViewPager2
    inner class VipAdapter(private val items: List<VipPackage>) : RecyclerView.Adapter<VipAdapter.VipViewHolder>() {
        inner class VipViewHolder(val binding: ItemVipPackageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VipViewHolder {
            return VipViewHolder(ItemVipPackageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: VipViewHolder, position: Int) {
            val item = items[position]
            val formatter = DecimalFormat("#,###")

            holder.binding.tvName.text = item.name
            holder.binding.tvPrice.text = "${formatter.format(item.price)} xu"
            holder.binding.tvDuration.text = "( Hiệu lực ${item.months} tháng )"
            holder.binding.imgBadge.setImageResource(item.iconRes) // <--- Thêm dòng này

            // Loại bỏ tint mặc định nếu hình ảnh của bạn là ảnh màu tự nhiên
            holder.binding.imgBadge.imageTintList = null

            // Bắt sự kiện click vào thẻ để ViewPager tự cuộn tới đó
            holder.binding.root.setOnClickListener {
                binding.viewPagerVip.setCurrentItem(position, true)
            }
        }

        override fun getItemCount() = items.size
    }
}