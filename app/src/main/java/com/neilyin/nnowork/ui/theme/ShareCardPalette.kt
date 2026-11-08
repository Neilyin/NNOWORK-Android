package com.neilyin.nnowork.ui.theme

import androidx.compose.ui.graphics.toArgb

/**
 * 分享圖（離職收據、樹洞分享卡）用的固定色票，給 android.graphics.Canvas 直接用的 ARGB int。
 * 分享圖是固定外觀的「實體卡片」，不跟 App 主題走，但色值集中在 ui/theme 管理。
 */
object ShareCardPalette {
    /** 收據外圍底色（一般／大師）。 */
    val receiptBackdrop: Int = colorHex("#16212C").toArgb()
    val receiptMasterBackdrop: Int = colorHex("#23182B").toArgb()
    /** 收據強調色（一般藍／大師紫）。 */
    val receiptAccent: Int = colorHex("#1B69AB").toArgb()
    val receiptMasterAccent: Int = colorHex("#6F2D91").toArgb()
    val paper: Int = PixelColors.white.toArgb()
    val paperBorder: Int = PixelColors.black.toArgb()
    val ink: Int = colorHex("#232323").toArgb()
    val mutedInk: Int = colorHex("#5F6970").toArgb()
    val money: Int = colorHex("#1C9150").toArgb()
    val dashedDivider: Int = colorHex("#91979C").toArgb()

    /** 樹洞分享卡。 */
    val treeHoleBackdrop: Int = colorHex("#242329").toArgb()
    val treeHoleAccent: Int = colorHex("#FFC83D").toArgb()
    val treeHoleMutedInk: Int = colorHex("#5B5A63").toArgb()
    val treeHoleCard: Int = colorHex("#FFFFFF").toArgb()
    val treeHoleInk: Int = colorHex("#000000").toArgb()
}
