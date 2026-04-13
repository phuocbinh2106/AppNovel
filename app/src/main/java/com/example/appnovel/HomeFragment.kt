package com.example.appnovel

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private var notifyListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 1. HEADER & NOTIFICATION
        view.findViewById<LinearLayout>(R.id.layoutSearch).setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        view.findViewById<ImageView>(R.id.imgNotify).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        // 2. BANNER VÔ HẠN (Đã khôi phục)
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

        // 3. XỬ LÝ NÚT XEM THÊM
        view.findViewById<TextView>(R.id.tvSeeMoreHot).setOnClickListener {
            val intent = Intent(requireContext(), NovelListActivity::class.java)
            intent.putExtra("TITLE", "TRUYỆN NỔI BẬT")
            startActivity(intent)
        }

        view.findViewById<TextView>(R.id.tvSeeMoreNew).setOnClickListener {
            val intent = Intent(requireContext(), NovelListActivity::class.java)
            intent.putExtra("TITLE", "MỚI CẬP NHẬT")
            startActivity(intent)
        }

        // 4. LOAD DỮ LIỆU CÁC MỤC KHÁC
        setupHotNovels(view)
        setupNewNovels(view)
        setupRanking(view)
        listenNotifications(view)

        return view
    }

    private fun setupHotNovels(view: View) {
        val rvHot = view.findViewById<RecyclerView>(R.id.rvHotNovels)
        firestore.collection("novels")
            .orderBy("views", Query.Direction.DESCENDING)
            .limit(6)
            .addSnapshotListener { value, _ ->
                if (value != null && isAdded) {
                    val list = value.toObjects(Novel::class.java)
                    rvHot.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    rvHot.adapter = NovelHorizontalAdapter(list)
                }
            }
    }

    private fun setupNewNovels(view: View) {
        val rvNew = view.findViewById<RecyclerView>(R.id.rvNewNovels)
        firestore.collection("novels")
            .orderBy("lastChapterTimestamp", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { value, _ ->
                if (value != null && isAdded) {
                    val list = value.toObjects(Novel::class.java)
                    rvNew.layoutManager = LinearLayoutManager(context)
                    rvNew.adapter = NovelVerticalAdapter(list)
                }
            }
    }

    private fun setupRanking(view: View) {
        val rvRanking = view.findViewById<RecyclerView>(R.id.rvRanking)
        val tvWeek = view.findViewById<TextView>(R.id.tvTopWeek)
        val tvMonth = view.findViewById<TextView>(R.id.tvTopMonth)
        val tvAll = view.findViewById<TextView>(R.id.tvTopAll)

        fun updateTabUI(selected: TextView) {
            val unselectedColor = 0xFFAAAAAA.toInt()
            val selectedColor = 0xFFFB923C.toInt()

            tvWeek.setTextColor(unselectedColor)
            tvWeek.setTypeface(null, Typeface.NORMAL)
            tvMonth.setTextColor(unselectedColor)
            tvMonth.setTypeface(null, Typeface.NORMAL)
            tvAll.setTextColor(unselectedColor)
            tvAll.setTypeface(null, Typeface.NORMAL)

            selected.setTextColor(selectedColor)
            selected.setTypeface(null, Typeface.BOLD)
        }

        fun loadRankingData(orderByField: String) {
            firestore.collection("novels")
                .orderBy(orderByField, Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { value ->
                    if (value != null && isAdded) {
                        val list = value.toObjects(Novel::class.java)
                        rvRanking.layoutManager = LinearLayoutManager(context)
                        rvRanking.adapter = RankingAdapter(list)
                    }
                }
        }

        tvWeek.setOnClickListener {
            updateTabUI(tvWeek)
            loadRankingData("views")
        }

        tvMonth.setOnClickListener {
            updateTabUI(tvMonth)
            loadRankingData("views")
        }

        tvAll.setOnClickListener {
            updateTabUI(tvAll)
            loadRankingData("views")
        }

        // Khởi tạo mặc định
        updateTabUI(tvWeek)
        loadRankingData("views")
    }

    private fun listenNotifications(view: View) {
        val userId = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .getString("userId", "") ?: ""
        
        val notifyDot = view.findViewById<View>(R.id.viewNotifyDot)
        if (userId.isEmpty()) {
            notifyDot.visibility = View.GONE
            return
        }

        notifyListener?.remove()
        notifyListener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { value, _ ->
                if (isAdded) {
                    notifyDot.visibility = if (value != null && !value.isEmpty) View.VISIBLE else View.GONE
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notifyListener?.remove()
    }
}
