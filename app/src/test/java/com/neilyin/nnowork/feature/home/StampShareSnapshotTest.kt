package com.neilyin.nnowork.feature.home

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/** §20.3：周目要在蓋章當下擷取，與 round/stamp 一起；之後換工作周目改變也不影響這則分享。 */
class StampShareSnapshotTest {
    @Test
    fun `share sends the run number captured at stamp time not the current one`() = runBlocking {
        var currentRun = 1
        val snapshot = StampCreationResult(
            justCompleted = true,
            shareContent = "  今天又開了三小時會  ",
            roundNumber = 3,
            stampCount = 10,
            runNumber = captureRunNumber { currentRun },
        )
        // 蓋完章後才換工作：周目變 2、回合從 1 重算。
        currentRun = 2

        var sent: List<Any?>? = null
        submitStampShare(snapshot) { content, round, count, run -> sent = listOf(content, round, count, run) }
        assertEquals(listOf("今天又開了三小時會", 3, 10, 1), sent)
    }

    @Test
    fun `a failing run lookup does not block stamping and defers to the repository`() = runBlocking {
        assertNull(captureRunNumber { throw IOException("datastore") })
        assertEquals(4, captureRunNumber { 4 })
    }
}
