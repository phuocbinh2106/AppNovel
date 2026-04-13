package com.example.appnovel

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // 1. Mặc định khi mở app sẽ hiện trang Home ngay
        replaceFragment(HomeFragment())

        // 2. Bắt sự kiện khi bấm vào các nút trên thanh Menu
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_library -> {replaceFragment(LibraryFragment())}
                R.id.nav_profile -> {replaceFragment(AccountFragment())}
            }
            true
        }
    }

    // Hàm tiện ích để đổi màn hình (Fragment)
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
    }
}