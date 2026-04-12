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
        // Xóa userId cũ dạng Int nếu còn
        val userId = try {
            sharedPref.getString("userId", null)
        } catch (e: ClassCastException) {
            sharedPref.edit()
                .clear()
                .apply()
            updateUI()
            return
        }

        android.util.Log.d("PayOS", "listenCoins userId=$userId")
        if (userId == null) return

        firestoreListener?.remove()
        firestoreListener = db.collection("users")
            .document(userId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) return@addSnapshotListener
                val coins = snap.getLong("coins") ?: 0L
                sharedPref.edit().putInt("coins", coins.toInt()).apply()
                activity?.runOnUiThread {
                    if (_binding != null) {
                        binding.tvCoins.text = "${formatMoney(coins.toInt())} Xu"
                    }
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

    private fun showDoiMatKhauBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_doi_mat_khau, null)
        dialog.setContentView(view)

        val edtOld = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtOldPassword)
        val edtNew = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtNewPassword)
        val edtConfirm = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edtConfirmNewPassword)
        val btnThayDoi = view.findViewById<android.widget.Button>(R.id.btnThayDoi)
        val tvForgot = view.findViewById<android.widget.TextView>(R.id.tvForgotPassword)

        tvForgot.setOnClickListener {
            val email = sharedPref.getString("email", "") ?: ""
            if (email.isEmpty()) return@setOnClickListener
            com.google.firebase.auth.FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Đã gửi email đặt lại mật khẩu!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Lỗi: ${it.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
        }

        btnThayDoi.setOnClickListener {
            val oldPass = edtOld.text.toString().trim()
            val newPass = edtNew.text.toString().trim()
            val confirmPass = edtConfirm.text.toString().trim()

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                android.widget.Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp!", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                android.widget.Toast.makeText(requireContext(), "Mật khẩu phải có ít nhất 6 ký tự", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val email = sharedPref.getString("email", "") ?: ""

            if (user == null || email.isEmpty()) return@setOnClickListener

            btnThayDoi.isEnabled = false

            // Re-authenticate trước khi đổi mật khẩu
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPass)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPass)
                        .addOnSuccessListener {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Đổi mật khẩu thành công!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            btnThayDoi.isEnabled = true
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Lỗi: ${it.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener {
                    btnThayDoi.isEnabled = true
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Mật khẩu cũ không đúng!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
        }

        dialog.show()
    }

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
            startActivity(Intent(requireContext(), LichSuNapActivity::class.java))
        }

        // Đã đăng nhập: Đổi mật khẩu
        binding.btnDoiMatKhau.setOnClickListener {
            showDoiMatKhauBottomSheet()
        }

        // Đã đăng nhập: Thành viên
        binding.btnThanhVien.setOnClickListener {
            startActivity(Intent(requireContext(), ThanhVienActivity::class.java))
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
