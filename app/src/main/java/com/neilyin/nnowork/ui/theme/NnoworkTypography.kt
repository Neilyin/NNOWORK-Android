package com.neilyin.nnowork.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 角色化字級。方向 B 收斂成 M3 的 5 個角色族、8 個主要尺寸（另加 1 個數字 hero）：
 *
 * | 角色       | 欄位                 | sp |
 * |------------|----------------------|----|
 * | label S    | caption              | 11 |
 * | label      | small                | 13 |
 * | body       | body                 | 14 |
 * | body L     | bodyLarge／subtitle  | 16 |（subtitle 是 title-small，靠粗體與 bodyLarge 區分）
 * | title      | title                | 20 |
 * | headline   | heading              | 24 |
 * | page title | pageTitle            | 28 |（所有分頁大標題同一個大小）
 * | display    | display              | 32 |
 * | 數字 hero  | hero                 | 40 |（只給慶祝畫面的大數字）
 *
 * 字型分工見 [pixelTextStyle]：預設 Sans；數字、集點格、徽章、品牌標題才用等寬。
 */
@Immutable
data class NnoworkTypeScale(
    val caption: TextUnit,
    val small: TextUnit,
    val body: TextUnit,
    val bodyLarge: TextUnit,
    val subtitle: TextUnit,
    val title: TextUnit,
    val heading: TextUnit,
    val pageTitle: TextUnit,
    val display: TextUnit,
    val hero: TextUnit,
)

internal val DefaultTypeScale = NnoworkTypeScale(
    caption = 11.sp,
    small = 13.sp,
    body = 14.sp,
    bodyLarge = 16.sp,
    subtitle = 16.sp,
    title = 20.sp,
    heading = 24.sp,
    pageTitle = 28.sp,
    display = 32.sp,
    hero = 40.sp,
)

/** 每個主題的完整字級（目前全主題相同；之後可各主題分開調整）。 */
@Suppress("UnusedReceiverParameter")
val CardTheme.typeScale: NnoworkTypeScale
    get() = DefaultTypeScale
