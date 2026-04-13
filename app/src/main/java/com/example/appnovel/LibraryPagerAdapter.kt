package com.example.appnovel

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class LibraryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LichSuFragment()
            1 -> TuTruyenFragment()
            else -> LichSuFragment()
        }
    }
}