package com.example.appnovel

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper (context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "novel.app.db"
        const val DATABASE_VERSION = 1

        const val TABLE_USERS = "users"
        const val COL_ID = "id"
        const val COL_USERNAME = "username"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD = "password"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL UNIQUE,
                $COL_PASSWORD TEXT NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun registerUser(username: String, email: String, password: String): String {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_USERNAME, username.trim())
                put(COL_EMAIL, email.lowercase().trim())
                put(COL_PASSWORD, hashPassword(password))
            }
            val result = db.insert(TABLE_USERS, null, values)
            db.close()
            if (result != -1L) "success" else "error"
        } catch (e: SQLiteConstraintException) {
            "email_exists"
        }
    }

    fun loginUser(email: String, password: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_ID, COL_USERNAME, COL_EMAIL),
            "$COL_EMAIL = ? AND $COL_PASSWORD = ?",
            arrayOf(email.lowercase().trim(), hashPassword(password)),
            null,null,null
        )
        val user = if (cursor.moveToFirst()) {
            User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL))
            )
        } else null

        cursor.close()
        db.close()
        return user
    }

    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_ID),
            "$COL_EMAIL = ?",
            arrayOf(email.lowercase().trim()),
            null,null,null
        )
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }
    private fun hashPassword(password: String) : String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())
        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}