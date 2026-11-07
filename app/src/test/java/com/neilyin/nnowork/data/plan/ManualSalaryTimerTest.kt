package com.neilyin.nnowork.data.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualSalaryTimerTest {
    @Test
    fun `start pause resume and stop preserve only active elapsed time`() {
        val initial = ResignationPlan(hourlyRate = 240.0)
        val running = ManualSalaryTimer.start(initial, now = 1_000L)
        assertEquals(ManualSalaryTimer.RUNNING, running.manualTimerStatus)
        assertEquals(4_000L, ManualSalaryTimer.elapsedMillis(running, now = 5_000L))

        val paused = ManualSalaryTimer.pause(running, now = 5_000L)
        assertEquals(ManualSalaryTimer.PAUSED, paused.manualTimerStatus)
        assertEquals(4_000L, ManualSalaryTimer.elapsedMillis(paused, now = 50_000L))

        val resumed = ManualSalaryTimer.start(paused, now = 50_000L)
        val stopped = ManualSalaryTimer.stop(resumed, now = 56_000L)
        assertEquals(ManualSalaryTimer.IDLE, stopped.manualTimerStatus)
        assertEquals(10_000L, ManualSalaryTimer.elapsedMillis(stopped, now = 90_000L))
    }

    @Test
    fun `starting after stop creates a fresh session`() {
        val stopped = ResignationPlan(
            manualTimerStatus = ManualSalaryTimer.IDLE,
            manualTimerElapsedMillis = 90_000L,
        )

        val restarted = ManualSalaryTimer.start(stopped, now = 200_000L)

        assertEquals(0L, restarted.manualTimerElapsedMillis)
        assertEquals(5_000L, ManualSalaryTimer.elapsedMillis(restarted, now = 205_000L))
    }

    @Test
    fun `earnings use configured hourly rate`() {
        val plan = ResignationPlan(
            hourlyRate = 200.0,
            manualTimerStatus = ManualSalaryTimer.PAUSED,
            manualTimerElapsedMillis = 90 * 60 * 1_000L,
        )

        assertEquals(300.0, ManualSalaryTimer.earned(plan, now = 0L), 0.001)
    }

    @Test
    fun `clock moving backwards never subtracts elapsed time`() {
        val plan = ResignationPlan(
            manualTimerStatus = ManualSalaryTimer.RUNNING,
            manualTimerStartedAt = 10_000L,
            manualTimerElapsedMillis = 4_000L,
        )

        assertEquals(4_000L, ManualSalaryTimer.elapsedMillis(plan, now = 9_000L))
    }

    @Test
    fun `manual hourly mode only needs an hourly rate to be configured`() {
        val manual = ResignationPlan(
            salaryPayMode = 1,
            hourlyTrackingMode = HourlyTrackingMode.MANUAL,
            hourlyRate = 200.0,
            workStartMinutes = 540,
            workEndMinutes = 540,
        )
        val scheduled = manual.copy(hourlyTrackingMode = HourlyTrackingMode.SCHEDULE)

        with(SalaryCalculator) {
            assertTrue(manual.isConfigured)
            assertFalse(scheduled.isConfigured)
        }
    }

    @Test
    fun `unknown hourly tracking mode falls back to scheduled mode`() {
        assertEquals(HourlyTrackingMode.SCHEDULE, HourlyTrackingMode.normalize(-1))
        assertEquals(HourlyTrackingMode.SCHEDULE, HourlyTrackingMode.normalize(99))
        assertEquals(HourlyTrackingMode.MANUAL, HourlyTrackingMode.normalize(HourlyTrackingMode.MANUAL))
    }
}
