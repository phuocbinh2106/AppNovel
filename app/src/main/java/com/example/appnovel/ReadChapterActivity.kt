package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.appnovel.databinding.ActivityReadChapterBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

class ReadChapterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReadChapterBinding
    private lateinit var db: DatabaseHelper
    private val firestore = FirebaseFirestore.getInstance()

    private var novelTitle = ""
    private var novelCover = ""
    private var chapterId = ""
    private var novelId = ""
    private var chapterList = listOf<Chapter>()
    private var currentIndex = 0
    private var userId = ""
    private var userCoins = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadChapterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)
        chapterId  = intent.getStringExtra("CHAPTER_ID") ?: ""
        novelId    = intent.getStringExtra("NOVEL_ID") ?: ""
        novelTitle = intent.getStringExtra("NOVEL_TITLE") ?: ""
        novelCover = intent.getStringExtra("NOVEL_COVER") ?: ""

        val prefs  = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId     = prefs.getString("userId", "") ?: ""
        userCoins  = prefs.getInt("coins", 0)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadChapterList()
        setupNavigation()
        tryOpenChapter(chapterId)
    }

    // ── Tải danh sách chapter từ SQLite local ─────────────────────────────────
    private fun loadChapterList() {
        val cursor = db.getChaptersByNovelId(novelId)
        val list = mutableListOf<Chapter>()
        if (cursor.moveToFirst()) {
            do {
                list.add(Chapter(
                    id        = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_ID)),
                    novelId   = novelId,
                    title     = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_TITLE)),
                    content   = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_CONTENT)),
                    coinPrice = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_COIN_PRICE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        chapterList  = list
        currentIndex = list.indexOfFirst { it.id == chapterId }
    }

    // ── Điểm vào: kiểm tra khóa trước khi mở ─────────────────────────────────
    private fun tryOpenChapter(id: String) {
        val chapter = chapterList.find { it.id == id } ?: return
        currentIndex = chapterList.indexOf(chapter)

        // Miễn phí → mở luôn
        if (chapter.coinPrice <= 0) {
            displayChapter(chapter)
            return
        }

        // Chưa đăng nhập
        if (userId.isEmpty()) {
            showMustLoginDialog(chapter)
            return
        }

        // Kiểm tra Firestore đã mua chưa
        firestore.collection("purchases")
            .document("${userId}_${chapter.id}")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) displayChapter(chapter)
                else showPurchaseDialog(chapter)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi kết nối, thử lại sau!", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Dialog: chưa đăng nhập ────────────────────────────────────────────────
    private fun showMustLoginDialog(chapter: Chapter) {
        AlertDialog.Builder(this)
            .setTitle("🔒 Chương có phí")
            .setMessage("Chương này cần ${chapter.coinPrice} xu.\nVui lòng đăng nhập để tiếp tục.")
            .setPositiveButton("Đăng nhập") { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("Hủy") { _, _ -> goBackOrFinish() }
            .setCancelable(false)
            .show()
    }

    // ── Dialog: xác nhận mua ──────────────────────────────────────────────────
    private fun showPurchaseDialog(chapter: Chapter) {
        val canAfford = userCoins >= chapter.coinPrice
        val message = buildString {
            append("Chương này cần ${chapter.coinPrice} xu để đọc.\n")
            append("Xu của bạn: $userCoins xu.\n\n")
            if (canAfford)
                append("Bạn có muốn dùng ${chapter.coinPrice} xu để mở khóa không?")
            else
                append("Bạn không đủ xu. Hãy nạp thêm để đọc chương này.")
        }

        AlertDialog.Builder(this)
            .setTitle("🔒 Chương có phí")
            .setMessage(message)
            .setPositiveButton(if (canAfford) "Mở khóa" else "Nạp xu") { _, _ ->
                if (canAfford) purchaseChapter(chapter)
                else {
                    // TODO: mở màn hình nạp xu
                    Toast.makeText(this, "Tính năng nạp xu sắp ra mắt!", Toast.LENGTH_SHORT).show()
                    goBackOrFinish()
                }
            }
            .setNegativeButton("Hủy") { _, _ -> goBackOrFinish() }
            .setCancelable(false)
            .show()
    }

    // ── Trừ xu Firestore + SharedPrefs, lưu purchase ─────────────────────────
    private fun purchaseChapter(chapter: Chapter) {
        val userRef     = firestore.collection("users").document(userId)
        val purchaseRef = firestore.collection("purchases").document("${userId}_${chapter.id}")

        firestore.runTransaction { transaction ->
            val snap        = transaction.get(userRef)
            val latestCoins = (snap.getLong("coins") ?: 0L).toInt()

            if (latestCoins < chapter.coinPrice)
                throw Exception("Không đủ xu")

            // Trừ coins trong Firestore
            transaction.update(userRef, "coins", latestCoins - chapter.coinPrice)

            // Lưu bản ghi đã mua
            transaction.set(purchaseRef, mapOf(
                "userId"    to userId,
                "chapterId" to chapter.id,
                "novelId"   to novelId,
                "coinSpent" to chapter.coinPrice,
                "timestamp" to System.currentTimeMillis()
            ))

            latestCoins - chapter.coinPrice  // giá trị trả về
        }.addOnSuccessListener { newCoins ->
            // Cập nhật SharedPrefs để UI hiển thị xu mới
            userCoins = newCoins
            getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
                .putInt("coins", newCoins)
                .apply()

            Toast.makeText(this, "Đã dùng ${chapter.coinPrice} xu để mở khóa!", Toast.LENGTH_SHORT).show()
            displayChapter(chapter)
        }.addOnFailureListener { e ->
            val msg = if (e.message == "Không đủ xu") "Không đủ xu để mở khóa!" else "Lỗi giao dịch, thử lại!"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // ── Hiển thị nội dung chapter ─────────────────────────────────────────────
    private fun displayChapter(chapter: Chapter) {
        currentIndex = chapterList.indexOf(chapter)
        supportActionBar?.title = chapter.title
        binding.tvChapterTitle.text = chapter.title
        binding.tvContent.text      = chapter.content
        binding.scrollView.scrollTo(0, 0)
        binding.btnPrev.visibility = if (currentIndex > 0) View.VISIBLE else View.INVISIBLE
        binding.btnNext.visibility = if (currentIndex < chapterList.size - 1) View.VISIBLE else View.INVISIBLE
        saveHistory(chapter)
    }

    // ── Nút prev / next ───────────────────────────────────────────────────────
    private fun setupNavigation() {
        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) tryOpenChapter(chapterList[currentIndex - 1].id)
        }
        binding.btnNext.setOnClickListener {
            addExpForReading()
            if (currentIndex < chapterList.size - 1) tryOpenChapter(chapterList[currentIndex + 1].id)
        }
    }

    private fun goBackOrFinish() {
        if (currentIndex > 0) displayChapter(chapterList[currentIndex - 1])
        else finish()
    }

    // ── Lưu lịch sử đọc ──────────────────────────────────────────────────────
    private fun saveHistory(chapter: Chapter) {
        val prefs   = getSharedPreferences("LichSuDoc", Context.MODE_PRIVATE)
        val arr     = try { JSONArray(prefs.getString("lich_su", "[]")) } catch (e: Exception) { JSONArray() }
        val filtered = JSONArray()

        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i).getString("novelId") != novelId)
                filtered.put(arr.getJSONObject(i))
        }

        val entry = JSONObject().apply {
            put("novelId",       novelId)
            put("title",         novelTitle)
            put("coverUrl",      novelCover)
            put("lastChapter",   chapter.title)
            put("lastChapterId", chapter.id)
            put("timestamp",     System.currentTimeMillis())
        }

        val finalArr = JSONArray()
        finalArr.put(entry)
        for (i in 0 until filtered.length()) finalArr.put(filtered.getJSONObject(i))

        val limited = JSONArray()
        for (i in 0 until minOf(finalArr.length(), 50)) limited.put(finalArr.getJSONObject(i))
        prefs.edit().putString("lich_su", limited.toString()).apply()
    }

    private fun addExpForReading() {
        val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("userId", "") ?: ""
        if (userId.isEmpty()) return

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)

        // Dùng FieldValue.increment để tự động cộng thêm 10 exp vào Database
        // (Firebase sẽ tự lo việc cộng dồn an toàn dù user đổi mạng)
        userRef.update("exp", FieldValue.increment(10))
            .addOnSuccessListener {
                // Có thể hiện Toast nhỏ "Nhận +10 KN tu luyện!" hoặc im lặng
            }
    }
}