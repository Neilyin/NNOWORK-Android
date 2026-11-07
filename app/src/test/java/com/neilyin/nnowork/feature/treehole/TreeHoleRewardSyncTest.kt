package com.neilyin.nnowork.feature.treehole

import com.neilyin.nnowork.data.reward.RewardSyncSummary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TreeHoleRewardSyncTest {
    private var canSync = true
    private var syncCalls = 0
    private var nextResult: () -> RewardSyncSummary = { RewardSyncSummary() }
    private val credited = mutableListOf<RewardSyncSummary>()
    private var gate: CompletableDeferred<Unit>? = null

    private val sync = TreeHoleRewardSync(
        scope = CoroutineScope(Dispatchers.Unconfined),
        canSync = { canSync },
        synchronize = {
            syncCalls++
            gate?.await()
            nextResult()
        },
        onCredited = { credited += it },
    )

    @Test
    fun `entering the tree hole syncs only after terms and profile allow community access`() {
        canSync = false
        sync.onScreenEntered()
        assertEquals(0, syncCalls)
        canSync = true
        sync.onScreenEntered()
        assertEquals(1, syncCalls)
    }

    @Test
    fun `every successful my-posts reload triggers a sync`() {
        sync.onMyPostsReloaded()
        sync.onMyPostsReloaded()
        assertEquals(2, syncCalls)
    }

    @Test
    fun `credited coins are reported per kind and empty results stay silent`() {
        sync.onScreenEntered()
        assertTrue(credited.isEmpty())
        nextResult = { RewardSyncSummary(approvedPostCoins = 10, feedbackRewardCoins = 10) }
        sync.onMyPostsReloaded()
        assertEquals(listOf(RewardSyncSummary(approvedPostCoins = 10, feedbackRewardCoins = 10)), credited)
    }

    @Test
    fun `a second trigger while one sync is running is coalesced`() = runBlocking {
        gate = CompletableDeferred()
        sync.onScreenEntered()
        sync.onMyPostsReloaded()
        assertEquals(1, syncCalls)
        gate!!.complete(Unit)
        sync.onMyPostsReloaded()
        assertEquals(2, syncCalls)
    }

    @Test
    fun `failures are silent and do not block the next trigger`() {
        nextResult = { throw IOException("offline") }
        sync.onScreenEntered()
        assertTrue(credited.isEmpty())
        nextResult = { RewardSyncSummary(approvedPostCoins = 5) }
        sync.onScreenEntered()
        assertEquals(1, credited.size)
    }
}
