package com.example.appnovel

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "novel.app.db"
        const val DATABASE_VERSION = 7

        const val ROLE_USER = "user"
        const val ROLE_ADMIN = "admin"
        const val ROLE_UPLOADER = "uploader"

        const val TABLE_USERS = "users"
        const val COL_ID = "id"
        const val COL_USERNAME = "username"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD = "password"
        const val COL_COINS = "coins"
        const val COL_ROLE = "role"

        const val TABLE_NOVELS = "novels"
        const val COL_NOV_ID = "nov_id"
        const val COL_NOV_TITLE = "title"
        const val COL_NOV_AUTHOR = "author"
        const val COL_NOV_IMAGE = "imageUrl"
        const val COL_NOV_DESC = "description"
        const val COL_NOV_STATUS = "status"
        const val COL_NOV_UPLOADER_ID = "uploader_id"

        const val TABLE_CHAPTERS = "chapters"
        const val COL_CH_ID = "ch_id"
        const val COL_CH_NOVEL_ID = "novel_id"
        const val COL_CH_TITLE = "chapter_title"
        const val COL_CH_CONTENT = "content"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_USERS ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_USERNAME TEXT, $COL_EMAIL TEXT UNIQUE, $COL_PASSWORD TEXT, $COL_COINS INTEGER DEFAULT 0, $COL_ROLE TEXT DEFAULT '$ROLE_USER')")
        db.execSQL("CREATE TABLE $TABLE_NOVELS ($COL_NOV_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_NOV_TITLE TEXT, $COL_NOV_AUTHOR TEXT, $COL_NOV_IMAGE TEXT, $COL_NOV_DESC TEXT, $COL_NOV_STATUS TEXT, $COL_NOV_UPLOADER_ID INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE $TABLE_CHAPTERS ($COL_CH_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_CH_NOVEL_ID INTEGER, $COL_CH_TITLE TEXT, $COL_CH_CONTENT TEXT, FOREIGN KEY($COL_CH_NOVEL_ID) REFERENCES $TABLE_NOVELS($COL_NOV_ID))")

        insertDefaultAccount(db, "admin", "admin@gmail.com", "123123", ROLE_ADMIN)
        insertDefaultAccount(db, "uploader1", "uploader1@gmail.com", "123123", ROLE_UPLOADER)
        insertDefaultAccount(db, "uploader2", "uploader2@gmail.com", "123123", ROLE_UPLOADER)
        insertDefaultAccount(db, "uploader3", "uploader3@gmail.com", "123123", ROLE_UPLOADER)
    }

    private fun insertDefaultAccount(db: SQLiteDatabase, name: String, email: String, pass: String, role: String) {
        val v = ContentValues().apply {
            put(COL_USERNAME, name); put(COL_EMAIL, email); put(COL_PASSWORD, hashPassword(pass)); put(COL_ROLE, role)
        }
        db.insert(TABLE_USERS, null, v)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAPTERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOVELS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // --- CRUD TRUYỆN (NOVELS) ---
    fun addNovel(title: String, author: String, img: String, desc: String, uploaderId: Int = 0): Long {
        val v = ContentValues().apply {
            put(COL_NOV_TITLE, title); put(COL_NOV_AUTHOR, author)
            put(COL_NOV_IMAGE, img); put(COL_NOV_DESC, desc); put(COL_NOV_STATUS, "Đang ra")
            put(COL_NOV_UPLOADER_ID, uploaderId)
        }
        return writableDatabase.insert(TABLE_NOVELS, null, v)
    }

    fun getAllNovels(): List<Novel> {
        val list = mutableListOf<Novel>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_NOVELS ORDER BY $COL_NOV_ID DESC", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Novel(cursor.getInt(0), cursor.getString(1) ?: "", cursor.getString(2) ?: "", cursor.getString(3) ?: "", cursor.getString(4) ?: "", cursor.getString(5) ?: ""))
            } while (cursor.moveToNext())
        }
        cursor.close(); return list
    }

    fun getNovelsByRole(role: String, userId: Int): List<Novel> {
        val list = mutableListOf<Novel>()
        val sql = if (role == ROLE_ADMIN) {
            "SELECT * FROM $TABLE_NOVELS ORDER BY $COL_NOV_ID DESC"
        } else {
            "SELECT * FROM $TABLE_NOVELS WHERE $COL_NOV_UPLOADER_ID = $userId ORDER BY $COL_NOV_ID DESC"
        }
        val cursor = readableDatabase.rawQuery(sql, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Novel(cursor.getInt(0), cursor.getString(1) ?: "", cursor.getString(2) ?: "", cursor.getString(3) ?: "", cursor.getString(4) ?: "", cursor.getString(5) ?: ""))
            } while (cursor.moveToNext())
        }
        cursor.close(); return list
    }

    fun updateNovel(id: Int, title: String, author: String, img: String, desc: String, status: String, uploaderId: Int): Boolean {
        val v = ContentValues().apply {
            put(COL_NOV_TITLE, title); put(COL_NOV_AUTHOR, author)
            put(COL_NOV_IMAGE, img); put(COL_NOV_DESC, desc); put(COL_NOV_STATUS, status)
            put(COL_NOV_UPLOADER_ID, uploaderId)
        }
        return writableDatabase.update(TABLE_NOVELS, v, "$COL_NOV_ID=?", arrayOf(id.toString())) > 0
    }

    fun deleteNovel(id: Int): Boolean {
        writableDatabase.delete(TABLE_CHAPTERS, "$COL_CH_NOVEL_ID=?", arrayOf(id.toString()))
        return writableDatabase.delete(TABLE_NOVELS, "$COL_NOV_ID=?", arrayOf(id.toString())) > 0
    }

    // --- CRUD CHAPTERS ---
    fun addChapter(novelId: Int, title: String, content: String): Long {
        val v = ContentValues().apply {
            put(COL_CH_NOVEL_ID, novelId); put(COL_CH_TITLE, title); put(COL_CH_CONTENT, content)
        }
        return writableDatabase.insert(TABLE_CHAPTERS, null, v)
    }

    fun getChaptersByNovelId(novelId: Int): Cursor {
        return readableDatabase.rawQuery("SELECT * FROM $TABLE_CHAPTERS WHERE $COL_CH_NOVEL_ID=? ORDER BY $COL_CH_ID ASC", arrayOf(novelId.toString()))
    }

    fun updateChapter(chapterId: Int, title: String, content: String): Boolean {
        val v = ContentValues().apply { put(COL_CH_TITLE, title); put(COL_CH_CONTENT, content) }
        return writableDatabase.update(TABLE_CHAPTERS, v, "$COL_CH_ID=?", arrayOf(chapterId.toString())) > 0
    }

    fun deleteChapter(chapterId: Int): Boolean {
        return writableDatabase.delete(TABLE_CHAPTERS, "$COL_CH_ID=?", arrayOf(chapterId.toString())) > 0
    }

    // --- USERS ---
    fun getAllUploaders(): List<User> {
        val list = mutableListOf<User>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COL_ROLE = ?", arrayOf(ROLE_UPLOADER))
        if (cursor.moveToFirst()) {
            do {
                list.add(User(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_COINS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close(); return list
    }

    fun loginUser(email: String, pass: String): User? {
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COL_EMAIL=? AND $COL_PASSWORD=?", arrayOf(email.lowercase(), hashPassword(pass)))
        return if (cursor.moveToFirst()) {
            val user = User(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)), cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)), cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)), cursor.getInt(cursor.getColumnIndexOrThrow(COL_COINS)), cursor.getString(cursor.getColumnIndexOrThrow(COL_ROLE)))
            cursor.close(); user
        } else { cursor.close(); null }
    }

    private fun hashPassword(p: String): String = MessageDigest.getInstance("SHA-256").digest(p.toByteArray()).joinToString("") { "%02x".format(it) }
    
    fun registerUser(username: String, email: String, pass: String): String {
        return try {
            val v = ContentValues().apply { put(COL_USERNAME, username); put(COL_EMAIL, email.lowercase()); put(COL_PASSWORD, hashPassword(pass)); put(COL_ROLE, ROLE_USER) }
            if (writableDatabase.insert(TABLE_USERS, null, v) != -1L) "success" else "error"
        } catch (e: Exception) { "email_exists" }
    }
    fun updateUsername(email: String, newUsername: String): Boolean {
        val v = ContentValues().apply { put(COL_USERNAME, newUsername) }
        return writableDatabase.update(TABLE_USERS, v, "$COL_EMAIL=?", arrayOf(email.lowercase())) > 0
    }
    fun deleteAccount(email: String): Boolean {
        return writableDatabase.delete(TABLE_USERS, "$COL_EMAIL=?", arrayOf(email.lowercase())) > 0
    }
}
