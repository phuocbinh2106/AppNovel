package com.example.appnovel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class BannerAdapter(private val bannerImageList: List<String>) :
    RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgBanner: ImageView = itemView.findViewById(R.id.imgBanner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.main_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        // Sử dụng toán tử chia lấy dư để lặp lại danh sách ảnh
        val imageUrl = bannerImageList[position % bannerImageList.size]

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .into(holder.imgBanner)
    }

    // Trả về một con số rất lớn để có thể trượt "vô hạn"
    override fun getItemCount(): Int {
        return if (bannerImageList.isNotEmpty()) Int.MAX_VALUE else 0
    }
}
