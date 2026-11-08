package com.neilyin.nnowork.data.plan

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class HolidayCalendarTest {

    @After fun reset() { HolidayCalendar.regionOverride = null }

    private fun day(key: String): Long {
        val (y, m, d) = key.split("-").map(String::toInt)
        return Calendar.getInstance().apply { clear(); set(y, m - 1, d) }.timeInMillis
    }

    @Test fun regionFromPhone() {
        HolidayCalendar.regionOverride = "tw"
        assertEquals("TW", HolidayCalendar.currentRegion())
        HolidayCalendar.regionOverride = "FR"
        assertNull(HolidayCalendar.currentRegion())
        assertTrue(HolidayCalendar.holidays().isEmpty())
    }

    @Test fun allNineRegionsHave2027Data() {
        listOf("TW", "US", "JP", "CN", "HK", "MY", "SG", "CA", "AU").forEach {
            assertTrue(it, HolidayCalendar.holidays(it).any { k -> k.startsWith("2027-") })
        }
        assertTrue(HolidayCalendar.holidays("US").all { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) })
    }

    @Test fun chinaMakeupWeekendIsWorkday() {
        HolidayCalendar.regionOverride = "CN"
        val sunday = day("2027-01-31") // 春節調休補班（週日）
        assertTrue(SalaryCalculator.isWeekend(sunday))
        assertFalse(SalaryCalculator.isDefaultRedRestDay(sunday))
        assertTrue(SalaryCalculator.isDefaultRedRestDay(day("2027-02-10")))
    }

    @Test fun legacyRestDaysFallBackToDefaultsIn2027() {
        HolidayCalendar.regionOverride = "TW"
        // 舊資料只預選到 2026：2027 沒有任何 key
        val plan = ResignationPlan(monthlyScheduleMode = 0, customRestDays = setOf("2026-12-26", "2026-12-27"))
        assertTrue(SalaryCalculator.isRestDay(plan, day("2027-02-09")))  // 春節補假（週二）
        assertTrue(SalaryCalculator.isRestDay(plan, day("2027-01-02")))  // 週六
        assertFalse(SalaryCalculator.isRestDay(plan, day("2027-01-04"))) // 週一
        // 2026 照使用者的設定
        assertFalse(SalaryCalculator.isRestDay(plan, day("2026-12-25")))
    }

    @Test fun defaultRestDaysForYearIncludesHolidays() {
        HolidayCalendar.regionOverride = "JP"
        val set = SalaryCalculator.defaultRestDaysForYear(ResignationPlan(monthlyScheduleMode = 0), 2027)
        assertTrue(HolidayCalendar.holidays("JP").filter { it.startsWith("2027") }.all { it in set })
        assertTrue(set.all { it.startsWith("2027") })
    }

    @Test fun chineseNameForMainland() {
        assertEquals("中國大陸", HolidayCalendar.displayName("CN", Locale.TRADITIONAL_CHINESE))
        assertEquals("中国大陆", HolidayCalendar.displayName("CN", Locale.SIMPLIFIED_CHINESE))
    }
}
