package com.example.appnovel


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. Lớp dữ liệu cho mỗi nút Menu
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

        // Tạm thời dùng icon hệ thống, sau này bạn có thể thay bằng icon tự tải về
        holder.imgMenu.setImageResource(item.iconRes)

        // Mẹo: Bo tròn cái ImageView nếu bạn chưa làm trong XML
        holder.imgMenu.clipToOutline = true
    }

    override fun getItemCount() = menuList.size
}