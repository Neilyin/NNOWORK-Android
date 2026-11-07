package com.neilyin.nnowork.feature.history

import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.feature.home.calendarDaysBetween

/** 自由畫面「冒險成果」（iOS FreedomView.Snapshot）。[companyName] 為空時畫面換成在地化的「公司」。 */
data class FreedomStats(
    val companyName: String,
    val cards: Int,
    val stamps: Int,
    val days: Int,
)

/**
 * iOS makeSnapshot：完成卡片＝目前第幾張（currentRound）、總蓋章數＝全部紀錄、
 * 忍耐天數＝第一個章到這張卡完成日（沒有就今天）的日曆天差。
 */
fun computeFreedomStats(
    settings: AppSettings,
    stamps: List<StampRecordEntity>,
    nowMillis: Long = System.currentTimeMillis(),
): FreedomStats {
    val first = stamps.minOfOrNull { it.date } ?: nowMillis
    val end = settings.cardCompletedDate ?: nowMillis
    return FreedomStats(
        companyName = settings.companyName.trim(),
        cards = settings.currentRound.coerceAtLeast(1),
        stamps = stamps.size,
        days = calendarDaysBetween(first, end),
    )
}

/** iOS FreedomView.join：前半段以全形標點結尾（中日文）就直接接，否則（英文）補半形空格。 */
fun joinFreedomLine(a: String, b: String): String {
    val last = a.lastOrNull() ?: return b
    return if (last in "！，、。：") a + b else "$a $b"
}

/** 對話框全文：三段，沿用既有翻譯字串組合。 */
fun freedomDialogText(
    congrats: String,
    free: String,
    bye: String,
    companyLine: String,
    wish1: String,
    wish2: String,
): String = listOf(
    joinFreedomLine(congrats, free),
    joinFreedomLine(bye, companyLine),
    wish1 + "\n" + wish2,
).joinToString("\n")
