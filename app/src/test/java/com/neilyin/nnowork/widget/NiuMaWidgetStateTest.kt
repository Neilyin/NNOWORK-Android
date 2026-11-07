package com.neilyin.nnowork.widget

import com.neilyin.nnowork.data.plan.HourlyTrackingMode
import com.neilyin.nnowork.data.plan.ManualSalaryTimer
import com.neilyin.nnowork.data.plan.ResignationPlan
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NiuMaWidgetStateTest {
    private fun at(month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(2026, month - 1, day, hour, minute, 0)
        }.timeInMillis

    /** 月薪 30000、09:00–18:00、11 月共 30 天 → 日薪 1000。 */
    private val monthly = ResignationPlan(
        monthlySalary = 30_000.0,
        workStartMinutes = 9 * 60,
        workEndMinutes = 18 * 60,
        paydayDays = setOf(1),
        customRestDays = setOf("2026-11-07", "2026-10-09"),
    )

    @Test
    fun `unconfigured plan`() {
        assertEquals(NiuMaState.NotConfigured, NiuMaWidgetState.select(ResignationPlan(), at(11, 4, 10)))
    }

    @Test
    fun `before work carries the shift start`() {
        val state = NiuMaWidgetState.select(monthly, at(11, 4, 7)) as NiuMaState.BeforeWork
        assertEquals(at(11, 4, 9), state.startsAt)
    }

    @Test
    fun `working computes earnings progress and countdown`() {
        val state = NiuMaWidgetState.select(monthly, at(11, 4, 13, 30)) as NiuMaState.Working
        assertEquals(500.0, state.earned, 0.01)
        assertEquals(0.5f, state.progress, 0.001f)
        assertEquals(4L * 60 * 60 * 1000 + 30 * 60 * 1000, state.countdownMillis)
        assertEquals(at(11, 4, 18), state.offWorkAt)
        assertEquals(state.earned + 3_000.0, state.cycleEarned, 0.01) // 11/1..11/3 各 1000
    }

    @Test
    fun `off work reports the full day wage`() {
        val state = NiuMaWidgetState.select(monthly, at(11, 4, 19)) as NiuMaState.OffWork
        assertEquals(1000.0, state.dailyWage, 0.01)
    }

    @Test
    fun `monthly rest day is paid`() {
        val state = NiuMaWidgetState.select(monthly, at(11, 7, 12)) as NiuMaState.RestDay
        assertEquals(1000.0, state.paidAmount, 0.01)
    }

    @Test
    fun `hourly rest day is unpaid`() {
        val hourly = monthly.copy(salaryPayMode = 1, hourlyRate = 200.0)
        val state = NiuMaWidgetState.select(hourly, at(11, 7, 12)) as NiuMaState.RestDay
        assertEquals(0.0, state.paidAmount, 0.0)
    }

    @Test
    fun `national holiday on monthly pay`() {
        val state = NiuMaWidgetState.select(monthly, at(10, 9, 12)) as NiuMaState.Holiday
        assertEquals(30_000.0 / 31, state.dailyWage, 0.01)
    }

    @Test
    fun `national holiday on hourly pay stays a plain rest day`() {
        val hourly = monthly.copy(salaryPayMode = 1, hourlyRate = 200.0)
        assertTrue(NiuMaWidgetState.select(hourly, at(10, 9, 12)) is NiuMaState.RestDay)
    }

    @Test
    fun `night shift keeps working after midnight then goes off work`() {
        val night = monthly.copy(workStartMinutes = 22 * 60, workEndMinutes = 6 * 60)
        val working = NiuMaWidgetState.select(night, at(11, 5, 2)) as NiuMaState.Working
        assertEquals(0.5f, working.progress, 0.001f)
        assertEquals(at(11, 5, 6), working.offWorkAt)
        assertEquals(500.0, working.earned, 0.01)

        val off = NiuMaWidgetState.select(night, at(11, 5, 7)) as NiuMaState.OffWork
        assertEquals(1000.0, off.dailyWage, 0.01)
    }

    private val manualBase = ResignationPlan(
        salaryPayMode = 1,
        hourlyTrackingMode = HourlyTrackingMode.MANUAL,
        hourlyRate = 200.0,
        paydayDays = setOf(1),
    )

    @Test
    fun `manual timer running paused and stopped`() {
        val now = at(11, 4, 12)
        val running = ManualSalaryTimer.start(manualBase, now - 30 * 60_000L)
        val r = NiuMaWidgetState.select(running, now) as NiuMaState.Manual
        assertEquals(NiuMaState.ManualStatus.RUNNING, r.status)
        assertEquals(100.0, r.earned, 0.01)
        assertEquals(30 * 60_000L, r.elapsedMillis)

        val paused = ManualSalaryTimer.pause(running, now)
        val p = NiuMaWidgetState.select(paused, now + 3_600_000L) as NiuMaState.Manual
        assertEquals(NiuMaState.ManualStatus.PAUSED, p.status)
        assertEquals(30 * 60_000L, p.elapsedMillis)

        val stopped = ManualSalaryTimer.stop(paused, now)
        val s = NiuMaWidgetState.select(stopped, now) as NiuMaState.Manual
        assertEquals(NiuMaState.ManualStatus.STOPPED, s.status)
        assertEquals(100.0, s.earned, 0.01)
    }

    @Test
    fun `refresh cadence per state`() {
        val now = at(11, 4, 13)
        val working = NiuMaWidgetState.select(monthly, now)
        assertEquals(now + NiuMaWidgetState.TICK_MILLIS, NiuMaWidgetState.nextRefreshAt(working, now))

        val nearEnd = at(11, 4, 17, 59) + 30_000L
        val ending = NiuMaWidgetState.select(monthly, nearEnd)
        assertEquals(at(11, 4, 18) + 1_000L, NiuMaWidgetState.nextRefreshAt(ending, nearEnd))

        val before = NiuMaWidgetState.select(monthly, at(11, 4, 7))
        assertEquals(at(11, 4, 9), NiuMaWidgetState.nextRefreshAt(before, at(11, 4, 7)))

        val off = NiuMaWidgetState.select(monthly, at(11, 4, 19))
        assertEquals(at(11, 5, 0), NiuMaWidgetState.nextRefreshAt(off, at(11, 4, 19)))

        val running = NiuMaWidgetState.select(ManualSalaryTimer.start(manualBase, now - 1000), now)
        assertNotNull(NiuMaWidgetState.nextRefreshAt(running, now))
        val paused = NiuMaWidgetState.select(ManualSalaryTimer.pause(ManualSalaryTimer.start(manualBase, now - 1000), now), now)
        assertNull(NiuMaWidgetState.nextRefreshAt(paused, now))
        assertNull(NiuMaWidgetState.nextRefreshAt(NiuMaState.NotConfigured, now))
    }
}
