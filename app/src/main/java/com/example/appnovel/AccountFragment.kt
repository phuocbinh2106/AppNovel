package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.appnovel.databinding.FragmentAccountBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AccountFragment : Fragment() {
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPref: SharedPreferences
    private var firestoreListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPref = requireActivity()
            .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        updateUI()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        listenCoins()
    }

    override fun onPause() {
        super.onPause()
        firestoreListener?.remove()
    }

    private fun listenCoins() {
        val userId = sharedPref.getInt("userId", -1)
        android.util.Log.d("PayOS", "listenCoins userId=$userId")
        if (userId == -1) return

        firestoreListener?.remove()
        firestoreListener = db.collection("users")
            .document(userId.toString())
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                val coins = snap.getLong("coins") ?: 0L
                sharedPref.edit().putInt("coins", coins.toInt()).apply()
                activity?.runOnUiThread {
                    binding.tvCoins.text = "${formatMoney(coins.toInt())} Xu"
                }
            }
    }

    private fun updateUI() {
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            binding.layoutGuest.visibility = View.GONE
            binding.layoutLoggedIn.visibility = View.VISIBLE

            binding.tvUsername.text =
                sharedPref.getString("username", "User")
            binding.tvEmail.text =
                sharedPref.getString("email", "")

            val coins =sharedPref.getInt("coins", 0)
            binding.tvCoins.text = "${formatMoney(coins.toInt())} Xu"
        } else {
            binding.layoutGuest.visibility = View.VISIBLE
            binding.layoutLoggedIn.visibility = View.GONE
            firestoreListener?.remove()
        }
    }

    private fun formatMoney(amount: Int) =
        String.format("%,d", amount).replace(",", ".")

    private fun setupClickListeners() {

        // Chưa đăng nhập: bấm → mở LoginActivity
        binding.btnLoginRegister.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }

        // Chưa đăng nhập: đổi màu nền
        binding.btnChangeBackground.setOnClickListener {
            // TODO: mở màn hình đổi màu nền
        }

        binding.layoutLoggedIn.findViewById<LinearLayout>(R.id.headerUser)
            .setOnClickListener {
                startActivity(Intent(requireContext(), ProfileActivity::class.java))
            }

        // Đã đăng nhập: đổi màu nền (using the correct ID from XML: btnChangeBackgroundColor)
        binding.btnChangeBackgroundColor.setOnClickListener {
            // TODO: mở màn hình đổi màu nền
        }

        // Đã đăng nhập: Nạp xu
        binding.btnNapXu.setOnClickListener {
            startActivity(
                Intent(requireContext(), NapXuActivity::class.java))
        }

        // Đã đăng nhập: Vé tháng
        binding.btnVeThang.setOnClickListener {
            // TODO
        }

        // Đã đăng nhập: Lịch sử nạp
        binding.btnLichSuNap.setOnClickListener {
            // TODO
        }

        // Đã đăng nhập: Đổi mật khẩu
        binding.btnDoiMatKhau.setOnClickListener {
            // TODO
        }

        // Đã đăng nhập: Thành viên
        binding.btnThanhVien.setOnClickListener {
            // TODO
        }

        // Đã đăng nhập: Đăng xuất
        binding.btnDangXuat.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    sharedPref.edit()
                        .putBoolean("isLoggedIn", false)
                        .remove("username")
                        .remove("email")
                        .remove("coins")
                        .apply()
                    updateUI()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        _binding = null
    }
}
