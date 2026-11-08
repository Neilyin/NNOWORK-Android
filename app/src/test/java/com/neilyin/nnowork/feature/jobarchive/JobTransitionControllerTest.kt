package com.neilyin.nnowork.feature.jobarchive

import com.neilyin.nnowork.data.jobarchive.JobTransitionException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class JobTransitionControllerTest {
    private class FakeActions : JobTransitionActions {
        val archiveCalls = mutableListOf<Pair<String, Int>>()
        val discardCalls = mutableListOf<Pair<String, Int>>()
        var archiveError: Exception? = null
        var discardError: Exception? = null
        var unfinished = false

        override suspend fun archiveAndStartNewJob(newCompanyName: String, maxStamps: Int): List<String> {
            archiveCalls += newCompanyName to maxStamps
            archiveError?.let { throw it }
            return listOf("todo-a")
        }

        override suspend fun startNewJobWithoutArchive(newCompanyName: String, maxStamps: Int): List<String> {
            discardCalls += newCompanyName to maxStamps
            discardError?.let { throw it }
            return listOf("todo-d")
        }

        override suspend fun hasUnfinishedUploadedArchive(): Boolean = unfinished
    }

    private val actions = FakeActions()
    private val cancelled = mutableListOf<List<String>>()
    private val controller = JobTransitionController(actions) { cancelled += it }

    @Test
    fun `cancelling the discard confirmation never starts a new job`() = runBlocking {
        controller.updateCompanyName("新公司")
        controller.requestDiscard()
        assertTrue(controller.state.value.showDiscardConfirm)
        assertTrue("opening the confirmation alone changes nothing", actions.discardCalls.isEmpty())

        controller.dismissDiscard()
        // 取消後就算再呼叫 confirm（例如重複點擊殘留的事件）也不能執行。
        controller.confirmDiscard()

        assertFalse(controller.state.value.showDiscardConfirm)
        assertTrue(actions.discardCalls.isEmpty())
        assertTrue(cancelled.isEmpty())
        assertFalse(controller.state.value.completed)
    }

    @Test
    fun `confirmed discard runs once with the trimmed name and cancels cleared reminders`() = runBlocking {
        controller.updateCompanyName("  新公司  ")
        controller.selectMaxStamps(30)
        controller.requestDiscard()
        controller.confirmDiscard()
        controller.confirmDiscard() // 第二次：確認框已關閉，不會再跑一次。

        assertEquals(listOf("新公司" to 30), actions.discardCalls)
        assertEquals(listOf(listOf("todo-d")), cancelled)
        assertTrue(controller.state.value.completed)
        assertFalse(controller.state.value.processing)
        assertTrue(actions.archiveCalls.isEmpty())
    }

    @Test
    fun `blank company name blocks both actions`() = runBlocking {
        controller.updateCompanyName("   ")
        controller.requestDiscard()
        controller.archive()
        assertFalse(controller.state.value.showDiscardConfirm)
        assertTrue(actions.archiveCalls.isEmpty())
        assertTrue(actions.discardCalls.isEmpty())
        assertFalse(controller.state.value.canProceed)
    }

    @Test
    fun `invalid card sizes are ignored`() {
        controller.selectMaxStamps(15)
        assertEquals(10, controller.state.value.maxStamps)
        controller.selectMaxStamps(20)
        assertEquals(20, controller.state.value.maxStamps)
    }

    @Test
    fun `archive success completes and cancels the cleared reminders`() = runBlocking {
        controller.updateCompanyName("新公司")
        controller.archive()
        assertEquals(listOf("新公司" to 10), actions.archiveCalls)
        assertEquals(listOf(listOf("todo-a")), cancelled)
        assertTrue(controller.state.value.completed)
        assertNull(controller.state.value.error)
    }

    @Test
    fun `archive failure shows the mapped error and touches nothing`() = runBlocking {
        controller.updateCompanyName("新公司")
        actions.archiveError = JobTransitionException.ServerNotReady()
        actions.unfinished = true
        controller.archive()
        assertEquals(JobTransitionError.SERVER_NOT_READY, controller.state.value.error)
        assertFalse(controller.state.value.completed)
        assertFalse(controller.state.value.processing)
        assertTrue(cancelled.isEmpty())
        assertTrue("refreshes the unfinished-archive hint after a failure", controller.state.value.hasUnfinishedArchive)

        controller.dismissError()
        actions.archiveError = JobTransitionException.Server("boom")
        controller.archive()
        assertEquals(JobTransitionError.SERVER, controller.state.value.error)
        assertEquals("boom", controller.state.value.serverMessage)
    }

    @Test
    fun `unexpected errors fall back like iOS`() {
        assertEquals(JobTransitionError.NETWORK, JobTransitionController.errorKind(IOException("x"), JobTransitionError.NETWORK))
        assertEquals(JobTransitionError.LOCAL_SAVE, JobTransitionController.errorKind(IOException("x"), JobTransitionError.LOCAL_SAVE))
        assertEquals(JobTransitionError.NOT_SIGNED_IN, JobTransitionController.errorKind(JobTransitionException.NotSignedIn(), JobTransitionError.NETWORK))
        assertEquals(JobTransitionError.LOCAL_SAVE, JobTransitionController.errorKind(JobTransitionException.LocalSaveFailed(), JobTransitionError.NETWORK))
    }

    @Test
    fun `discard failure keeps the screen open with a local save error`() = runBlocking {
        controller.updateCompanyName("新公司")
        actions.discardError = IllegalStateException("disk")
        controller.requestDiscard()
        controller.confirmDiscard()
        assertEquals(JobTransitionError.LOCAL_SAVE, controller.state.value.error)
        assertFalse(controller.state.value.completed)
        assertTrue(cancelled.isEmpty())
    }

    @Test
    fun `reset after closing gives the next open a fresh screen`() = runBlocking {
        controller.updateCompanyName("新公司")
        controller.selectMaxStamps(20)
        controller.archive()
        assertTrue(controller.state.value.completed)
        controller.reset()
        assertEquals(JobTransitionUiState(), controller.state.value)
    }

    @Test
    fun `load surfaces the unfinished archive hint`() = runBlocking {
        actions.unfinished = true
        controller.load()
        assertTrue(controller.state.value.hasUnfinishedArchive)
    }
}
