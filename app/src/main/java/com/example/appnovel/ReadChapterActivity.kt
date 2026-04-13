package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appnovel.databinding.ActivityReadChapterBinding
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

class ReadChapterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadChapterBinding
    private lateinit var db: DatabaseHelper
    private val firestore = FirebaseFirestore.getInstance()

    private var novelTitle = ""
    private var novelCover = ""
    private var chapterId  = ""
    private var novelId    = ""
    private var chapterList = listOf<Chapter>()
    private var currentIndex = 0
    private var userId    = ""
    private var userCoins = 0

    // Settings state
    private var fontSize   = 16f
    private var lineExtra  = 8f
    private var bgColor    = "#111827"   // màu nền mặc định
    private var textColor  = "#e5e7eb"   // màu chữ mặc định
    private var isSettingsVisible = false
    private var isAutoUnlock = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadChapterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db        = DatabaseHelper(this)
        chapterId  = intent.getStringExtra("CHAPTER_ID") ?: ""
        novelId    = intent.getStringExtra("NOVEL_ID") ?: ""
        novelTitle = intent.getStringExtra("NOVEL_TITLE") ?: ""
        novelCover = intent.getStringExtra("NOVEL_COVER") ?: ""

        val prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        userId    = prefs.getString("userId", "") ?: ""
        userCoins = prefs.getInt("coins", 0)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Restore settings từ SharedPrefs
        val readPrefs = getSharedPreferences("ReadSettings", Context.MODE_PRIVATE)
        fontSize    = readPrefs.getFloat("fontSize", 16f)
        lineExtra   = readPrefs.getFloat("lineExtra", 8f)
        bgColor     = readPrefs.getString("bgColor", "#111827") ?: "#111827"
        textColor   = readPrefs.getString("textColor", "#e5e7eb") ?: "#e5e7eb"
        isAutoUnlock = readPrefs.getBoolean("autoUnlock", false)

        applyReadSettings()

        loadChapterList()
        setupNavigation()
        setupSettings()
        tryOpenChapter(chapterId)
    }

    // ────────────────────────────────────────────────────────────────────────
    //  CHAPTER LIST
    // ────────────────────────────────────────────────────────────────────────
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

    // ────────────────────────────────────────────────────────────────────────
    //  LOCK / OPEN LOGIC
    // ────────────────────────────────────────────────────────────────────────
    private fun tryOpenChapter(id: String) {
        val chapter = chapterList.find { it.id == id } ?: return
        currentIndex = chapterList.indexOf(chapter)

        if (chapter.coinPrice <= 0) {
            showContent(chapter); return
        }
        if (userId.isEmpty()) {
            showLockScreen(chapter, purchased = false); return
        }

        firestore.collection("purchases").document("${userId}_${chapter.id}").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    showContent(chapter)
                } else if (isAutoUnlock && userCoins >= chapter.coinPrice) {
                    // Auto-unlock: tự động mua và thông báo nhỏ
                    purchaseChapter(chapter, silent = true)
                } else {
                    showLockScreen(chapter, purchased = false)
                }
            }
            .addOnFailureListener { showLockScreen(chapter, purchased = false) }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  LOCK SCREEN
    // ────────────────────────────────────────────────────────────────────────
    private fun showLockScreen(chapter: Chapter, purchased: Boolean) {
        binding.scrollView.visibility    = View.GONE
        binding.layoutLocked.visibility  = View.VISIBLE

        binding.tvLockedTitle.text = chapter.title
        binding.tvLockedDesc.text  = "Chương này cần ${chapter.coinPrice} xu\nXu của bạn: $userCoins xu"

        // Nút Mở khóa bằng xu — text động theo trạng thái đủ/thiếu xu
        val canAfford = userCoins >= chapter.coinPrice
        binding.btnUnlockCoin.text = if (canAfford)
            "🔓  Mở Khóa (${chapter.coinPrice} Xu)"
        else
            "🔓  Mở Khóa (thiếu xu)"
        binding.btnUnlockCoin.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                if (canAfford) Color.parseColor("#16a34a") else Color.parseColor("#374151")
            )

        // ── Nút Mua vé tháng ──
        binding.btnBuyMonthly.setOnClickListener {
            startActivity(Intent(this, MonthlyPassActivity::class.java))
        }

        // ── Nút Mở khóa bằng xu ──
        binding.btnUnlockCoin.setOnClickListener {
            if (userId.isEmpty()) {
                startActivity(Intent(this, LoginActivity::class.java)); return@setOnClickListener
            }
            if (canAfford) purchaseChapter(chapter)
            else Toast.makeText(this, "Không đủ xu! Hãy nạp thêm.", Toast.LENGTH_SHORT).show()
        }

        // ── Nút Nạp xu ──
        binding.btnTopUpCoin.setOnClickListener {
            startActivity(Intent(this, NapXuActivity::class.java))
        }

        // Cập nhật nút prev/next (vẫn cho chuyển chapter dù đang ở lock screen)
        updateNavButtons()
    }

    private fun purchaseChapter(chapter: Chapter, silent: Boolean = false) {
        val userRef     = firestore.collection("users").document(userId)
        val purchaseRef = firestore.collection("purchases").document("${userId}_${chapter.id}")

        firestore.runTransaction { transaction ->
            val snap        = transaction.get(userRef)
            val latestCoins = (snap.getLong("coins") ?: 0L).toInt()
            if (latestCoins < chapter.coinPrice) throw Exception("Không đủ xu")
            transaction.update(userRef, "coins", latestCoins - chapter.coinPrice)
            transaction.set(purchaseRef, mapOf(
                "userId"    to userId,
                "chapterId" to chapter.id,
                "novelId"   to novelId,
                "coinSpent" to chapter.coinPrice,
                "timestamp" to System.currentTimeMillis()
            ))
            latestCoins - chapter.coinPrice
        }.addOnSuccessListener { newCoins ->
            userCoins = newCoins
            getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
                .putInt("coins", newCoins).apply()
            if (!silent) {
                Toast.makeText(this, "Đã dùng ${chapter.coinPrice} xu để mở khóa!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "🔓 Tự động mở: -${chapter.coinPrice} xu (còn $newCoins xu)", Toast.LENGTH_SHORT).show()
            }
            showContent(chapter)
        }.addOnFailureListener { e ->
            val msg = if (e.message == "Không đủ xu") "Không đủ xu!" else "Lỗi giao dịch!"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  DISPLAY CONTENT
    // ────────────────────────────────────────────────────────────────────────
    private fun showContent(chapter: Chapter) {
        currentIndex = chapterList.indexOf(chapter)
        binding.layoutLocked.visibility = View.GONE
        binding.scrollView.visibility   = View.VISIBLE

        supportActionBar?.title = chapter.title
        binding.tvChapterTitle.text = chapter.title
        binding.tvContent.text      = chapter.content
        binding.scrollView.scrollTo(0, 0)
        updateNavButtons()
        saveHistory(chapter)
    }

    private fun updateNavButtons() {
        binding.btnPrev.visibility = if (currentIndex > 0) View.VISIBLE else View.INVISIBLE
        binding.btnNext.visibility = if (currentIndex < chapterList.size - 1) View.VISIBLE else View.INVISIBLE
    }

    private fun setupNavigation() {
        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) tryOpenChapter(chapterList[currentIndex - 1].id)
        }
        binding.btnNext.setOnClickListener {
            if (currentIndex < chapterList.size - 1) tryOpenChapter(chapterList[currentIndex + 1].id)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  SETTINGS PANEL
    // ────────────────────────────────────────────────────────────────────────
    private fun setupSettings() {
        // Nút mở/đóng settings
        binding.btnSettings.setOnClickListener {
            isSettingsVisible = !isSettingsVisible
            binding.layoutSettings.visibility = if (isSettingsVisible) View.VISIBLE else View.GONE
        }

        // Tab chỉnh sửa (mặc định active)
        binding.tabEdit.setOnClickListener { /* đã hiện panelEdit */ }
        binding.tabInfo.setOnClickListener {
            Toast.makeText(this, "Thông tin chương", Toast.LENGTH_SHORT).show()
        }

        // ── Font ──
        binding.btnFontSerif.setOnClickListener {
            binding.tvContent.typeface = Typeface.SERIF
            highlightFontBtn(0)
        }
        binding.btnFontSans.setOnClickListener {
            binding.tvContent.typeface = Typeface.SANS_SERIF
            highlightFontBtn(1)
        }
        binding.btnFontMono.setOnClickListener {
            binding.tvContent.typeface = Typeface.MONOSPACE
            highlightFontBtn(2)
        }

        // ── Cỡ chữ ──
        binding.tvFontSize.text = fontSize.toInt().toString()
        binding.btnFontDecrease.setOnClickListener {
            if (fontSize > 12f) { fontSize -= 1f; applyTextStyle(); binding.tvFontSize.text = fontSize.toInt().toString() }
        }
        binding.btnFontIncrease.setOnClickListener {
            if (fontSize < 28f) { fontSize += 1f; applyTextStyle(); binding.tvFontSize.text = fontSize.toInt().toString() }
        }

        // ── Giãn dòng ──
        binding.tvLineHeight.text = lineExtra.toInt().toString()
        binding.btnLineDecrease.setOnClickListener {
            if (lineExtra > 0f) { lineExtra -= 2f; applyTextStyle(); binding.tvLineHeight.text = lineExtra.toInt().toString() }
        }
        binding.btnLineIncrease.setOnClickListener {
            if (lineExtra < 24f) { lineExtra += 2f; applyTextStyle(); binding.tvLineHeight.text = lineExtra.toInt().toString() }
        }

        // ── Màu nền ──
        // Đen, Xám → chữ trắng | còn lại → chữ đen
        binding.colorPink.setOnClickListener   { setBg("#fce7f3", "#111111") }
        binding.colorBlue.setOnClickListener   { setBg("#dbeafe", "#111111") }
        binding.colorGray.setOnClickListener   { setBg("#374151", "#ffffff") }
        binding.colorBlack.setOnClickListener  { setBg("#111111", "#ffffff") }
        binding.colorYellow.setOnClickListener { setBg("#fef9c3", "#111111") }
        binding.colorWhite.setOnClickListener  { setBg("#ffffff",  "#111111") }

        // ── Mặc định ──
        binding.btnDefault.setOnClickListener {
            fontSize  = 16f; lineExtra = 8f
            setBg("#111111", "#ffffff")
            binding.tvContent.typeface = Typeface.DEFAULT
            binding.tvFontSize.text    = "16"
            binding.tvLineHeight.text  = "8"
            applyTextStyle()
        }
    }

    private fun setBg(bg: String, text: String) {
        bgColor   = bg
        textColor = text
        applyReadSettings()
        saveReadSettings()
    }

    private fun applyReadSettings() {
        try {
            binding.scrollView.setBackgroundColor(Color.parseColor(bgColor))
            binding.tvContent.setTextColor(Color.parseColor(textColor))
            binding.tvChapterTitle.setTextColor(Color.parseColor(textColor))
        } catch (_: Exception) {}
        applyTextStyle()
    }

    private fun applyTextStyle() {
        binding.tvContent.textSize          = fontSize
        binding.tvContent.setLineSpacing(lineExtra, 1f)
        saveReadSettings()
    }

    private fun saveReadSettings() {
        getSharedPreferences("ReadSettings", Context.MODE_PRIVATE).edit()
            .putFloat("fontSize", fontSize)
            .putFloat("lineExtra", lineExtra)
            .putString("bgColor", bgColor)
            .putString("textColor", textColor)
            .apply()
    }

    private fun highlightFontBtn(active: Int) {
        val buttons = listOf(binding.btnFontSerif, binding.btnFontSans, binding.btnFontMono)
        buttons.forEachIndexed { i, btn ->
            btn.strokeColor = android.content.res.ColorStateList.valueOf(
                if (i == active) Color.parseColor("#60a5fa") else Color.parseColor("#374151")
            )
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  HISTORY
    // ────────────────────────────────────────────────────────────────────────
    private fun saveHistory(chapter: Chapter) {
        val prefs    = getSharedPreferences("LichSuDoc", Context.MODE_PRIVATE)
        val arr      = try { JSONArray(prefs.getString("lich_su", "[]")) } catch (e: Exception) { JSONArray() }
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
}