package com.neilyin.nnowork.feature.yearreview

import android.content.Context
import com.neilyin.nnowork.R
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** 年度回顧的文字格式：跟著 App 目前的語系（resources 的 locale），不跟系統地區。 */
class YearReviewFormat(private val context: Context) {
    val locale: Locale = context.resources.configuration.locales[0]

    fun string(id: Int, vararg args: Any): String = context.getString(id, *args)

    fun plural(id: Int, count: Int, vararg args: Any): String = context.resources.getQuantityString(id, count, *args)

    fun number(value: Int): String = NumberFormat.getIntegerInstance(locale).format(value)

    /** `$` 前綴刻意固定（只代表「錢」，不跟薪水幣別）；千分位、無小數。 */
    fun money(value: Double): String = "$" + NumberFormat.getIntegerInstance(locale).apply {
        maximumFractionDigits = 0
    }.format(value.roundToInt())

    /** 日文 24 小時制（「23時」），其他語系 12 小時制配時段詞。 */
    fun hour(hour: Int): String {
        val uses24Hour = locale.language == "ja"
        val h12 = if (uses24Hour) hour else if (hour % 12 == 0) 12 else hour % 12
        val res = when (hour) {
            in 0..4 -> R.string.yr_hour_night
            in 5..11 -> R.string.yr_hour_morning
            12 -> R.string.yr_hour_noon
            in 13..17 -> R.string.yr_hour_afternoon
            else -> R.string.yr_hour_evening
        }
        return string(res, h12)
    }

    fun weekdayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL_STANDALONE, locale)

    fun weekdayShort(day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT_STANDALONE, locale)

    fun monthName(month: Int): String = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, locale)

    /** 「3月2日」「Mar 2」 */
    fun monthDay(date: LocalDate): String = DateTimeFormatter.ofPattern(skeleton("MMMd"), locale).format(date)

    /** 「2025年3月2日」「Mar 2, 2025」 */
    fun fullDate(date: LocalDate): String = DateTimeFormatter.ofPattern(skeleton("yMMMd"), locale).format(date)

    /** 「2025/3/2」「3/2/2025」 */
    fun shortDate(date: LocalDate): String = DateTimeFormatter.ofPattern(skeleton("yMd"), locale).format(date)

    private fun skeleton(template: String): String = android.text.format.DateFormat.getBestDateTimePattern(locale, template)

    fun slotTypeName(slot: YearReviewTimeSlot): String = string(
        when (slot) {
            YearReviewTimeSlot.EARLY_MORNING -> R.string.yr_slot_type_early_morning
            YearReviewTimeSlot.WORK_MORNING -> R.string.yr_slot_type_work_morning
            YearReviewTimeSlot.AFTERNOON -> R.string.yr_slot_type_afternoon
            YearReviewTimeSlot.EVENING -> R.string.yr_slot_type_evening
            YearReviewTimeSlot.LATE_NIGHT -> R.string.yr_slot_type_late_night
        },
    )

    /** 年度稱號：最崩潰星期＋時段＋程度；精簡版用專屬稱號。 */
    fun annualTitle(data: YearReviewData): String {
        val parts = data.titleParts
            ?: return string(if (data.totalStamps == 0) R.string.yr_title_lite_zero else R.string.yr_title_lite_few)
        return string(R.string.yr_title_format, titleWeekday(parts.weekday), titleSlot(parts.slot), rankName(parts.rank))
    }

    fun annualTitleDescription(data: YearReviewData): String = when {
        data.titleParts != null -> string(R.string.yr_title_desc_full)
        data.totalStamps == 0 -> string(R.string.yr_title_desc_zero)
        else -> string(R.string.yr_title_desc_few)
    }

    private fun titleWeekday(day: DayOfWeek): String = string(
        when (day) {
            DayOfWeek.SUNDAY -> R.string.yr_title_weekday_sun
            DayOfWeek.MONDAY -> R.string.yr_title_weekday_mon
            DayOfWeek.TUESDAY -> R.string.yr_title_weekday_tue
            DayOfWeek.WEDNESDAY -> R.string.yr_title_weekday_wed
            DayOfWeek.THURSDAY -> R.string.yr_title_weekday_thu
            DayOfWeek.FRIDAY -> R.string.yr_title_weekday_fri
            DayOfWeek.SATURDAY -> R.string.yr_title_weekday_sat
        },
    )

    private fun titleSlot(slot: YearReviewTimeSlot): String = string(
        when (slot) {
            YearReviewTimeSlot.EARLY_MORNING -> R.string.yr_title_slot_dawn
            YearReviewTimeSlot.WORK_MORNING -> R.string.yr_title_slot_morning
            YearReviewTimeSlot.AFTERNOON -> R.string.yr_title_slot_afternoon
            YearReviewTimeSlot.EVENING -> R.string.yr_title_slot_evening
            YearReviewTimeSlot.LATE_NIGHT -> R.string.yr_title_slot_night
        },
    )

    private fun rankName(rank: YearReviewTitleRank): String = string(
        when (rank) {
            YearReviewTitleRank.APPRENTICE -> R.string.yr_rank_apprentice
            YearReviewTitleRank.WARRIOR -> R.string.yr_rank_warrior
            YearReviewTitleRank.KNIGHT -> R.string.yr_rank_knight
            YearReviewTitleRank.OVERLORD -> R.string.yr_rank_overlord
        },
    )

    /** 原因顯示：內建原因翻成目前語系，自訂原因／關鍵字用原文。 */
    fun reasonText(item: YearReviewReasonCount): String =
        item.builtInKey?.let { key -> YearReviewBuiltInReasons.entries.firstOrNull { it.first == key }?.let { string(it.second) } }
            ?: item.text
}
