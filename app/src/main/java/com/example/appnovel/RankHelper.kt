package com.example.appnovel

object RankHelper {
    val danhSachCanhGioi = listOf(
        CanhGioi(36, "Hóa Thần Sơ Kỳ",       300_000_000, "#8B4513", R.drawable.ic_lv9),
        CanhGioi(35, "Xung Kích Hóa Thần",    100_000_000, "#8B4513", R.drawable.ic_lv9),
        CanhGioi(34, "Rèn Luyện Thần Thức",    80_000_000, "#8B4513", R.drawable.ic_lv9),
        CanhGioi(33, "Nhục Thân Đàn Hóa",      60_000_000, "#8B4513", R.drawable.ic_lv9),
        CanhGioi(32, "Nguyên Anh Hậu Kỳ",      50_000_000, "#6B2D6B", R.drawable.ic_lv8),
        CanhGioi(31, "Nguyên Anh Trung Kỳ",    30_000_000, "#6B2D6B", R.drawable.ic_lv8),
        CanhGioi(30, "Nguyên Anh Sơ Kỳ",       10_000_000, "#6B2D6B", R.drawable.ic_lv8),
        CanhGioi(29, "Phá Đan Ngưng Anh",      14_000_000, "#6B2D6B", R.drawable.ic_lv8),
        CanhGioi(28, "Kim Đan Đại Viên Mãn",    7_000_000, "#1a4a4a", R.drawable.ic_lv7),
        CanhGioi(27, "Kim Đan Hậu Kỳ",          5_000_000, "#1a4a4a", R.drawable.ic_lv7),
        CanhGioi(26, "Kim Đan Trung Kỳ",        3_000_000, "#1a4a4a", R.drawable.ic_lv7),
        CanhGioi(25, "Kim Đan Sơ Kỳ",           1_000_000, "#1a4a4a", R.drawable.ic_lv7),
        CanhGioi(24, "Kết Đan",                   400_000, "#4a6b3a", R.drawable.ic_lv6),
        CanhGioi(23, "Đạo Cơ Đại Viên Mãn",      300_000, "#4a6b3a", R.drawable.ic_lv6),
        CanhGioi(22, "Đạo Cơ Hậu Kỳ",            200_000, "#4a6b3a", R.drawable.ic_lv6),
        CanhGioi(21, "Đạo Cơ Trung Kỳ",          100_000, "#4a6b3a", R.drawable.ic_lv6),
        CanhGioi(20, "Đạo Cơ Sơ Kỳ",              70_000, "#1e3a5f", R.drawable.ic_lv5),
        CanhGioi(19, "Ngưng Kết Đạo Cơ",          30_000, "#1e3a5f", R.drawable.ic_lv5),
        CanhGioi(18, "Luyện Khí Tầng 12",          30_000, "#1e3a5f", R.drawable.ic_lv5),
        CanhGioi(17, "Luyện Khí Tầng 11",          27_000, "#1e3a5f", R.drawable.ic_lv5),
        CanhGioi(16, "Luyện Khí Tầng 10",          25_000, "#6b2020", R.drawable.ic_lv4),
        CanhGioi(15, "Luyện Khí Tầng 9",           20_000, "#6b2020", R.drawable.ic_lv4),
        CanhGioi(14, "Luyện Khí Tầng 8",           17_000, "#6b2020", R.drawable.ic_lv4),
        CanhGioi(13, "Luyện Khí Tầng 7",           14_000, "#6b2020", R.drawable.ic_lv4),
        CanhGioi(12, "Luyện Khí Tầng 6",           10_000, "#1e4a2a", R.drawable.ic_lv3),
        CanhGioi(11, "Luyện Khí Tầng 5",            8_000, "#1e4a2a", R.drawable.ic_lv3),
        CanhGioi(10, "Luyện Khí Tầng 4",            7_000, "#1e4a2a", R.drawable.ic_lv3),
        CanhGioi(9,  "Luyện Khí Tầng 3",            5_200, "#1e4a2a", R.drawable.ic_lv3),
        CanhGioi(8,  "Luyện Khí Tầng 2",            2_600, "#1e3a5f", R.drawable.ic_lv2),
        CanhGioi(7,  "Luyện Khí Tầng 1",            1_600, "#1e3a5f", R.drawable.ic_lv2),
        CanhGioi(6,  "Tiên Thiên Võ Giả",           1_200, "#1e3a5f", R.drawable.ic_lv2),
        CanhGioi(5,  "Hậu Thiên Võ Giả",            1_200, "#1e3a5f", R.drawable.ic_lv2),
        CanhGioi(4,  "Nhất Lưu Võ Giả",               800, "#1e3a5f", R.drawable.ic_lv1),
        CanhGioi(3,  "Nhị Lưu Võ Giả",                500, "#1e3a5f", R.drawable.ic_lv1),
        CanhGioi(2,  "Tam Lưu Võ Giả",                300, "#1e3a5f", R.drawable.ic_lv1),
        CanhGioi(1,  "Bất Nhập Lưu Võ Giả",           100, "#1a1a2e", R.drawable.ic_lv1)
    )

    // Hàm tìm cảnh giới hiện tại dựa vào số EXP
    fun getCurrentRank(exp: Long): CanhGioi {
        return danhSachCanhGioi.filter { exp >= it.kn }.maxByOrNull { it.kn } ?: danhSachCanhGioi.last()
    }

    // Hàm tìm cảnh giới tiếp theo để tính tiến trình
    fun getNextRank(currentLevel: Int): CanhGioi? {
        return danhSachCanhGioi.find { it.level == currentLevel + 1 }
    }
}