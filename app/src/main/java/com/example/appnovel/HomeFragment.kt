package com.example.appnovel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // --- 1. XỬ LÝ BANNER SLIDER ---
        val viewPagerBanner = view.findViewById<ViewPager2>(R.id.viewPagerBanner)
        val bannerList = listOf(
            "https://img.upanh.tv/2023/12/04/Toan-Tri-Doc-Gia-Web-Banner-1920x1080.jpg",
            "https://img.upanh.tv/2023/12/04/Solo-Leveling-Web-Banner.jpg"
        )
        viewPagerBanner.adapter = BannerAdapter(bannerList)


        // --- 2. XỬ LÝ MENU CHỨC NĂNG (MỚI CẬP NHẬT) ---
        val rvMenu = view.findViewById<RecyclerView>(R.id.rvMenu)
        val menuList = listOf(
            MenuItem("Tất cả", R.drawable.ic_all),
            MenuItem("BXH", R.drawable.ic_rank),
            MenuItem("Free", R.drawable.ic_free),
        )
        // Dùng GridLayoutManager với spanCount = 5 để chia đều 5 nút trên 1 hàng
        rvMenu.layoutManager = GridLayoutManager(context, 5)
        rvMenu.adapter = MainMenuAdapter(menuList)


        // --- 3. XỬ LÝ TRUYỆN NỔI BẬT ---
        val rvHotNovels = view.findViewById<RecyclerView>(R.id.rvHotNovels)
        val hotList = mutableListOf<Novel>()
        hotList.add(Novel("Toàn Trí Độc Giả", "Sing N Song", "https://upload.wikimedia.org/wikipedia/vi/a/a7/Omniscient_Reader%27s_Viewpoint_cover.jpg", status = "Đang ra"))
        hotList.add(Novel("Solo Leveling", "Chugong", "https://upload.wikimedia.org/wikipedia/vi/1/1c/Solo_Leveling_Webtoon.png", status = "Hoàn thành"))

        rvHotNovels.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvHotNovels.adapter = NovelHorizontalAdapter(hotList)


        // --- 4. XỬ LÝ MỚI CẬP NHẬT ---
        val rvNewNovels = view.findViewById<RecyclerView>(R.id.rvNewNovels)
        val newList = mutableListOf<Novel>()
        newList.add(Novel("Phàm Nhân Tu Tiên", "Vong Ngữ", "https://picsum.photos/200/300", chapter = "Chương 2450", time = "1 phút trước"))

        rvNewNovels.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rvNewNovels.adapter = NovelVerticalAdapter(newList)

        return view
    }
}