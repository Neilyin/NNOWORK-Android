package com.neilyin.nnowork.feature.home

import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.util.DateUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * 忍耐大師階段總結的數字（iOS `MasterLevelReceiptView` 的 computed properties）。
 * 純資料，畫面與分享圖共用；[companyName] 為空時由呼叫端換成在地化的「公司」。
 */
data class MasterSummaryStats(
    val companyName: String,
    val firstStampDate: Long,
    val completionDate: Long,
    val totalDays: Int,
    val totalStamps: Int,
    val savedAmount: Double,
)

/**
 * iOS 算法：第一個章（全部紀錄最早一筆）到 cardCompletedDate（沒有就現在）的「日曆天」差；
 * 章數是全部紀錄數；金額是離職計畫目前存到的錢。
 */
fun computeMasterSummaryStats(
    settings: AppSettings,
    stamps: List<StampRecordEntity>,
    savedAmount: Double,
    nowMillis: Long = System.currentTimeMillis(),
): MasterSummaryStats {
    val first = stamps.minOfOrNull { it.date } ?: nowMillis
    val completion = settings.cardCompletedDate ?: nowMillis
    return MasterSummaryStats(
        companyName = settings.companyName.trim(),
        firstStampDate = first,
        completionDate = completion,
        totalDays = calendarDaysBetween(first, completion),
        totalStamps = stamps.size,
        savedAmount = savedAmount,
    )
}

/** 兩個時間點各自取當天 00:00 後相差幾天（iOS Calendar.dateComponents([.day])）；倒過來時回 0。 */
fun calendarDaysBetween(startMillis: Long, endMillis: Long): Int {
    val start = DateUtils.startOfDay(startMillis)
    val end = DateUtils.startOfDay(endMillis)
    if (end <= start) return 0
    // 用四捨五入吸收日光節約時間造成的 ±1 小時。
    return (TimeUnit.MILLISECONDS.toHours(end - start) / 24.0).roundToLong().toInt()
}

/** iOS：「$」＋千分位、無小數。 */
fun formatMasterSavedAmount(value: Double, locale: Locale = Locale.getDefault()): String {
    val format = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 0
        isGroupingUsed = true
    }
    return "$" + format.format(value)
}

/** iOS formatDate：yyyy/M/d。 */
fun formatMasterDate(millis: Long): String = SimpleDateFormat("yyyy/M/d", Locale.US).format(Date(millis))

/** iOS formatFullDate：yyyy/M/d HH:mm。 */
fun formatMasterFullDate(millis: Long): String = SimpleDateFormat("yyyy/M/d HH:mm", Locale.US).format(Date(millis))

/** 「再來一張」的格子數選項（iOS 10／20／30）。 */
val MASTER_SLOT_OPTIONS = listOf(10, 20, 30)
