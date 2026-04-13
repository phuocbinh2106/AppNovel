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
    private lateinit var readPref: SharedPreferences      // ← SharedPrefs chứa setting đọc truyện
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
        sharedPref = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        readPref   = requireActivity().getSharedPreferences("ReadSettings", Context.MODE_PRIVATE)
        updateUI()
        setupClickListeners()
        setupAutoUnlockSwitch()   // ← Khởi tạo switch
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        listenCoins()
        // Đồng bộ lại trạng thái switch khi quay lại fragment
        binding.switchAutoUnlock.isChecked = readPref.getBoolean("autoUnlock", false)
    }

    override fun onPause() {
        super.onPause()
        firestoreListener?.remove()
    }

    // ── Switch tự động mở khóa ────────────────────────────────────────────────
    private fun setupAutoUnlockSwitch() {
        // Load trạng thái đã lưu
        val isAutoUnlock = readPref.getBoolean("autoUnlock", false)
        binding.switchAutoUnlock.isChecked = isAutoUnlock

        binding.switchAutoUnlock.setOnCheckedChangeListener { _, checked ->
            // Nếu bật mà chưa đăng nhập → không cho bật
            val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
            if (checked && !isLoggedIn) {
                binding.switchAutoUnlock.isChecked = false
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để sử dụng tính năng này!", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }

            // Lưu vào ReadSettings để ReadChapterActivity đọc
            readPref.edit().putBoolean("autoUnlock", checked).apply()

            val msg = if (checked)
                "Đã bật tự động mở khóa bằng xu"
            else
                "Đã tắt tự động mở khóa"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    // ── Lắng nghe coins từ Firestore realtime ─────────────────────────────────
    private fun listenCoins() {
        val userId = try {
            sharedPref.getString("userId", null)
        } catch (e: Exception) {
            sharedPref.edit().remove("userId").apply(); null
        }
        if (userId.isNullOrEmpty()) return

        firestoreListener?.remove()
        firestoreListener = db.collection("users").document(userId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null || !snap.exists()) return@addSnapshotListener

                val coins  = snap.getLong("coins") ?: 0L
                val exp    = snap.getLong("exp") ?: 0L
                val vipUntil = snap.getTimestamp("vipUntil")?.toDate()
                val vipName  = snap.getString("vipName") ?: "Thành Viên VIP"

                sharedPref.edit()
                    .putInt("coins", coins.toInt())
                    .putLong("exp", exp)
                    .apply()

                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread

                    // Xu
                    binding.tvCoins.text = "${formatMoney(coins.toInt())} Xu"

                    // Banner VIP
                    if (vipUntil != null && vipUntil.after(java.util.Date())) {
                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        binding.tvVipTitle.text  = "Gói: $vipName"
                        binding.tvVipExpiry.text = "Hiệu lực đến: ${sdf.format(vipUntil)}"
                        binding.btnBuyVip.text   = "Gia hạn"
                    } else {
                        binding.tvVipTitle.text  = "Mua gói VIP ngay!!"
                        binding.tvVipExpiry.text = "Hiệu lực: Đã hết hạn"
                        binding.btnBuyVip.text   = "Mua thêm vé"
                    }

                    // Cảnh giới / Rank
                    val currentRank = RankHelper.getCurrentRank(exp)
                    val nextRank    = RankHelper.getNextRank(currentRank.level)
                    binding.tvRankName.text = currentRank.ten
                    binding.imgRankIcon.setImageResource(currentRank.iconRes)

                    if (nextRank != null) {
                        binding.tvExpProgress.text = "${formatMoney(exp.toInt())} / ${formatMoney(nextRank.kn.toInt())}"
                        val expInLevel    = exp - currentRank.kn
                        val expForNext    = nextRank.kn - currentRank.kn
                        binding.progressRank.progress = if (expForNext > 0)
                            ((expInLevel.toFloat() / expForNext.toFloat()) * 100).toInt()
                        else 100
                    } else {
                        binding.tvExpProgress.text = "Max Level"
                        binding.progressRank.progress = 100
                    }
                }
            }
    }

    // ── UI login/logout ───────────────────────────────────────────────────────
    private fun updateUI() {
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            binding.layoutGuest.visibility    = View.GONE
            binding.layoutLoggedIn.visibility = View.VISIBLE
            binding.tvUsername.text = sharedPref.getString("username", "User")
            binding.tvEmail.text    = sharedPref.getString("email", "")
            binding.tvCoins.text    = "${formatMoney(sharedPref.getInt("coins", 0))} Xu"

            val role = sharedPref.getString("role", "user")
            binding.btnAdminManager.visibility =
                if (role == "admin" || role == "uploader") View.VISIBLE else View.GONE
        } else {
            binding.layoutGuest.visibility    = View.VISIBLE
            binding.layoutLoggedIn.visibility = View.GONE
            binding.btnAdminManager.visibility = View.GONE
            // Tắt switch khi đăng xuất
            readPref.edit().putBoolean("autoUnlock", false).apply()
            firestoreListener?.remove()
        }
    }

    private fun formatMoney(amount: Int) =
        String.format("%,d", amount).replace(",", ".")

    // ── Click listeners ───────────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.btnLoginRegister.setOnClickListener {
            startActivity(Intent(requireActivity(), LoginActivity::class.java))
        }

        binding.layoutLoggedIn.findViewById<LinearLayout>(R.id.headerUser).setOnClickListener {
            startActivity(Intent(requireActivity(), ProfileActivity::class.java))
        }

        binding.btnBuyVip.setOnClickListener {
            startActivity(Intent(requireActivity(), MonthlyPassActivity::class.java))
        }

        binding.btnVeThang.setOnClickListener {
            if (sharedPref.getBoolean("isLoggedIn", false)) {
                startActivity(Intent(requireActivity(), MonthlyPassActivity::class.java))
            } else {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireActivity(), LoginActivity::class.java))
            }
        }

        binding.btnAdminManager.setOnClickListener {
            startActivity(Intent(requireActivity(), AdminAddActivity::class.java))
        }

        binding.btnLichSuNap.setOnClickListener {
            startActivity(Intent(requireActivity(), LichSuNapActivity::class.java))
        }

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
                    readPref.edit().putBoolean("autoUnlock", false).apply()
                    updateUI()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    // ── Đổi mật khẩu ─────────────────────────────────────────────────────────
    private fun showChangePasswordBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view   = layoutInflater.inflate(R.layout.bottom_sheet_doi_mat_khau, null)
        dialog.setContentView(view)

        val edtOld     = view.findViewById<TextInputEditText>(R.id.edtOldPassword)
        val edtNew     = view.findViewById<TextInputEditText>(R.id.edtNewPassword)
        val edtConfirm = view.findViewById<TextInputEditText>(R.id.edtConfirmNewPassword)
        val btnChange  = view.findViewById<Button>(R.id.btnThayDoi)
        val tvForgot   = view.findViewById<TextView>(R.id.tvForgotPassword)

        btnChange?.setOnClickListener {
            val old     = edtOld?.text.toString().trim()
            val new_    = edtNew?.text.toString().trim()
            val confirm = edtConfirm?.text.toString().trim()

            if (old.isEmpty() || new_.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (new_ != confirm) {
                Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // TODO: cập nhật mật khẩu lên Firebase
            Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        tvForgot?.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        _binding = null
    }
}