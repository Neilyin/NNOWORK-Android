package com.neilyin.nnowork.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 角色化字級。原本 feature 內散落 21 種 sp（9–48），收斂成 10 級，
 * 每個舊值對到最接近的角色（誤差表見 Phase A 回報）：
 * 9/10/11→caption、12/13→small、14/15→body、16/17→bodyLarge、18/19→subtitle、
 * 20/21→title、22/24/25→heading、28/30→pageTitle、36/38→display、48→hero。
 * Phase 3 要改：整組字級與字重層級（目前全用 Monospace、級距過密）。
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
    caption = 10.sp,
    small = 12.sp,
    body = 14.sp,
    bodyLarge = 16.sp,
    subtitle = 18.sp,
    title = 20.sp,
    heading = 24.sp,
    pageTitle = 30.sp,
    display = 38.sp,
    hero = 48.sp,
)

/** 每個主題的完整字級（Phase A 全主題相同；之後可各主題分開調整）。 */
@Suppress("UnusedReceiverParameter")
val CardTheme.typeScale: NnoworkTypeScale
    get() = DefaultTypeScale
