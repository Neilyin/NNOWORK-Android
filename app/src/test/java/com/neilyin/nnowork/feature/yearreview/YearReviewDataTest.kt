package com.neilyin.nnowork.feature.yearreview

import com.neilyin.nnowork.data.treehole.MyTreeHoleComment
import com.neilyin.nnowork.data.treehole.POST_TYPE_QUESTION
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.ui.theme.CardTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class YearReviewDataTest {
    private val zone = ZoneId.of("Asia/Taipei")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0, second: Int = 0, millis: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, minute, second, millis * 1_000_000).atZone(zone).toInstant().toEpochMilli()

    private fun stamp(millis: Long, reason: String = "累") = YearReviewStamp(millis, reason)

    private fun build(
        stamps: List<YearReviewStamp>,
        now: Long = at(2026, 12, 15),
        year: Int = 2026,
        card: YearReviewCardSnapshot = YearReviewCardSnapshot(),
        archives: List<YearReviewArchiveSnapshot> = emptyList(),
        saved: Double = 0.0,
        target: Double = 0.0,
        coins: List<YearReviewCoinEntry> = emptyList(),
        purchases: List<YearReviewPurchase> = emptyList(),
        treeHole: Boolean = false,
        nickname: String? = null,
    ) = YearReviewData.build(
        YearReviewInputs(
            year = year, stamps = stamps, card = card, archives = archives, savedAmount = saved, targetAmount = target,
            coinEntries = coins, purchases = purchases, nickname = nickname, hasTreeHoleProfile = treeHole,
            nowMillis = now, zone = zone,
        ),
    )

    @Test
    fun dedupesSameSecondAndReasonAndFiltersYear() {
        val data = build(
            listOf(
                stamp(at(2026, 3, 2, 10, 0, 5, 400), "A"),
                stamp(at(2026, 3, 2, 10, 0, 5, 0), "A"), // 封存只到秒：同一個章
                stamp(at(2026, 3, 2, 10, 0, 5, 0), "A "), // 原因不 trim：不同 key
                stamp(at(2025, 6, 1), "去年"),
                stamp(at(2027, 1, 2), "明年"),
            ),
        )
        assertEquals(2, data.totalStamps)
        // firstStamp 看全期
        assertEquals("去年", data.firstStamp?.reason)
    }

    @Test
    fun averageDaysCountsFromFirstStampToPeriodEndInclusive() {
        // 12/15 看當年：12/1 → 12/15 = 15 天，3 個章 → 5
        val data = build(listOf(stamp(at(2026, 12, 1)), stamp(at(2026, 12, 3)), stamp(at(2026, 12, 5))), now = at(2026, 12, 15))
        assertEquals(5.0, data.averageDaysPerStamp!!, 1e-9)
        assertNull(build(listOf(stamp(at(2026, 12, 1)))).averageDaysPerStamp)
    }

    @Test
    fun periodEndIsYearEndWhenReviewingLastYearInJanuary() {
        val data = build(listOf(stamp(at(2026, 12, 30))), now = at(2027, 1, 10))
        assertTrue(data.endsAtYearEnd)
        assertEquals(2, data.adventureDays) // 12/30 → 12/31
        // 今年才開始用（第一個章在 periodEnd 之後）→ 起點頁跳過
        val late = build(listOf(stamp(at(2027, 1, 5))), now = at(2027, 1, 10))
        assertNull(late.adventureDays)
        assertFalse(YearReviewPage.ORIGIN in late.pages)
    }

    @Test
    fun december15IsNotYearEnd() {
        val data = build(listOf(stamp(at(2026, 1, 1))), now = at(2026, 12, 15))
        assertFalse(data.endsAtYearEnd)
        assertEquals(349, data.adventureDays) // 1/1 → 12/15 含頭尾
    }

    @Test
    fun timeSlotsAndPeakHours() {
        assertEquals(YearReviewTimeSlot.EARLY_MORNING, YearReviewTimeSlot.forHour(5))
        assertEquals(YearReviewTimeSlot.EARLY_MORNING, YearReviewTimeSlot.forHour(8))
        assertEquals(YearReviewTimeSlot.WORK_MORNING, YearReviewTimeSlot.forHour(9))
        assertEquals(YearReviewTimeSlot.AFTERNOON, YearReviewTimeSlot.forHour(17))
        assertEquals(YearReviewTimeSlot.EVENING, YearReviewTimeSlot.forHour(22))
        assertEquals(YearReviewTimeSlot.LATE_NIGHT, YearReviewTimeSlot.forHour(23))
        assertEquals(YearReviewTimeSlot.LATE_NIGHT, YearReviewTimeSlot.forHour(4))

        // 深夜：0 時 1 個、23 時 1 個 → 時段內同數取先出現的 23
        val data = build(
            listOf(
                stamp(at(2026, 2, 1, 0)), stamp(at(2026, 2, 2, 23)),
                stamp(at(2026, 2, 3, 10)), stamp(at(2026, 2, 4, 10, 30)),
            ),
        )
        assertEquals(10, data.peakHour)
        // 上班 2 個、深夜 2 個 → 同數取順序較前的上班
        assertEquals(YearReviewTimeSlot.WORK_MORNING, data.dominantTimeSlot)
        assertEquals(23, data.peakHour(YearReviewTimeSlot.LATE_NIGHT))
        assertNull(data.peakHour(YearReviewTimeSlot.EVENING))
        // peakHour 同數取較早的小時
        val tie = build(listOf(stamp(at(2026, 2, 1, 15)), stamp(at(2026, 2, 1, 9))))
        assertEquals(9, tie.peakHour)
    }

    @Test
    fun weekdayAndMonthTiesPickEarlier() {
        // 2026-03-01 是週日、03-02 週一
        val data = build(listOf(stamp(at(2026, 3, 1)), stamp(at(2026, 3, 2)), stamp(at(2026, 5, 4))))
        assertEquals(DayOfWeek.SUNDAY, LocalDate.of(2026, 3, 1).dayOfWeek)
        // 週一 2、週日 1
        assertEquals(DayOfWeek.MONDAY, data.peakWeekday)
        assertEquals(3, data.peakMonth)
        val tie = build(listOf(stamp(at(2026, 3, 1)), stamp(at(2026, 3, 7))))
        // 週日與週六各 1 → 依週一→週日，先遇到週六
        assertEquals(DayOfWeek.SATURDAY, tie.peakWeekday)
    }

    @Test
    fun longestStreakKeepsEarlierRunOnTie() {
        val data = build(
            listOf(
                stamp(at(2026, 3, 2)), stamp(at(2026, 3, 3)), stamp(at(2026, 3, 3, 18)),
                stamp(at(2026, 4, 10)), stamp(at(2026, 4, 11)),
                stamp(at(2026, 5, 1)),
            ),
        )
        val streak = data.longestStreak!!
        assertEquals(2, streak.length)
        assertEquals(LocalDate.of(2026, 3, 2), streak.start)
        assertEquals(LocalDate.of(2026, 3, 3), streak.end)
        val longer = build((1..5).map { stamp(at(2026, 6, it)) } + stamp(at(2026, 1, 1)))
        assertEquals(5, longer.longestStreak!!.length)
    }

    @Test
    fun completedCardsAndMasterFromCurrentCard() {
        val card = YearReviewCardSnapshot(
            currentRound = 4,
            isCardLocked = true,
            cardCompletedDate = at(2026, 9, 1),
            roundCompletedDates = listOf(at(2025, 12, 1), at(2026, 2, 1), at(2026, 5, 1)),
        )
        val data = build(listOf(stamp(at(2026, 1, 1))), card = card)
        // 今年 2 筆 + 鎖卡（3 < 4）1 筆
        assertEquals(3, data.completedCardCount)
        // 第 3 筆（5/1）在今年
        assertTrue(data.becameMaster)
        val notMaster = build(listOf(stamp(at(2026, 1, 1))), card = YearReviewCardSnapshot(currentRound = 2, roundCompletedDates = listOf(at(2026, 2, 1))))
        assertEquals(1, notMaster.completedCardCount)
        assertFalse(notMaster.becameMaster)
    }

    @Test
    fun completedCardsFromArchives() {
        val archive = YearReviewArchiveSnapshot(
            endDate = at(2026, 8, 1),
            isMaster = false,
            completionDate = at(2026, 7, 20),
            lastCardCompleted = YearReviewArchiveSnapshot.lastCardCompleted(false, at(2026, 7, 20), at(2026, 8, 1)),
            stamps = listOf(
                YearReviewArchivedStamp(at(2025, 11, 1), 1),
                YearReviewArchivedStamp(at(2025, 12, 31), 1), // 第 1 回合最後一章在去年 → 不算
                YearReviewArchivedStamp(at(2026, 3, 1), 2), // 第 2 回合（非最後）→ +1
                YearReviewArchivedStamp(at(2026, 4, 1), 3), // 第 3 回合（非最後）→ +1、忍耐大師
                YearReviewArchivedStamp(at(2026, 7, 20), 4), // 最後回合：收據集滿 → +1
            ),
        )
        val data = build(listOf(stamp(at(2026, 1, 1))), archives = listOf(archive))
        assertEquals(3, data.completedCardCount)
        assertTrue(data.becameMaster)
        assertEquals(1, data.jobsChanged)
        assertTrue(YearReviewPage.CARDS in data.pages)
    }

    @Test
    fun lastCardCompletedComparesReceiptTimesNotEndDate() {
        assertFalse(YearReviewArchiveSnapshot.lastCardCompleted(false, 1_000_000L, 1_000_500L))
        assertTrue(YearReviewArchiveSnapshot.lastCardCompleted(false, 1_000_000L, 1_001_000L))
        assertTrue(YearReviewArchiveSnapshot.lastCardCompleted(true, 1_000_000L, 1_000_000L))
    }

    @Test
    fun savingsCoinsAndThemes() {
        val data = build(
            List(6) { stamp(at(2026, 2, it + 1)) },
            saved = 33_399.0, target = 100_000.0,
            coins = listOf(
                YearReviewCoinEntry(10, "dailyStamp", at(2026, 3, 1)),
                YearReviewCoinEntry(500, "migration", at(2026, 3, 1)),
                YearReviewCoinEntry(5, "debugGrant", at(2026, 3, 1)),
                YearReviewCoinEntry(-15, "themeUnlock", at(2026, 3, 1)),
                YearReviewCoinEntry(7, "dailyStamp", at(2025, 3, 1)),
            ),
            purchases = listOf(
                YearReviewPurchase("theme.summer", at(2026, 5, 1)),
                YearReviewPurchase("theme.sakura", at(2026, 4, 1)),
                YearReviewPurchase("theme.summer", at(2026, 6, 1)),
                YearReviewPurchase("theme.panda", at(2025, 6, 1)),
            ),
        )
        assertEquals(33, data.savingsPercent)
        assertEquals(10, data.coinsEarned)
        assertEquals(listOf(CardTheme.SAKURA, CardTheme.SUMMER), data.purchasedThemes)
        assertTrue(YearReviewPage.SAVINGS in data.pages)
        assertTrue(YearReviewPage.COINS in data.pages)
        assertEquals(100, build(List(6) { stamp(at(2026, 2, it + 1)) }, saved = 200.0, target = 100.0).savingsPercent)
    }

    @Test
    fun litePagesAndTitles() {
        val empty = build(emptyList())
        assertEquals(
            listOf(YearReviewPage.OPENING, YearReviewPage.STAMP_COUNT, YearReviewPage.TITLE, YearReviewPage.SUMMARY),
            empty.pages,
        )
        assertTrue(empty.isLite)
        assertNull(empty.titleParts)

        val lite = build(List(4) { stamp(at(2026, 2, it + 1), "r$it") }, target = 100.0, saved = 10.0)
        assertTrue(lite.isLite)
        assertFalse(YearReviewPage.TIME_OF_DAY in lite.pages)
        assertFalse(YearReviewPage.SAVINGS in lite.pages)
        assertFalse(YearReviewPage.STREAK in lite.pages)

        val full = build(List(25) { stamp(at(2026, 3, 2 + (it % 5) * 7, 23, it)) })
        assertFalse(full.isLite)
        val parts = full.titleParts!!
        assertEquals(DayOfWeek.MONDAY, parts.weekday)
        assertEquals(YearReviewTimeSlot.LATE_NIGHT, parts.slot)
        assertEquals(YearReviewTitleRank.WARRIOR, parts.rank)
    }

    @Test
    fun titleRankThresholds() {
        assertEquals(YearReviewTitleRank.APPRENTICE, YearReviewTitleRank.forStamps(19))
        assertEquals(YearReviewTitleRank.WARRIOR, YearReviewTitleRank.forStamps(20))
        assertEquals(YearReviewTitleRank.WARRIOR, YearReviewTitleRank.forStamps(59))
        assertEquals(YearReviewTitleRank.KNIGHT, YearReviewTitleRank.forStamps(60))
        assertEquals(YearReviewTitleRank.OVERLORD, YearReviewTitleRank.forStamps(150))
    }

    @Test
    fun treeHolePageFollowsLoadState() {
        val loading = build(listOf(stamp(at(2026, 2, 1))), treeHole = true, nickname = "  小牛  ")
        assertEquals("小牛", loading.nickname)
        assertTrue(YearReviewPage.TREE_HOLE in loading.pages)
        assertFalse(YearReviewPage.TREE_HOLE in loading.withTreeHole(null).pages)
        assertFalse(YearReviewPage.TREE_HOLE in loading.withTreeHole(YearReviewTreeHoleStats()).pages)
        assertTrue(YearReviewPage.TREE_HOLE in loading.withTreeHole(YearReviewTreeHoleStats(exp = 10)).pages)
        assertFalse(YearReviewPage.TREE_HOLE in build(listOf(stamp(at(2026, 2, 1)))).pages)
        assertNull(build(emptyList(), nickname = "   ").nickname)
    }

    @Test
    fun treeHoleStatsCountOnlyApprovedThisYear() {
        val posts = listOf(
            TreeHolePost(id = "1", status = "approved", createdAt = "2026-03-01T10:00:00.123456+00:00", likesCount = 2, cheerCount = 1, cryCount = 1),
            TreeHolePost(id = "2", status = "approved", createdAt = "2026-04-01T10:00:00Z", postType = POST_TYPE_QUESTION, likesCount = 1),
            TreeHolePost(id = "3", status = "pending", createdAt = "2026-04-01T10:00:00Z"),
            TreeHolePost(id = "4", status = "approved", createdAt = "2025-12-31T15:00:00Z"), // 台北 2025-12-31 23:00
            TreeHolePost(id = "5", status = "approved", createdAt = "2025-12-31T17:00:00Z"), // 台北 2026-01-01 01:00
        )
        val comments = listOf(
            MyTreeHoleComment(id = "c1", status = "approved", createdAt = "2026-05-01T00:00:00Z"),
            MyTreeHoleComment(id = "c2", status = "rejected", createdAt = "2026-05-01T00:00:00Z"),
            MyTreeHoleComment(id = "c3", status = "approved", createdAt = "not a date"),
        )
        val stats = YearReviewTreeHoleStatsBuilder.make(posts, comments, exp = 120, helpful = -1, year = 2026, zone = zone)
        assertEquals(2, stats.stampPosts)
        assertEquals(1, stats.questions)
        assertEquals(5, stats.reactions)
        assertEquals(1, stats.comments)
        assertEquals(120, stats.exp)
        assertEquals(0, stats.helpful)
        assertTrue(stats.hasContent)
        assertFalse(YearReviewTreeHoleStats(hearts = 3).hasContent)
    }
}
