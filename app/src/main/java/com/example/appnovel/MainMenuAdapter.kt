package com.example.appnovel

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. Định nghĩa dữ liệu Menu ngay tại đây
data class MenuItem(val name: String, val iconRes: Int)

class MainMenuAdapter(private val menuList: List<MenuItem>) :
    RecyclerView.Adapter<MainMenuAdapter.MenuViewHolder>() {

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMenu: ImageView = itemView.findViewById(R.id.imgMenu)
        val tvMenuName: TextView = itemView.findViewById(R.id.tvMenuName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.main_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuList[position]
        holder.tvMenuName.text = item.name
        holder.imgMenu.setImageResource(item.iconRes)
        holder.imgMenu.clipToOutline = true

        // Gắn sự kiện click để mở trang thêm truyện (Chỉ dành cho Admin)
        holder.itemView.setOnClickListener {
            if (item.name == "Tất cả") {
                val context = holder.itemView.context
                val intent = Intent(context, AdminAddActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = menuList.size
}
