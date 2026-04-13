package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        dbHelper = DatabaseHelper(requireContext())

        // 1. THANH TÌM KIẾM & THÔNG BÁO
        view.findViewById<LinearLayout>(R.id.layoutSearch).setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        view.findViewById<ImageView>(R.id.imgNotify).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        // 2. BANNER VÔ HẠN
        val viewPagerBanner = view.findViewById<ViewPager2>(R.id.viewPagerBanner)
        val bannerList = listOf(
            "https://i.pinimg.com/736x/02/49/62/024962a37e6a0228efe8f46c8afc25f6.jpg",
            "https://i.pinimg.com/1200x/bb/ea/b5/bbeab5567ebd2bc2028f3757e9024015.jpg",
            "https://i.pinimg.com/1200x/86/9b/9e/869b9eb4d075b381037f9e11f8470565.jpg"
        )
        viewPagerBanner.adapter = BannerAdapter(bannerList)

        if (bannerList.isNotEmpty()) {
            val initialPosition = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % bannerList.size)
            viewPagerBanner.setCurrentItem(initialPosition, false)
        }

        // 3. MENU
        val rvMenu = view.findViewById<RecyclerView>(R.id.rvMenu)
        val menuList = listOf(
            MenuItem("Tất cả", R.drawable.ic_all),
            MenuItem("BXH", R.drawable.ic_rank),
            MenuItem("Free", R.drawable.ic_free),
        )
        rvMenu.layoutManager = GridLayoutManager(context, 5)
        rvMenu.adapter = MainMenuAdapter(menuList)

        // 4. HIỂN THỊ DỮ LIỆU TỪ FIREBASE THEO PHÂN LOẠI
        setupFirebaseListeners(view)
        listenNotifications(view)

        return view
    }

    private fun setupFirebaseListeners(view: View) {
        val rvHotNovels = view.findViewById<RecyclerView>(R.id.rvHotNovels)
        val rvNewNovels = view.findViewById<RecyclerView>(R.id.rvNewNovels)

        // TRUYỆN NỔI BẬT: Sắp xếp theo Lượt xem (views) giảm dần
        firestore.collection("novels")
            .orderBy("views", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { value, error ->
                if (value != null) {
                    val hotList = value.toObjects(Novel::class.java)
                    rvHotNovels.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    rvHotNovels.adapter = NovelHorizontalAdapter(hotList)
                }
            }

        // MỚI CẬP NHẬT: Sắp xếp theo thời gian chương mới (lastChapterTimestamp) giảm dần
        firestore.collection("novels")
            .orderBy("lastChapterTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (value != null) {
                    val newList = value.toObjects(Novel::class.java)
                    rvNewNovels.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    rvNewNovels.adapter = NovelVerticalAdapter(newList)
                }
            }
    }

    private fun listenNotifications(view: View) {
        val userId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("userId", "") ?: ""
        if (userId.isEmpty()) return

        val notifyDot = view.findViewById<View>(R.id.viewNotifyDot)

        firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { value, _ ->
                if (value != null && !value.isEmpty) {
                    notifyDot.visibility = View.VISIBLE
                } else {
                    notifyDot.visibility = View.GONE
                }
            }
    }
}
