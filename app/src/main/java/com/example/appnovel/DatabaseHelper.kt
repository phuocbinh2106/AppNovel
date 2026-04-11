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
        const val DATABASE_VERSION = 2

        //Bảng Users
        const val TABLE_USERS = "users"
        const val COL_ID = "id"
        const val COL_USERNAME = "username"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD = "password"
        const val COL_COINS = "coins"

        //Bảng transactions
        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_TXN_ID = "txn_id"
        const val COL_TXN_USER_ID = "user_id"
        const val COL_TXN_AMOUNT = "amount"
        const val COL_TXN_COINS = "coins_added"
        const val COL_TXN_DATE = "created_at"

        //Bảng mã nạp
        const val TABLE_CODES = "recharge_codes"
        const val COL_CODE_ID = "code_id"
        const val COL_CODE_VALUE = "code_value"
        const val COL_CODE_AMOUNT = "code_amount"
        const val COL_CODE_COINS = "code_coins"
        const val COL_CODE_USED = "code_used"
        const val COL_CODE_USED_BY = "code_used_by"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL UNIQUE,
                $COL_PASSWORD TEXT NOT NULL,
                $COL_COINS INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_TXN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TXN_USER_ID INTEGER NOT NULL,
                $COL_TXN_AMOUNT INTEGER NOT NULL,
                $COL_TXN_COINS INTEGER NOT NULL,
                $COL_TXN_DATE TEXT NOT NULL,
                FOREIGN KEY ($COL_TXN_USER_ID) REFERENCES $TABLE_USERS($COL_ID)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_CODES (
                $COL_CODE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CODE_VALUE TEXT NOT NULL UNIQUE,
                $COL_CODE_AMOUNT INTEGER NOT NULL,
                $COL_CODE_COINS INTEGER NOT NULL,
                $COL_CODE_USED INTEGER NOT NULL DEFAULT 0,
                $COL_CODE_USED_BY INTEGER DEFAULT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CODES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
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

    fun getCoins(userId: Int): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_COINS),
            "$COL_ID = ?",
            arrayOf(userId.toString()),
            null,null,null
        )
        val coins = if (cursor.moveToFirst())
            cursor.getInt(cursor.getColumnIndexOrThrow(COL_COINS)) else 0
        cursor.close()
        db.close()
        return coins
    }

    fun getTransactions(userId: Int): List<Transaction> {
        val db = readableDatabase
        val list = mutableListOf<Transaction>()
        val cursor = db.query(
            TABLE_TRANSACTIONS,
            null,
            "$COL_TXN_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null,
            "$COL_TXN_ID DESC"
        )
        while (cursor.moveToNext()) {
            list.add(
                Transaction(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TXN_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TXN_USER_ID)),
                    amountVnd = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TXN_AMOUNT)),
                    coinsAdded = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TXN_COINS)),
                    date = cursor.getString(cursor.getColumnIndexOrThrow(COL_TXN_DATE))
                )
            )
        }
        cursor.close()
        db.close()
        return list
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

    fun insertRechargeCode(code: String, amountVnd: Int, coins: Int): Boolean {
        return try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_CODE_VALUE, code.uppercase().trim())
                put(COL_CODE_AMOUNT, amountVnd)
                put(COL_CODE_COINS, coins)
                put(COL_CODE_USED, 0)
            }
            val result = db.insert(TABLE_CODES, null, values)
            db.close()
            result != -1L
        } catch (e: Exception) { false }
    }

    fun redeemCode(userId: Int, code:String): RedeemResult {
        val db = writableDatabase
        val cursor = db.query(
            TABLE_CODES, null,
            "$COL_CODE_VALUE = ?",
            arrayOf(code.uppercase().trim()),
            null,null,null
        )

        if (!cursor.moveToFirst()) {
            cursor.close(); db.close()
            return RedeemResult.NOT_FOUND
        }

        val isUsed = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CODE_USED)) == 1
        if (isUsed) {
            cursor.close(); db.close()
            return RedeemResult.ALREADY_USED
        }

        val codeId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CODE_ID))
        val coins = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CODE_COINS))
        val amount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CODE_AMOUNT))
        cursor.close()

        return try {
            db.beginTransaction()

            val markUsed = ContentValues().apply {
                put(COL_CODE_USED, 1)
                put(COL_CODE_USED_BY, userId)
            }
            db.update(TABLE_CODES, markUsed, "$COL_CODE_ID = ?", arrayOf(codeId.toString()))

            val newCoins = getCoins(userId) + coins
            val updateCoins = ContentValues().apply { put(COL_COINS, newCoins) }
            db.update(TABLE_USERS, updateCoins, "$COL_ID = ?", arrayOf(userId.toString()))

            val txn = ContentValues().apply {
                put(COL_TXN_USER_ID, userId)
                put(COL_TXN_AMOUNT, amount)
                put(COL_TXN_COINS, coins)
                put(COL_TXN_DATE, java.text.SimpleDateFormat(
                    "dd/MM/yyyy HH:mm", java.util.Locale.getDefault()
                ).format(java.util.Date()))
            }
            db.insert(TABLE_TRANSACTIONS, null, txn)

            db.setTransactionSuccessful()
            RedeemResult.Success(coins, newCoins)
        } catch (e: Exception) {
            RedeemResult.ERROR
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    private fun hashPassword(password: String) : String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())
        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
    // ── Cập nhật username ────────────────────────────────────
    fun updateUsername(email: String, newUsername: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_USERNAME, newUsername.trim())
        }
        val rows = db.update(
            TABLE_USERS,
            values,
            "$COL_EMAIL = ?",
            arrayOf(email.lowercase().trim())
        )
        db.close()
        return rows > 0
    }

    // ── Xóa tài khoản ───────────────────────────────────────
    fun deleteAccount(email: String): Boolean {
        val db = writableDatabase
        val rows = db.delete(
            TABLE_USERS,
            "$COL_EMAIL = ?",
            arrayOf(email.lowercase().trim())
        )
        db.close()
        return rows > 0
    }
}