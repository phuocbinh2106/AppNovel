package com.example.appnovel

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.appnovel.databinding.ActivityReadChapterBinding
import org.json.JSONArray
import org.json.JSONObject

class ReadChapterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReadChapterBinding
    private lateinit var db: DatabaseHelper
    private var chapterId = ""
    private var novelId = ""
    private var chapterList = listOf<Chapter>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadChapterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)
        chapterId = intent.getStringExtra("CHAPTER_ID") ?: ""
        novelId = intent.getStringExtra("NOVEL_ID") ?: ""

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadChapterList()
        loadChapter(chapterId)
        setupNavigation()
    }

    private fun loadChapterList() {
        val cursor = db.getChaptersByNovelId(novelId)
        val list = mutableListOf<Chapter>()
        if (cursor.moveToFirst()) {
            do {
                list.add(Chapter(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_ID)),
                    novelId = novelId,
                    title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_TITLE)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_CONTENT)),
                    coinPrice = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CH_COIN_PRICE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        chapterList = list
        currentIndex = list.indexOfFirst { it.id == chapterId }
    }

    private fun loadChapter(id: String) {
        val chapter = chapterList.find { it.id == id } ?: return
        currentIndex = chapterList.indexOf(chapter)

        supportActionBar?.title = chapter.title
        binding.tvChapterTitle.text = chapter.title
        binding.tvContent.text = chapter.content

        // Scroll về đầu
        binding.scrollView.scrollTo(0, 0)

        // Cập nhật nút prev/next
        binding.btnPrev.visibility = if (currentIndex > 0) View.VISIBLE else View.INVISIBLE
        binding.btnNext.visibility = if (currentIndex < chapterList.size - 1) View.VISIBLE else View.INVISIBLE

        // Lưu lịch sử đọc
        saveHistory(chapter)
    }

    private fun setupNavigation() {
        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                loadChapter(chapterList[currentIndex - 1].id)
            }
        }
        binding.btnNext.setOnClickListener {
            if (currentIndex < chapterList.size - 1) {
                loadChapter(chapterList[currentIndex + 1].id)
            }
        }
    }

    private fun saveHistory(chapter: Chapter) {
        val novel = db.getAllNovels().find { it.id == novelId } ?: return
        val prefs = getSharedPreferences("LichSuDoc", Context.MODE_PRIVATE)
        val json = prefs.getString("lich_su", "[]") ?: "[]"

        val arr = try { JSONArray(json) } catch (e: Exception) { JSONArray() }

        // Xóa entry cũ nếu có
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            if (arr.getJSONObject(i).getString("novelId") != novelId) {
                newArr.put(arr.getJSONObject(i))
            }
        }

        // Thêm entry mới vào đầu
        val obj = JSONObject().apply {
            put("novelId", novelId)
            put("title", novel.title)
            put("coverUrl", novel.imageUrl)
            put("lastChapter", chapter.title)
            put("lastChapterId", chapter.id)
            put("timestamp", System.currentTimeMillis())
        }

        // Ghép mới vào đầu
        val finalArr = JSONArray()
        finalArr.put(obj)
        for (i in 0 until newArr.length()) {
            finalArr.put(newArr.getJSONObject(i))
        }

        // Giới hạn 50 truyện trong lịch sử
        val limitedArr = JSONArray()
        for (i in 0 until minOf(finalArr.length(), 50)) {
            limitedArr.put(finalArr.getJSONObject(i))
        }

        prefs.edit().putString("lich_su", limitedArr.toString()).apply()

        // Cập nhật tủ truyện nếu đang theo dõi
        updateTuTruyen(novel, chapter)
    }

    private fun updateTuTruyen(novel: Novel, chapter: Chapter) {
        val prefs = getSharedPreferences("TuTruyen", Context.MODE_PRIVATE)
        val json = prefs.getString("tu_truyen", "[]") ?: "[]"
        val arr = try { JSONArray(json) } catch (e: Exception) { return }

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("novelId") == novelId) {
                obj.put("lastChapter", chapter.title)
                prefs.edit().putString("tu_truyen", arr.toString()).apply()
                break
            }
        }
    }
}
