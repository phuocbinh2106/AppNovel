package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appnovel.databinding.ActivityChonGoiBinding

class ChonGoiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChonGoiBinding
    private var userId = -1
    private var method = "BANK"

    private var selectedIndex = 0
    private val goiNganHang = listOf(
        Triple(50_000, 52_500, 5),
        Triple(100_000, 106_000, 6),
        Triple(200_000, 215_000, 8),
        Triple(500_000, 550_000, 10),
        Triple(1_000_000, 1_200_000, 20)
    )
    private val goiMomo = listOf(
        Triple(10_000, 10_300, 3),
        Triple(20_000, 20_800, 4),
        Triple(50_000, 52_500, 5),
        Triple(100_000, 106_000, 6),
        Triple(200_000, 215_000, 8),
        Triple(500_000, 550_000, 10),
        Triple(1_000_000, 1_200_000, 20)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChonGoiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        method = intent.getStringExtra("METHOD") ?: "BANK"
        userId = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getInt("userId", -1)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (method == "BANK") "Nạp Xu Qua Ngân Hàng" else "Nạp Xu Qua Ví Momo"
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        val danhSachGoi = if (method == "BANK") goiNganHang else goiMomo
        buildRadioOptions(danhSachGoi)

        binding.btnDoiXu.text =
            if (method == "BANK") "Đổi xu qua chuyển khoản ngân hàng"
            else "Đổi xu qua thanh toán ví Momo"

        binding.btnDoiXu.setOnClickListener {
            android.util.Log.d("PayOS", "userId=$userId, method=$method")

            val goi = danhSachGoi[selectedIndex]
            binding.btnDoiXu.isEnabled = false
            binding.btnDoiXu.text = "Đang tạo đơn..."

            val data = hashMapOf(
                "amount" to goi.first,
                "coins" to goi.second,
                "userId" to userId.toString(),
                "method" to method
            )

            com.google.firebase.functions.FirebaseFunctions
                .getInstance("us-central1")
                .getHttpsCallable("createPayosOrder")
                .call(data)
                .addOnSuccessListener { result ->
                    android.util.Log.d("PayOS", "Success: ${result.data}")
                    val map = result.data as Map<*, *>
                    val checkoutUrl = map["checkoutUrl"] as String
                    val orderId = map["orderId"] as String
                    startActivityForResult(
                        Intent(this, PaymentWebViewActivity::class.java)
                            .putExtra("URL", checkoutUrl)
                            .putExtra("ORDER_ID", orderId),
                        1001
                    )
                }
                .addOnFailureListener { e ->
                    val code = (e as? com.google.firebase.functions.FirebaseFunctionsException)?.code
                    val msg = "Code: $code | ${e.message}"
                    android.util.Log.e("PayOS", msg, e)
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
                .addOnCompleteListener {
                    android.util.Log.d("PayOS", "Complete: ${it.isSuccessful}")
                    binding.btnDoiXu.isEnabled = true
                    binding.btnDoiXu.text = if (method == "BANK")
                        "Đổi xu qua chuyển khoản ngân hàng"
                    else "Đổi xu qua ví MoMo"
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            finish()
        }
    }

    private fun buildRadioOptions(list: List<Triple<Int, Int, Int>>) {
        binding.radioGroupGoi.removeAllViews()  // tránh duplicate khi rebuild

        list.forEachIndexed { index, (vnd, xu, _) ->
            val item = LayoutInflater.from(this).inflate(R.layout.item_goi_nap, binding.radioGroupGoi, false)
            val rb = item.findViewById<RadioButton>(R.id.rbGoi)

            item.findViewById<TextView>(R.id.tvVnd).text = "${formatMoney(vnd)} VNĐ"
            item.findViewById<TextView>(R.id.tvXu).text = "${formatMoney(xu)} Xu"

            // KHÔNG set rb.id — giữ nguyên id từ XML
            rb.isChecked = (index == 0)
            rb.isClickable = false   // để item cha xử lý click
            rb.isFocusable = false
            rb.buttonTintList = ColorStateList.valueOf(Color.parseColor("#4ade80"))

            item.tag = index  // lưu index qua tag thay vì id
            item.setOnClickListener { selectItem(index) }

            binding.radioGroupGoi.addView(item)
        }
    }

    private fun selectItem(index: Int) {
        selectedIndex = index
        for (i in 0 until binding.radioGroupGoi.childCount) {
            val item = binding.radioGroupGoi.getChildAt(i)
            val rb = item.findViewById<RadioButton>(R.id.rbGoi)
            rb?.isChecked = (i == index)
        }
    }

    private fun formatMoney(amount: Int) =
        String.format("%,d", amount).replace(',', '.')
}