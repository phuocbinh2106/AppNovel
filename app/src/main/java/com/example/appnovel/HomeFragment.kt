package com.example.appnovel

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class HomeFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper // Đã sửa từ Database sang DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_home, container, false)
            dbHelper = DatabaseHelper(requireContext())

            // 1. BANNER
            val viewPagerBanner = view.findViewById<ViewPager2>(R.id.viewPagerBanner)
            val bannerList = listOf(
                "https://img.upanh.tv/2023/12/04/Toan-Tri-Doc-Gia-Web-Banner-1920x1080.jpg",
                "https://img.upanh.tv/2023/12/04/Solo-Leveling-Web-Banner.jpg"
            )
            viewPagerBanner.adapter = BannerAdapter(bannerList)

            // 2. MENU
            val rvMenu = view.findViewById<RecyclerView>(R.id.rvMenu)
            val menuList = listOf(
                MenuItem("Tất cả", R.drawable.ic_all),
                MenuItem("BXH", R.drawable.ic_rank),
                MenuItem("Free", R.drawable.ic_free),
            )
            rvMenu.layoutManager = GridLayoutManager(context, 5)
            rvMenu.adapter = MainMenuAdapter(menuList)

            // 3. HIỂN THỊ DỮ LIỆU
            setupRecyclerViews(view)

            view
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error loading UI: ${e.message}")
            View(context)
        }
    }

    private fun setupRecyclerViews(view: View) {
        try {
            val rvHotNovels = view.findViewById<RecyclerView>(R.id.rvHotNovels)
            val rvNewNovels = view.findViewById<RecyclerView>(R.id.rvNewNovels)

            // Lấy dữ liệu thực tế từ DatabaseHelper
            val novels = dbHelper.getAllNovels()

            rvHotNovels.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            rvHotNovels.adapter = NovelHorizontalAdapter(novels)

            rvNewNovels.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            rvNewNovels.adapter = NovelVerticalAdapter(novels)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error loading data: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { setupRecyclerViews(it) }
    }
}
