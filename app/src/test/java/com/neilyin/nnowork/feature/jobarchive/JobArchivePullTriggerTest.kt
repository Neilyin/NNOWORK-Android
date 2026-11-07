package com.neilyin.nnowork.feature.jobarchive

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class JobArchivePullTriggerTest {
    private var pulls = 0
    private val trigger = JobArchivePullTrigger(CoroutineScope(Dispatchers.Unconfined)) { pulls++ }

    @Test
    fun `launch foreground is skipped because app start already pulled`() {
        trigger.onForeground(isSignedIn = true)
        assertEquals(0, pulls)
    }

    @Test
    fun `returning to foreground pulls only when signed in`() {
        trigger.onForeground(isSignedIn = true) // 啟動
        trigger.onForeground(isSignedIn = false)
        assertEquals(0, pulls)
        trigger.onForeground(isSignedIn = true)
        trigger.onForeground(isSignedIn = true)
        assertEquals(2, pulls)
    }

    @Test
    fun `signing in pulls once and the first observed state or signing out never does`() {
        trigger.onSignedInChanged(true) // 啟動時就已登入：只記錄
        assertEquals(0, pulls)
        trigger.onSignedInChanged(false)
        assertEquals(0, pulls)
        trigger.onSignedInChanged(true)
        assertEquals(1, pulls)
        trigger.onSignedInChanged(true) // 同狀態重複通知
        assertEquals(1, pulls)
    }
}
