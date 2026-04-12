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
        const val DATABASE_VERSION = 10

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

    fun getAllNovels(): List<Novel> {
        val list = mutableListOf<Novel>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_NOVELS ORDER BY rowid DESC", null)
        if (cursor.moveToFirst()) {
            do { list.add(cursorToNovel(cursor)) } while (cursor.moveToNext())
        }
        cursor.close(); return list
    }

    fun getNovelsByRole(role: String, userId: String): List<Novel> {
        val list = mutableListOf<Novel>()
        val sql = if (role == ROLE_ADMIN) "SELECT * FROM $TABLE_NOVELS" else "SELECT * FROM $TABLE_NOVELS WHERE $COL_NOV_UPLOADER_ID = '$userId'"
        val cursor = readableDatabase.rawQuery(sql, null)
        if (cursor.moveToFirst()) {
            do { list.add(cursorToNovel(cursor)) } while (cursor.moveToNext())
        }
        cursor.close(); return list
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

    fun deleteNovel(id: String): Boolean {
        writableDatabase.delete(TABLE_CHAPTERS, "$COL_CH_NOVEL_ID=?", arrayOf(id))
        return writableDatabase.delete(TABLE_NOVELS, "$COL_NOV_ID=?", arrayOf(id)) > 0
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

    fun loginUser(email: String, pass: String): User? {
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COL_EMAIL=? AND $COL_PASSWORD=?", arrayOf(email.lowercase(), hashPassword(pass)))
        return if (cursor.moveToFirst()) {
            val user = User(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(4), cursor.getString(5))
            cursor.close(); user
        } else { cursor.close(); null }
    }

    fun getAllUploaders(): List<User> {
        val list = mutableListOf<User>()
        val cursor = readableDatabase.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COL_ROLE = ?", arrayOf(ROLE_UPLOADER))
        if (cursor.moveToFirst()) {
            do {
                list.add(User(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(4), cursor.getString(5)))
            } while (cursor.moveToNext())
        }
        cursor.close(); return list
    }

    private fun hashPassword(p: String): String = MessageDigest.getInstance("SHA-256").digest(p.toByteArray()).joinToString("") { "%02x".format(it) }
}
