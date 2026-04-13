package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.appnovel.databinding.FragmentAccountBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
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
        val userId = try {
            sharedPref.getString("userId", null)
        } catch (e: Exception) {
            sharedPref.edit().remove("userId").apply()
            null
        }

        if (userId.isNullOrEmpty()) return

        firestoreListener?.remove()
        firestoreListener = db.collection("users")
            .document(userId)
            .addSnapshotListener { snap, error ->
                if (error != null) return@addSnapshotListener

                if (snap != null && snap.exists()) {
                    val coins = snap.getLong("coins") ?: 0L
                    val exp = snap.getLong("exp") ?: 0L

                    // --- LẤY THÊM DỮ LIỆU VIP TỪ FIREBASE ---
                    val vipUntil = snap.getTimestamp("vipUntil")?.toDate()
                    val vipName = snap.getString("vipName") ?: "Thành Viên VIP"

                    // Lưu Xu và Exp vào máy
                    sharedPref.edit()
                        .putInt("coins", coins.toInt())
                        .putLong("exp", exp)
                        .apply()

                    activity?.runOnUiThread {
                        if (_binding != null) {
                            // 1. Cập nhật Text Xu
                            binding.tvCoins.text = "${formatMoney(coins.toInt())} Xu"

                            // === 2. LOGIC CẬP NHẬT BANNER VIP ===
                            if (vipUntil != null && vipUntil.after(java.util.Date())) {
                                // Nếu CÒN HẠN VIP
                                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                binding.tvVipTitle.text = "Gói: $vipName"
                                binding.tvVipExpiry.text = "Hiệu lực đến: ${sdf.format(vipUntil)}"
                                binding.btnBuyVip.text = "Gia hạn"
                            } else {
                                // Nếu KHÔNG CÓ VIP HOẶC ĐÃ HẾT HẠN
                                binding.tvVipTitle.text = "Mua gói VIP ngay!!"
                                binding.tvVipExpiry.text = "Hiệu lực: Đã hết hạn"
                                binding.btnBuyVip.text = "Mua thêm vé"
                            }

                            // === 3. LOGIC TÍNH CẢNH GIỚI ===
                            val currentRank = RankHelper.getCurrentRank(exp)
                            val nextRank = RankHelper.getNextRank(currentRank.level)

                            binding.tvRankName.text = currentRank.ten
                            binding.imgRankIcon.setImageResource(currentRank.iconRes)

                            if (nextRank != null) {
                                // Cập nhật text tiến trình (Ví dụ: 10 / 100)
                                binding.tvExpProgress.text = "${formatMoney(exp.toInt())} / ${formatMoney(nextRank.kn.toInt())}"

                                // Tính % cho ProgressBar
                                val expInCurrentLevel = exp - currentRank.kn
                                val expNeededForNextLevel = nextRank.kn - currentRank.kn

                                val progress = if (expNeededForNextLevel > 0) {
                                    ((expInCurrentLevel.toFloat() / expNeededForNextLevel.toFloat()) * 100).toInt()
                                } else 100

                                binding.progressRank.progress = progress
                            } else {
                                // Đã đạt cảnh giới cao nhất
                                binding.tvExpProgress.text = "Max Level"
                                binding.progressRank.progress = 100
                            }
                        }
                    }
                }
            }
    }
    private fun showChangePasswordBottomSheet() {
        // Tạo một BottomSheetDialog
        val bottomSheetDialog = BottomSheetDialog(requireContext())

        // Nạp giao diện XML của bottom sheet vào
        val view = layoutInflater.inflate(R.layout.bottom_sheet_doi_mat_khau, null)
        bottomSheetDialog.setContentView(view)

        // Ánh xạ các view BÊN TRONG bottom sheet (phải dùng view.findViewById)
        val edtOldPassword = view.findViewById<TextInputEditText>(R.id.edtOldPassword)
        val edtNewPassword = view.findViewById<TextInputEditText>(R.id.edtNewPassword)
        val edtConfirmNewPassword = view.findViewById<TextInputEditText>(R.id.edtConfirmNewPassword)
        val btnThayDoi = view.findViewById<Button>(R.id.btnThayDoi)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)

        // Sự kiện khi bấm nút "Thay đổi"
        btnThayDoi?.setOnClickListener {
            val oldPass = edtOldPassword?.text.toString().trim()
            val newPass = edtNewPassword?.text.toString().trim()
            val confirmPass = edtConfirmNewPassword?.text.toString().trim()

            // 1. Kiểm tra nhập thiếu
            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Kiểm tra mật khẩu mới có khớp nhau không
            if (newPass != confirmPass) {
                Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Gắn logic cập nhật mật khẩu lên Firebase tại đây

            Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss() // Đóng hộp thoại sau khi xong
        }

        // Sự kiện khi bấm "Quên mật khẩu?"
        tvForgotPassword?.setOnClickListener {
            Toast.makeText(requireContext(), "Chức năng lấy lại mật khẩu đang phát triển", Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }

        // Cuối cùng: Hiển thị Bottom Sheet lên
        bottomSheetDialog.show()
    }

    private fun updateUI() {
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            binding.layoutGuest.visibility = View.GONE
            binding.layoutLoggedIn.visibility = View.VISIBLE

            binding.tvUsername.text = sharedPref.getString("username", "User")
            binding.tvEmail.text = sharedPref.getString("email", "")

            val coins = sharedPref.getInt("coins", 0)
            binding.tvCoins.text = "${formatMoney(coins)} Xu"

            val role = sharedPref.getString("role", "user")
            if (role == "admin" || role == "uploader") {
                binding.btnAdminManager.visibility = View.VISIBLE
            } else {
                binding.btnAdminManager.visibility = View.GONE
            }
        } else {
            binding.layoutGuest.visibility = View.VISIBLE
            binding.layoutLoggedIn.visibility = View.GONE
            binding.btnAdminManager.visibility = View.GONE
            firestoreListener?.remove()
        }
    }

    private fun formatMoney(amount: Int) =
        String.format("%,d", amount).replace(",", ".")

    private fun setupClickListeners() {
        binding.btnLoginRegister.setOnClickListener {
            startActivity(Intent(requireActivity(), LoginActivity::class.java))
        }

        binding.layoutLoggedIn.findViewById<LinearLayout>(R.id.headerUser)
            .setOnClickListener {
                startActivity(Intent(requireActivity(), ProfileActivity::class.java))
            }

        binding.btnBuyVip.setOnClickListener {
            startActivity(Intent(requireActivity(), MonthlyPassActivity::class.java))
        }

        binding.btnVeThang.setOnClickListener {
            val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
            if (isLoggedIn) {
                startActivity(Intent(requireActivity(), MonthlyPassActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để sử dụng!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireActivity(), LoginActivity::class.java))
            }
        }

        binding.btnAdminManager.setOnClickListener {
            startActivity(Intent(requireActivity(), AdminAddActivity::class.java))
        }

        // Nút Lịch sử nạp
        binding.btnLichSuNap.setOnClickListener {
            startActivity(Intent(requireActivity(), LichSuNapActivity::class.java))
        }

        // Nút Thành viên (Cảnh giới)
        binding.btnThanhVien.setOnClickListener {
            startActivity(Intent(requireActivity(), ThanhVienActivity::class.java))
        }

        binding.btnNapXu.setOnClickListener {
            startActivity(Intent(requireActivity(), NapXuActivity::class.java))
        }

        binding.btnDoiMatKhau.setOnClickListener {
            showChangePasswordBottomSheet()
        }

        binding.btnDangXuat.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    sharedPref.edit().clear().apply()
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
