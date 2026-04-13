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
        const val DATABASE_VERSION = 11

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
        const val COL_NOV_GENRES = "genres"
        const val COL_NOV_IMAGE = "imageUrl"
        const val COL_NOV_DESC = "description"
        const val COL_NOV_STATUS = "status"
        const val COL_NOV_UPLOADER_ID = "uploader_id"

        const val TABLE_CHAPTERS = "chapters"
        const val COL_CH_ID = "ch_id"
        const val COL_CH_NOVEL_ID = "novel_id"
        const val COL_CH_TITLE = "chapter_title"
        const val COL_CH_CONTENT = "content"
        const val COL_CH_COIN_PRICE = "coin_price"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS purchased_chapters (user_id TEXT, chapter_id TEXT, PRIMARY KEY(user_id, chapter_id))")
        db.execSQL("CREATE TABLE $TABLE_USERS ($COL_ID TEXT PRIMARY KEY, $COL_USERNAME TEXT, $COL_EMAIL TEXT UNIQUE, $COL_PASSWORD TEXT, $COL_COINS INTEGER DEFAULT 0, $COL_ROLE TEXT DEFAULT '$ROLE_USER')")
        db.execSQL("CREATE TABLE $TABLE_NOVELS ($COL_NOV_ID TEXT PRIMARY KEY, $COL_NOV_TITLE TEXT, $COL_NOV_AUTHOR TEXT, $COL_NOV_GENRES TEXT, $COL_NOV_IMAGE TEXT, $COL_NOV_DESC TEXT, $COL_NOV_STATUS TEXT, $COL_NOV_UPLOADER_ID TEXT)")
        db.execSQL("CREATE TABLE $TABLE_CHAPTERS ($COL_CH_ID TEXT PRIMARY KEY, $COL_CH_NOVEL_ID TEXT, $COL_CH_TITLE TEXT, $COL_CH_CONTENT TEXT, $COL_CH_COIN_PRICE INTEGER DEFAULT 0, FOREIGN KEY($COL_CH_NOVEL_ID) REFERENCES $TABLE_NOVELS($COL_NOV_ID))")

        insertDefaultAccount(db, "admin_fixed_uid", "admin", "admin@gmail.com", "123123", ROLE_ADMIN)
        insertDefaultAccount(db, "uploader1_fixed_uid", "uploader1", "uploader1@gmail.com", "123123", ROLE_UPLOADER)
    }

    private fun insertDefaultAccount(db: SQLiteDatabase, id: String, name: String, email: String, pass: String, role: String) {
        val v = ContentValues().apply {
            put(COL_ID, id); put(COL_USERNAME, name); put(COL_EMAIL, email); put(COL_PASSWORD, hashPassword(pass)); put(COL_ROLE, role)
        }
        db.insert(TABLE_USERS, null, v)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS purchased_chapters")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHAPTERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOVELS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun addNovel(id: String, title: String, author: String, genres: List<String>, img: String, desc: String, uploaderId: String): Long {
        val v = ContentValues().apply {
            put(COL_NOV_ID, id); put(COL_NOV_TITLE, title); put(COL_NOV_AUTHOR, author)
            put(COL_NOV_GENRES, genres.joinToString(",")); put(COL_NOV_IMAGE, img); put(COL_NOV_DESC, desc)
            put(COL_NOV_STATUS, "Đang ra"); put(COL_NOV_UPLOADER_ID, uploaderId)
        }
        return writableDatabase.insert(TABLE_NOVELS, null, v)
    }

    private fun cursorToNovel(cursor: Cursor): Novel {
        val genreString = cursor.getString(3) ?: ""
        val genreList = if (genreString.isEmpty()) emptyList() else genreString.split(",")
        return Novel(cursor.getString(0), cursor.getString(1) ?: "", cursor.getString(2) ?: "", genreList, cursor.getString(4) ?: "", cursor.getString(5) ?: "", cursor.getString(6) ?: "", cursor.getString(7) ?: "")
    }

    fun updateChapter(chapterId: String, title: String, content: String, coinPrice: Int): Boolean {
        val v = ContentValues().apply {
            put(COL_CH_TITLE, title)
            put(COL_CH_CONTENT, content)
            put(COL_CH_COIN_PRICE, coinPrice)
        }
        return writableDatabase.update(TABLE_CHAPTERS, v, "$COL_CH_ID=?", arrayOf(chapterId)) > 0
    }

    fun addChapter(id: String, novelId: String, title: String, content: String, coinPrice: Int = 0): Long {
        val v = ContentValues().apply {
            put(COL_CH_ID, id); put(COL_CH_NOVEL_ID, novelId); put(COL_CH_TITLE, title); put(COL_CH_CONTENT, content); put(COL_CH_COIN_PRICE, coinPrice)
        }
        return writableDatabase.insert(TABLE_CHAPTERS, null, v)
    }

    fun getChaptersByNovelId(novelId: String): Cursor {
        return readableDatabase.rawQuery("SELECT * FROM $TABLE_CHAPTERS WHERE $COL_CH_NOVEL_ID=? ORDER BY rowid ASC", arrayOf(novelId))
    }

    fun deleteChapter(chapterId: String): Boolean {
        return writableDatabase.delete(TABLE_CHAPTERS, "$COL_CH_ID=?", arrayOf(chapterId)) > 0
    }

    private fun hashPassword(p: String): String = MessageDigest.getInstance("SHA-256").digest(p.toByteArray()).joinToString("") { "%02x".format(it) }

    fun getUserCoins(userId: String): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT $COL_COINS FROM $TABLE_USERS WHERE $COL_ID=?", arrayOf(userId)
        )
        val coins = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return coins
    }

    fun deductCoins(userId: String, amount: Int): Boolean {
        val current = getUserCoins(userId)
        if (current < amount) return false
        val v = ContentValues().apply { put(COL_COINS, current - amount) }
        return writableDatabase.update(TABLE_USERS, v, "$COL_ID=?", arrayOf(userId)) > 0
    }

    // Kiểm tra user đã mua chapter chưa (lưu local)
    fun hasPurchasedChapter(userId: String, chapterId: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM purchased_chapters WHERE user_id=? AND chapter_id=?",
            arrayOf(userId, chapterId)
        )
        val result = cursor.moveToFirst() && cursor.getInt(0) > 0
        cursor.close()
        return result
    }

    fun savePurchasedChapter(userId: String, chapterId: String) {
        val v = ContentValues().apply {
            put("user_id", userId)
            put("chapter_id", chapterId)
        }
        writableDatabase.insertWithOnConflict("purchased_chapters", null, v,
            android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)
    }
}
