package com.neilyin.nnowork.feature.yearreview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class YearReviewWindowTest {

    @Test
    fun reviewYearFollowsDecemberTenthToJanuaryThirtyFirst() {
        assertNull(YearReviewWindow.reviewYear(LocalDate.of(2026, 11, 7)))
        assertNull(YearReviewWindow.reviewYear(LocalDate.of(2026, 12, 9)))
        assertEquals(2026, YearReviewWindow.reviewYear(LocalDate.of(2026, 12, 10)))
        assertEquals(2026, YearReviewWindow.reviewYear(LocalDate.of(2026, 12, 31)))
        // 1 月看前一年
        assertEquals(2026, YearReviewWindow.reviewYear(LocalDate.of(2027, 1, 1)))
        assertEquals(2026, YearReviewWindow.reviewYear(LocalDate.of(2027, 1, 31)))
        assertNull(YearReviewWindow.reviewYear(LocalDate.of(2027, 2, 1)))
        assertNull(YearReviewWindow.reviewYear(LocalDate.of(2027, 6, 15)))
    }

    @Test
    fun earlyAccessOpensSettingsEntryOutsideWindowForCurrentYear() {
        val today = LocalDate.of(2026, 11, 7)
        assertNull(YearReviewWindow.availableYear(today, earlyAccess = false))
        assertEquals(2026, YearReviewWindow.availableYear(today, earlyAccess = true))
        // 期間內不受開關影響；1 月仍看前一年
        assertEquals(2026, YearReviewWindow.availableYear(LocalDate.of(2027, 1, 5), earlyAccess = true))
    }

    @Test
    fun simulateWindowOnlyAffectsInvite() {
        val today = LocalDate.of(2026, 11, 7)
        assertNull(YearReviewWindow.inviteWindowYear(today, simulateWindow = false))
        assertEquals(2026, YearReviewWindow.inviteWindowYear(today, simulateWindow = true))
        assertEquals(2026, YearReviewWindow.forcedYear(today, simulateWindow = false))
        assertEquals(2026, YearReviewWindow.forcedYear(LocalDate.of(2027, 1, 3), simulateWindow = false))
    }

    @Test
    fun todayUsesDeviceZoneAcrossYearBoundary() {
        val taipei = ZoneId.of("Asia/Taipei")
        // UTC 12/31 16:30 = 台北 1/1 00:30 → 已經是 1 月，回顧 2026
        val instant = ZonedDateTime.of(2026, 12, 31, 16, 30, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        val today = YearReviewWindow.today(instant, taipei)
        assertEquals(LocalDate.of(2027, 1, 1), today)
        assertEquals(2026, YearReviewWindow.reviewYear(today))
        // 同一個瞬間在紐約還是 12/31
        val newYork = YearReviewWindow.today(instant, ZoneId.of("America/New_York"))
        assertEquals(LocalDate.of(2026, 12, 31), newYork)
        assertEquals(2026, YearReviewWindow.reviewYear(newYork))
    }

    @Test
    fun inviteShownKeyMatchesIos() {
        assertEquals("year_review_invite_shown_2026", YearReviewInvitePolicy.shownKey(2026))
        assertEquals("year_review_test_early_access", YearReviewTestFlags.EARLY_ACCESS_KEY)
        assertEquals("year_review_test_simulate_invite_window", YearReviewTestFlags.SIMULATE_INVITE_WINDOW_KEY)
        assertEquals("debug_year_review_load_delay", YearReviewTestFlags.LOAD_DELAY_KEY)
    }

    @Test
    fun inviteShowsOncePerYearAndOnlyWithStamps() {
        assertTrue(YearReviewInvitePolicy.shouldInvite(2026, alreadyShown = false, hasStampsInYear = true))
        // 看過就不再邀請
        assertFalse(YearReviewInvitePolicy.shouldInvite(2026, alreadyShown = true, hasStampsInYear = true))
        // 沒章不邀請
        assertFalse(YearReviewInvitePolicy.shouldInvite(2026, alreadyShown = false, hasStampsInYear = false))
        // 期間外不邀請
        assertFalse(YearReviewInvitePolicy.shouldInvite(null, alreadyShown = false, hasStampsInYear = true))
    }

    @Test
    fun inviteNeedsTwoConsecutiveIdleChecks() {
        assertNull(YearReviewInvitePolicy.firstShowableCheck(emptyList()))
        assertNull(YearReviewInvitePolicy.firstShowableCheck(listOf(true)))
        assertNull(YearReviewInvitePolicy.firstShowableCheck(listOf(true, false, true)))
        assertEquals(1, YearReviewInvitePolicy.firstShowableCheck(listOf(true, true)))
        assertEquals(4, YearReviewInvitePolicy.firstShowableCheck(listOf(false, true, false, true, true)))
    }

    @Test
    fun anyTestFlagOnMarksTesting() {
        assertFalse(YearReviewTestFlags().anyOn)
        assertTrue(YearReviewTestFlags(loadDelay = true).anyOn)
        assertTrue(YearReviewTestFlags(earlyAccess = true).anyOn)
    }
}
