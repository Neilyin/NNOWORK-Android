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

    // Sakura
    val sakuraBackground = colorHex("#FFF5F8")
    val sakuraPrimary = colorHex("#FFACCF")
    val sakuraStamp = colorHex("#FF7597")
    val sakuraAccent = colorHex("#FF5E8E")
    val sakuraBorder = colorHex("#FF9CBF")
    val sakuraText = colorHex("#8A3A4E")

    // Lazy Cat
    val catBackground = colorHex("#FFF9E6")
    val catPrimary = colorHex("#FFB067")
    val catStamp = colorHex("#FF8C42")
    val catAccent = colorHex("#F9A826")
    val catBorder = colorHex("#FFD099")
    val catText = colorHex("#7A431D")

    // Notion 極簡黑白
    val notionBackground = colorHex("#F7F6F3")
    val notionSurface = colorHex("#FFFFFF")
    val notionText = colorHex("#191919")
    val notionSecondary = colorHex("#9B9A97")
    val notionBorder = colorHex("#E9E9E7")
    val notionStamp = colorHex("#191919")
    val notionAccent = colorHex("#37352F")
}
