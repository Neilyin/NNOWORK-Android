package com.neilyin.nnowork.feature.yearreview

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * DEBUG「使用示範資料」：固定種子產生一整年的章、卡片、封存、預備金、金幣與樹洞數字，
 * 讓期間外、資料少的測試機也看得到每一頁。不讀也不寫任何本機資料。
 */
object YearReviewDemo {
    val treeHole = YearReviewTreeHoleStats(
        stampPosts = 12, questions = 3, hearts = 48, cheers = 21, cries = 9, comments = 17, exp = 640, helpful = 4,
    )

    fun inputs(context: Context, year: Int, nowMillis: Long = System.currentTimeMillis()): YearReviewInputs {
        val zone = ZoneId.systemDefault()
        val rng = Random(year)
        val reasons = listOf("老闆廢話太多", "老闆廢話太多", "無效會議", "需求一直改", "同事雷", "加班沒錢", "週一又要開晨會")
        val end = minOf(LocalDate.of(year, 12, 31), YearReviewWindow.today(nowMillis, zone).takeIf { it.year == year } ?: LocalDate.of(year, 12, 31))
        val stamps = mutableListOf<YearReviewStamp>()
        var day = LocalDate.of(year, 1, 6)
        while (!day.isAfter(end)) {
            // 週一多一點、深夜多一點
            val chance = if (day.dayOfWeek == java.time.DayOfWeek.MONDAY) 0.75 else 0.22
            if (rng.nextDouble() < chance) {
                val hour = listOf(23, 23, 0, 10, 14, 20)[rng.nextInt(6)]
                val at = day.atTime(hour, rng.nextInt(60)).atZone(zone).toInstant().toEpochMilli()
                stamps += YearReviewStamp(at, reasons[rng.nextInt(reasons.size)])
            }
            day = day.plusDays(1)
        }
        // 一段 6 天的連續
        for (offset in 0 until 6) {
            val at = LocalDate.of(year, 3, 9).plusDays(offset.toLong()).atTime(22, 30).atZone(zone).toInstant().toEpochMilli()
            stamps += YearReviewStamp(at, "加班沒錢")
        }
        fun millis(month: Int, dayOfMonth: Int) = LocalDate.of(year, month, dayOfMonth).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val archive = YearReviewArchiveSnapshot(
            endDate = millis(6, 30),
            isMaster = false,
            completionDate = millis(6, 20),
            lastCardCompleted = true,
            stamps = listOf(
                YearReviewArchivedStamp(millis(2, 1), 1), YearReviewArchivedStamp(millis(3, 1), 2),
                YearReviewArchivedStamp(millis(4, 1), 3), YearReviewArchivedStamp(millis(6, 20), 4),
            ),
        )
        return YearReviewInputs(
            year = year,
            stamps = stamps,
            card = YearReviewCardSnapshot(currentRound = 2, roundCompletedDates = listOf(millis(9, 15))),
            archives = listOf(archive),
            savedAmount = 128_000.0,
            targetAmount = 200_000.0,
            coinEntries = listOf(
                YearReviewCoinEntry(stamps.size, "dailyStamp", millis(5, 1)),
                YearReviewCoinEntry(30, "treeHolePostApproved", millis(7, 1)),
            ),
            purchases = listOf(YearReviewPurchase("theme.sakura", millis(4, 2)), YearReviewPurchase("theme.summer", millis(8, 2))),
            nickname = context.getString(com.neilyin.nnowork.R.string.yr_debug_demo_nickname),
            hasTreeHoleProfile = true,
            nowMillis = nowMillis,
            zone = zone,
            builtInReasons = YearReviewBuiltInReasons.canonicalMap(context),
            builtInDisplay = YearReviewBuiltInReasons.displayMap(context),
            fallbackLanguage = yearReviewAppLanguage(context.resources.configuration.locales[0]),
        )
    }
}
