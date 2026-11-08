package com.neilyin.nnowork.feature.home

import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

/** 忍耐大師階段總結的數字要跟 iOS MasterLevelReceiptView 一致。 */
class MasterSummaryStatsTest {
    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, 0, 0)
        }.timeInMillis

    @Test
    fun `days run from the earliest stamp to the card completion date in calendar days`() {
        val stamps = listOf(
            StampRecordEntity(date = at(2026, 3, 5, 9), roundNumber = 2),
            StampRecordEntity(date = at(2026, 1, 1, 23), roundNumber = 1),
            StampRecordEntity(date = at(2026, 2, 1, 8), roundNumber = 3),
        )
        val settings = AppSettings(companyName = "  牛馬科技 ", currentRound = 3, cardCompletedDate = at(2026, 1, 31, 1))
        val stats = computeMasterSummaryStats(settings, stamps, savedAmount = 12345.6, nowMillis = at(2026, 6, 1))

        assertEquals("牛馬科技", stats.companyName)
        assertEquals(at(2026, 1, 1, 23), stats.firstStampDate)
        assertEquals(at(2026, 1, 31, 1), stats.completionDate)
        // 1/1 23:00 → 1/31 01:00：日曆天差 30（不是 29.08 天取整）。
        assertEquals(30, stats.totalDays)
        // 章數是全部紀錄（跨回合），不是只算這一張。
        assertEquals(3, stats.totalStamps)
        assertEquals(12345.6, stats.savedAmount, 0.0)
    }

    @Test
    fun `missing completion date falls back to now and no stamps gives zero days`() {
        val now = at(2026, 4, 10)
        val empty = computeMasterSummaryStats(AppSettings(), emptyList(), 0.0, nowMillis = now)
        assertEquals(0, empty.totalDays)
        assertEquals(0, empty.totalStamps)
        assertEquals(now, empty.firstStampDate)
        assertEquals(now, empty.completionDate)

        val withStamp = computeMasterSummaryStats(
            AppSettings(cardCompletedDate = null),
            listOf(StampRecordEntity(date = at(2026, 4, 3))),
            0.0,
            nowMillis = now,
        )
        assertEquals(7, withStamp.totalDays)
    }

    @Test
    fun `reversed dates never produce negative days`() {
        assertEquals(0, calendarDaysBetween(at(2026, 5, 2), at(2026, 5, 1)))
        assertEquals(0, calendarDaysBetween(at(2026, 5, 1, 1), at(2026, 5, 1, 23)))
        assertEquals(365, calendarDaysBetween(at(2025, 1, 1), at(2026, 1, 1)))
    }

    @Test
    fun `saved amount uses dollar sign grouping and no decimals`() {
        assertEquals("$12,346", formatMasterSavedAmount(12345.6, Locale.US))
        assertEquals("$0", formatMasterSavedAmount(0.0, Locale.US))
    }

    @Test
    fun `dates use the ios receipt patterns`() {
        assertEquals("2026/3/5", formatMasterDate(at(2026, 3, 5, 9)))
        assertEquals("2026/3/5 09:00", formatMasterFullDate(at(2026, 3, 5, 9)))
    }

    @Test
    fun `next card slot options match ios`() {
        assertEquals(listOf(10, 20, 30), MASTER_SLOT_OPTIONS)
    }
}
