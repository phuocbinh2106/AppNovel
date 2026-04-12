package com.example.appnovel

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AdminAddActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_admin_add)

            val btnGoToNovelList = findViewById<MaterialButton>(R.id.btnGoToAddNovel)
            val btnGoToChapterNovelSelect = findViewById<MaterialButton>(R.id.btnGoToAddChapter)

            // 1. Quản lý truyện: Xem danh sách để Sửa/Xóa truyện
            btnGoToNovelList?.setOnClickListener {
                val intent = Intent(this, AdminNovelListActivity::class.java)
                intent.putExtra("MODE", "MANAGE_NOVEL")
                startActivity(intent)
            }

            // 2. Quản lý chapter: Xem danh sách truyện để chọn viết chapter
            btnGoToChapterNovelSelect?.setOnClickListener {
                val intent = Intent(this, AdminNovelListActivity::class.java)
                intent.putExtra("MODE", "SELECT_FOR_CHAPTERS")
                startActivity(intent)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi khởi tạo màn hình quản trị", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
