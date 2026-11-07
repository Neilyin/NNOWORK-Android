package com.neilyin.nnowork.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 8-bit 風格、精緻化的調色盤。
 * 對應 iOS 版 PixelColors（PixelStyleModifiers.swift）。
 */
fun colorHex(hex: String): Color {
    val clean = hex.trim().removePrefix("#")
    return when (clean.length) {
        3 -> {
            val r = clean[0].digitToInt(16) * 17
            val g = clean[1].digitToInt(16) * 17
            val b = clean[2].digitToInt(16) * 17
            Color(r, g, b)
        }
        6 -> {
            val v = clean.toLong(16)
            Color(
                red = ((v shr 16) and 0xFF).toInt(),
                green = ((v shr 8) and 0xFF).toInt(),
                blue = (v and 0xFF).toInt()
            )
        }
        8 -> {
            val v = clean.toLong(16)
            Color(
                alpha = ((v shr 24) and 0xFF).toInt(),
                red = ((v shr 16) and 0xFF).toInt(),
                green = ((v shr 8) and 0xFF).toInt(),
                blue = (v and 0xFF).toInt()
            )
        }
        else -> Color.Black
    }
}

object PixelColors {
    val red = colorHex("#E05252")
    val yellow = colorHex("#F4D03F")
    val blue = colorHex("#5DADE2")
    val green = colorHex("#58D68D")
    val purple = colorHex("#AF7AC5")
    val orange = colorHex("#F5B041")
    val turquoise = colorHex("#48C9B0")

    val gray = colorHex("#95A5A6")
    val darkGray = colorHex("#2C3E50")
    val white = colorHex("#FDFEFE")
    val black = colorHex("#17202A")

    // 主題背景色
    val rpgBackground = colorHex("#212F3D")
    val cyberpunkBackground = colorHex("#050505")
    val vacationBackground = colorHex("#FAD7A0")

    // Sakura（低彩度底色，高對比文字）
    val sakuraBackground = colorHex("#FFF8FA")
    val sakuraPrimary = colorHex("#E58EAE")
    val sakuraStamp = colorHex("#C94F79")
    val sakuraAccent = colorHex("#B93C68")
    val sakuraBorder = colorHex("#D9A2B5")
    val sakuraText = colorHex("#442431")

    // Lazy Cat（暖米色底，焦糖橘互動色）
    val catBackground = colorHex("#F8F3E8")
    val catPrimary = colorHex("#E9A15C")
    val catStamp = colorHex("#C96B20")
    val catAccent = colorHex("#B85C0D")
    val catBorder = colorHex("#D4A56F")
    val catText = colorHex("#3F2B21")

    // Notion 極簡黑白
    val notionBackground = colorHex("#F7F6F3")
    val notionSurface = colorHex("#FFFFFF")
    val notionText = colorHex("#191919")
    val notionSecondary = colorHex("#5F5E5B")
    val notionBorder = colorHex("#E9E9E7")
    val notionStamp = colorHex("#191919")
    val notionAccent = colorHex("#37352F")

    // Summer（水藍）
    val summerBackground = colorHex("#EAF7FA")
    val summerSurface = colorHex("#FFFFFF")
    val summerPrimary = colorHex("#0E7C93")
    val summerAccent = colorHex("#2BB3C7")
    val summerBorder = colorHex("#BFE6EE")
    val summerStamp = colorHex("#2BB3C7")
    val summerText = colorHex("#0B4A57")
    val summerEmptyCell = colorHex("#D8F0F5")

    // Panda（黑白灰；用較大的圓角與 Notion 區隔）
    val pandaBackground = colorHex("#F2F2F0")
    val pandaSurface = colorHex("#FFFFFF")
    val pandaPrimary = colorHex("#1C1C1C")
    val pandaAccent = colorHex("#4A4A4A")
    val pandaBorder = colorHex("#DCDCD8")
    val pandaStamp = colorHex("#FFFFFF")
    val pandaText = colorHex("#1C1C1C")
    val pandaEmptyCell = colorHex("#E6E6E2")

    // Sloth（大地色）
    val slothBackground = colorHex("#F5EFE3")
    val slothSurface = colorHex("#FFFBF3")
    val slothPrimary = colorHex("#6B5B45")
    val slothAccent = colorHex("#8FA37A")
    val slothBorder = colorHex("#E0D5C0")
    val slothStamp = colorHex("#FFFBF3")
    val slothText = colorHex("#4A3F31")
    val slothEmptyCell = colorHex("#EAE0CE")
}
