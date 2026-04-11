package com.example.appnovel

import android.content.Context
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appnovel.databinding.ActivityPaymentWebViewBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PaymentWebViewActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: ActivityPaymentWebViewBinding
    private var firestoreListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra("URL") ?: run { finish(); return }
        val orderId = intent.getStringExtra("ORDER_ID") ?: run { finish(); return }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val uri = request.url.toString()
                    when {

                        uri.startsWith("appnovel://payment/success") -> {
                            listenOrderPaid(orderId)
                            return true
                        }

                        uri.startsWith("appnovel://payment/cancel") -> {
                            Toast.makeText(
                                this@PaymentWebViewActivity,
                                "Đã hủy thanh toán",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                            return true
                        }
                    }
                    return false
                }
            }
            loadUrl(url)
        }
    }

    private fun listenOrderPaid(orderId: String) {
        firestoreListener = db.collection("orders")
            .document(orderId)
            .addSnapshotListener { snap, error ->
                if (error != null) return@addSnapshotListener
                if (snap != null && snap.getString("status") == "paid") {
                    val coinsAdded: Long = snap.getLong("coins") ?: 0L
                    getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("coins", coinsAdded.toInt())
                        .apply()
                    runOnUiThread { showSuccessDialog(coinsAdded) }
                }
            }
    }

    private fun showSuccessDialog(coinsAdded: Long) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nạp xu thành công!")
            .setMessage("Bạn đã nhận được ${formatMoney(coinsAdded.toInt())} Xu!")
            .setPositiveButton("OK") { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
    }

    private fun formatMoney(amount: Int) =
        String.format("%,d", amount).replace(',', '.')


}