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

    // iOS reui 全 App 語義色（PixelStyleModifiers.swift:31-131）
    val rpgComponentBorderSoft = colorHex("#D4DBE3")
    val sakuraSecondaryText = colorHex("#66515A")
    val sakuraDivider = colorHex("#E7D9DE")
    val sakuraControlBorderSoft = colorHex("#CDBCC2")
    val catSurface = colorHex("#FFFDF8")
    val catSecondaryText = colorHex("#62544C")
    val catDivider = colorHex("#DDD3C6")
    val catControlBorderSoft = colorHex("#C8B9AE")
    val notionControlBorderSoft = colorHex("#BCBCBA")
    val notionAccentSoft = colorHex("#EFEFED")
    /** iOS #5C8A96 在頁底只有 3.5:1，Android 加深到 AA。 */
    val summerSecondary = colorHex("#4D747E")
    val summerDivider = colorHex("#D5EDF2")
    val summerControlBorder = colorHex("#A8DAE4")
    val summerAccentSoft = colorHex("#D6F1F7")
    val summerOnAccent = colorHex("#04323C")
    val summerDanger = colorHex("#B4453A")
    val pandaSecondary = colorHex("#6E6E6E")
    val pandaDivider = colorHex("#E4E4E1")
    val pandaControlBorder = colorHex("#C9C9C4")
    val pandaAccentSoft = colorHex("#E8E8E5")
    val pandaDanger = colorHex("#B42318")
    /** iOS #8A7A63 在頁底只有 3.6:1，Android 加深到 AA。 */
    val slothSecondary = colorHex("#776955")
    val slothDivider = colorHex("#E6DCC8")
    val slothControlBorder = colorHex("#CFC0A5")
    val slothAccentSoft = colorHex("#E7EEDF")
    val slothOnAccent = colorHex("#2C2417")
    val slothDanger = colorHex("#A65240")

    // 夜空金（iOS YearReviewNightStyle.swift:26-60；固定深色場景）
    val nightSkyTop = colorHex("#181D26")
    val nightSkyBottom = colorHex("#232A36")
    val nightSurface = colorHex("#1F2633")
    val nightElevated = colorHex("#273042")
    val nightGold = colorHex("#F7C244")
    val nightAmber = colorHex("#EAA838")
    val nightStarWhite = colorHex("#EAEFF5")
    /** starWhite 72% 疊在 nightSurface 上的不透明值（iOS textSoft，約 7:1）。 */
    val nightTextSoft = colorHex("#B1B7BF")
    val nightBanner = colorHex("#3A3934")
    val nightCardBorder = colorHex("#C9A445")
    val nightEmptyCell = colorHex("#2A3344")
    val nightCoral = colorHex("#F2937F")
}
