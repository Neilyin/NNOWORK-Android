package com.neilyin.nnowork.data.jobarchive

import com.neilyin.nnowork.data.backup.BackupDateCodec
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.plan.TodoItem
import com.neilyin.nnowork.data.settings.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.ZoneOffset

/** §19.6 資料安全順序：上傳失敗本機不動；本機提交失敗保留 pending；重試沿用同一 id。 */
class JobTransitionCoordinatorTest {
    private val owner = "user-1"
    private val now = BackupDateCodec.parse("2026-10-05T08:30:00Z")

    private class FakeLocal(
        var settings: AppSettings,
        var stamps: MutableList<StampRecordEntity>,
        var plan: ResignationPlan,
    ) : JobTransitionLocalStore {
        val archives = mutableListOf<JobArchiveEntity>()
        lateinit var pendingRef: MemoryPending
        /** Room transaction 失敗（整個 rollback）。 */
        var failCommit = false
        /** 模擬 process death：Room 已提交、DataStore 還沒寫。 */
        var crashAfterRoomPhase = false
        var commits = 0
        var dataStoreCommits = 0

        override suspend fun readCurrentJob() = CurrentJobSnapshot(settings, stamps.toList(), plan)
        override suspend fun archivesForOwner(owner: String) = archives.filter { it.ownerUserId == owner }
        override suspend fun commitRoomPhase(archive: JobArchiveEntity?, stampIds: Set<String>) {
            if (failCommit) throw IOException("disk full")
            archive?.let { candidate ->
                if (archives.none { it.ownerUserId == candidate.ownerUserId && it.id == candidate.id }) archives += candidate
            }
            stamps.removeAll { it.id in stampIds }
            commits++
            if (crashAfterRoomPhase) throw ProcessDeath()
        }
        override suspend fun resetCurrentJobAndClearPending(
            pendingKey: String?,
            transform: (AppSettings, ResignationPlan) -> Pair<AppSettings, ResignationPlan>,
        ) {
            val (newSettings, newPlan) = transform(settings, plan)
            settings = newSettings
            plan = newPlan
            if (pendingKey != null) pendingRef.entries = pendingRef.entries - pendingKey
            dataStoreCommits++
        }
        override suspend fun insertArchivesIfAbsent(archives: List<JobArchiveEntity>): Int {
            var inserted = 0
            archives.forEach { candidate ->
                if (this.archives.none { it.ownerUserId == candidate.ownerUserId && it.id == candidate.id }) {
                    this.archives += candidate
                    inserted++
                }
            }
            return inserted
        }
        override suspend fun claimUnowned(ids: Set<String>, owner: String): Int {
            var claimed = 0
            archives.replaceAll { archive ->
                if (archive.ownerUserId.isEmpty() && archive.id in ids) { claimed++; archive.copy(ownerUserId = owner) } else archive
            }
            return claimed
        }
    }

    private class FakeRemote : JobArchiveRemote {
        val uploads = mutableListOf<Pair<String, JobArchivePayload>>()
        var failUpload: Exception? = null
        var rows: List<JobArchiveRemoteRow> = emptyList()
        override suspend fun upload(id: String, payload: JobArchivePayload): String {
            failUpload?.let { throw it }
            uploads += id to payload
            return id
        }
        override suspend fun fetchAll() = rows
    }

    private class MemoryPending : JobArchivePendingStore {
        var entries: Map<String, JobArchivePendingEntry> = emptyMap()
        override suspend fun load() = entries
        override suspend fun save(entries: Map<String, JobArchivePendingEntry>) { this.entries = entries }
    }

    private fun stamp(id: String, date: String, reason: String, round: Int) =
        StampRecordEntity(id = id, date = BackupDateCodec.parse(date), reason = reason, roundNumber = round)

    private fun fixture(): FakeLocal = FakeLocal(
        settings = AppSettings(
            companyName = "舊公司",
            currentRound = 3,
            isCardLocked = true,
            cardCompletedDate = BackupDateCodec.parse("2026-10-01T09:00:00Z"),
            maxStamps = 10,
            currentJobIndex = 1,
            currentRunNumber = 0,
            unlockedStyles = listOf(1, 2, 3, 7),
            cardStyle = 7,
            coinCount = 99,
        ),
        stamps = mutableListOf(
            stamp("00000000-0000-4000-8000-000000000001", "2026-03-02T01:15:00Z", "無效會議", 1),
            stamp("00000000-0000-4000-8000-000000000002", "2026-05-02T01:15:00Z", "同事雷", 2),
            stamp("00000000-0000-4000-8000-000000000003", "2026-09-02T01:15:00Z", "無效會議", 3),
        ),
        plan = ResignationPlan(
            targetAmount = 100_000.0,
            currentSaved = 12_000.0,
            hasResume = true,
            todoItems = listOf(TodoItem(id = "t1", title = "更新履歷", reminderAt = 1L, notificationScheduled = true)),
            completedTodoItems = listOf(TodoItem(id = "t2", title = "存錢")),
            monthlySalary = 45_000.0,
        ),
    )

    private class ProcessDeath : Exception("process killed")

    private fun coordinator(local: FakeLocal, remote: FakeRemote, pending: MemoryPending, ids: MutableList<String>): JobTransitionCoordinator {
        local.pendingRef = pending
        return JobTransitionCoordinator(local, remote, pending, nowMillis = { now }, newUuid = { ids.removeAt(0) })
    }

    @Test
    fun `crash after Room write before DataStore write is rolled forward on recovery`() = runBlocking {
        val local = fixture().apply { crashAfterRoomPhase = true }
        val remote = FakeRemote()
        val pending = MemoryPending()
        try {
            coordinator(local, remote, pending, mutableListOf("aaaaaaaa-0000-4000-8000-000000000001"))
                .archiveAndStartNewJob(owner, "新公司", maxStamps = 20)
            fail("Expected the simulated crash")
        } catch (_: JobTransitionException.LocalSaveFailed) {
        }
        // 半套狀態：封存已在本機、章已刪，但 settings/plan 還是舊工作、pending 還在。
        assertEquals(1, local.archives.size)
        assertTrue(local.stamps.isEmpty())
        assertEquals("舊公司", local.settings.companyName)
        assertEquals(12_000.0, local.plan.currentSaved, 0.0)
        assertEquals(1, pending.entries.size)

        // App 重啟 → 復原前滾。
        local.crashAfterRoomPhase = false
        val recovered = coordinator(local, remote, pending, mutableListOf()).recoverPendingTransitions()
        assertEquals(listOf("aaaaaaaa-0000-4000-8000-000000000001"), recovered.map { it.archive.id })
        assertEquals(listOf("t1"), recovered.single().clearedReminderTodoIds)
        assertEquals("新公司", local.settings.companyName)
        assertEquals(20, local.settings.maxStamps)
        assertEquals(2, local.settings.currentJobIndex)
        assertEquals(2, local.settings.currentRunNumber)
        assertEquals(0.0, local.plan.currentSaved, 0.0)
        assertTrue(pending.entries.isEmpty())
        assertEquals("archive keeps every stamp", 3, local.archives.single().payload!!.stamps.size)
        assertEquals("no second upload during recovery", 1, remote.uploads.size)
    }

    @Test
    fun `running recovery twice is a no-op`() = runBlocking {
        val local = fixture().apply { crashAfterRoomPhase = true }
        val pending = MemoryPending()
        val remote = FakeRemote()
        runCatching {
            coordinator(local, remote, pending, mutableListOf("aaaaaaaa-0000-4000-8000-000000000001"))
                .archiveAndStartNewJob(owner, "新公司")
        }
        local.crashAfterRoomPhase = false
        val c = coordinator(local, remote, pending, mutableListOf())
        assertEquals(1, c.recoverPendingTransitions().size)
        val after = Triple(local.settings, local.plan, local.archives.toList())
        val writes = local.dataStoreCommits to local.commits
        assertTrue(c.recoverPendingTransitions().isEmpty())
        assertTrue(coordinator(local, remote, pending, mutableListOf()).recoverPendingTransitions().isEmpty())
        assertEquals(after, Triple(local.settings, local.plan, local.archives.toList()))
        assertEquals(writes, local.dataStoreCommits to local.commits)
        assertEquals(2, local.settings.currentJobIndex)
    }

    @Test
    fun `retrying after the crash completes the same transition instead of archiving an empty job`() = runBlocking {
        val local = fixture().apply { crashAfterRoomPhase = true }
        val pending = MemoryPending()
        val remote = FakeRemote()
        runCatching {
            coordinator(local, remote, pending, mutableListOf("aaaaaaaa-0000-4000-8000-000000000001"))
                .archiveAndStartNewJob(owner, "新公司")
        }
        local.crashAfterRoomPhase = false
        val result = coordinator(local, remote, pending, mutableListOf("cccccccc-0000-4000-8000-000000000003"))
            .archiveAndStartNewJob(owner, "新公司")
        assertEquals("aaaaaaaa-0000-4000-8000-000000000001", result.archive.id)
        assertEquals(1, local.archives.size)
        assertEquals(1, remote.uploads.size)
        assertEquals(2, local.settings.currentJobIndex)
    }

    @Test
    fun `crash after the pending DataStore write before upload changes nothing and recovery leaves it alone`() = runBlocking {
        val local = fixture()
        val before = Triple(local.settings, local.stamps.toList(), local.plan)
        val remote = FakeRemote().apply { failUpload = ProcessDeath() }
        val pending = MemoryPending()
        runCatching {
            coordinator(local, remote, pending, mutableListOf("aaaaaaaa-0000-4000-8000-000000000001"))
                .archiveAndStartNewJob(owner, "新公司")
        }
        assertEquals(1, pending.entries.size)
        assertFalse(pending.entries.values.single().uploaded)

        val c = coordinator(local, remote, pending, mutableListOf())
        assertTrue(c.recoverPendingTransitions().isEmpty())
        assertTrue(c.recoverPendingTransitions().isEmpty())
        assertEquals("nothing local was applied, nothing is rolled", before, Triple(local.settings, local.stamps.toList(), local.plan))
        assertEquals("pending is kept for the next attempt", 1, pending.entries.size)
        assertEquals(0, local.dataStoreCommits)

        // 使用者下次換工作：沿用同一個 id 與封存編號。
        remote.failUpload = null
        val result = coordinator(local, remote, pending, mutableListOf("ffffffff-0000-4000-8000-00000000000f"))
            .archiveAndStartNewJob(owner, "新公司")
        assertEquals("aaaaaaaa-0000-4000-8000-000000000001", result.archive.id)
        assertTrue(pending.entries.isEmpty())
    }

    @Test
    fun `stamps added during upload are archived and stamps added later are never deleted`() = runBlocking {
        val local = fixture()
        val remote = object : JobArchiveRemote {
            override suspend fun upload(id: String, payload: JobArchivePayload): String {
                local.stamps += stamp("00000000-0000-4000-8000-000000000004", "2026-10-04T01:00:00Z", "加班沒錢", 3)
                return id
            }
            override suspend fun fetchAll() = emptyList<JobArchiveRemoteRow>()
        }
        val pending = MemoryPending()
        local.pendingRef = pending
        JobTransitionCoordinator(local, remote, pending, nowMillis = { now }, newUuid = { "aaaaaaaa-0000-4000-8000-000000000001" })
            .archiveAndStartNewJob(owner, "新公司")
        assertEquals(4, local.archives.single().payload!!.stamps.size)
        assertTrue(local.stamps.isEmpty())
    }

    @Test
    fun `upload failure leaves every local record untouched`() = runBlocking {
        val local = fixture()
        val before = Triple(local.settings, local.stamps.toList(), local.plan)
        val remote = FakeRemote().apply { failUpload = JobTransitionException.Network(IOException("offline")) }
        val pending = MemoryPending()
        try {
            coordinator(local, remote, pending, mutableListOf("AAAAAAAA-0000-4000-8000-000000000001"))
                .archiveAndStartNewJob(owner, "新公司")
            fail("Expected network failure")
        } catch (_: JobTransitionException.Network) {
        }
        assertEquals(before, Triple(local.settings, local.stamps.toList(), local.plan))
        assertTrue(local.archives.isEmpty())
        assertEquals(0, local.commits)
        // 待完成紀錄（含完整快照）在上傳前就落地。
        val entry = pending.entries.values.single()
        assertEquals("aaaaaaaa-0000-4000-8000-000000000001", entry.id)
        assertFalse(entry.uploaded)
        assertEquals(3, JobArchiveCoding.decodePayload(entry.payloadJson!!)!!.stampCount)
    }

    @Test
    fun `not signed in never uploads or clears anything`() = runBlocking {
        val local = fixture()
        val remote = FakeRemote()
        try {
            coordinator(local, remote, MemoryPending(), mutableListOf("x")).archiveAndStartNewJob(null, "新公司")
            fail("Expected NotSignedIn")
        } catch (_: JobTransitionException.NotSignedIn) {
        }
        assertTrue(remote.uploads.isEmpty())
        assertEquals(3, local.stamps.size)
    }

    @Test
    fun `local commit failure keeps pending and the retry reuses the same id and archive number`() = runBlocking {
        val local = fixture().apply { failCommit = true }
        val remote = FakeRemote()
        val pending = MemoryPending()
        val ids = mutableListOf("aaaaaaaa-0000-4000-8000-000000000001", "bbbbbbbb-0000-4000-8000-000000000002")
        try {
            coordinator(local, remote, pending, ids).archiveAndStartNewJob(owner, "新公司")
            fail("Expected LocalSaveFailed")
        } catch (_: JobTransitionException.LocalSaveFailed) {
        }
        assertEquals(3, local.stamps.size)
        assertTrue(pending.entries.values.single().uploaded)

        local.failCommit = false
        val result = coordinator(local, remote, pending, ids).archiveAndStartNewJob(owner, "新公司")
        assertEquals(listOf("aaaaaaaa-0000-4000-8000-000000000001", "aaaaaaaa-0000-4000-8000-000000000001"), remote.uploads.map { it.first })
        assertEquals(remote.uploads[0].second.jobIndex, remote.uploads[1].second.jobIndex)
        assertEquals("aaaaaaaa-0000-4000-8000-000000000001", result.archive.id)
        assertTrue(pending.entries.isEmpty())
    }

    @Test
    fun `success archives everything then clears the current job`() = runBlocking {
        val local = fixture()
        val remote = FakeRemote()
        val pending = MemoryPending()
        val result = coordinator(local, remote, pending, mutableListOf("aaaaaaaa-0000-4000-8000-000000000001"))
            .archiveAndStartNewJob(owner, "  新公司  ", maxStamps = 20)

        val archive = local.archives.single()
        assertEquals(owner, archive.ownerUserId)
        assertEquals(1, archive.jobIndex)
        val payload = archive.payload!!
        assertEquals(3, payload.stamps.size)
        assertEquals(12_000.0, payload.savedAmount, 0.0)
        assertEquals(JobReceiptSnapshot.KIND_MASTER, payload.receipt.kind)
        assertEquals(listOf("更新履歷", "存錢"), payload.todos.map { it.title })

        assertTrue(local.stamps.isEmpty())
        assertEquals("新公司", local.settings.companyName)
        assertEquals(20, local.settings.maxStamps)
        assertEquals(1, local.settings.currentRound)
        assertFalse(local.settings.isCardLocked)
        assertNull(local.settings.cardCompletedDate)
        assertEquals("new job = archive number + 1", 2, local.settings.currentJobIndex)
        assertEquals("run number = previous + 1", 2, local.settings.currentRunNumber)
        // 不碰主題、金幣鏡像。
        assertEquals(7, local.settings.cardStyle)
        assertEquals(listOf(1, 2, 3, 7), local.settings.unlockedStyles)
        assertEquals(99, local.settings.coinCount)
        // 存款進度與待辦清除，目標與薪水保留。
        assertEquals(0.0, local.plan.currentSaved, 0.0)
        assertTrue(local.plan.todoItems.isEmpty() && local.plan.completedTodoItems.isEmpty())
        assertEquals(100_000.0, local.plan.targetAmount, 0.0)
        assertEquals(45_000.0, local.plan.monthlySalary, 0.0)

        assertEquals(listOf("t1"), result.clearedReminderTodoIds)
        assertTrue(pending.entries.isEmpty())
    }

    @Test
    fun `archive number avoids indexes already archived by this account`() = runBlocking {
        val local = fixture()
        local.archives += JobArchivePayloadTestData.payload(jobIndex = 4).toEntity("old", owner, "old", 0L)
        val remote = FakeRemote()
        coordinator(local, remote, MemoryPending(), mutableListOf("aaaaaaaa-0000-4000-8000-000000000001"))
            .archiveAndStartNewJob(owner, "新公司")
        assertEquals(5, remote.uploads.single().second.jobIndex)
        assertEquals(6, local.settings.currentJobIndex)
    }

    @Test
    fun `pull merges server rows and hides the unfinished pending transition`() = runBlocking {
        val local = fixture()
        val remote = FakeRemote()
        val pending = MemoryPending()
        pending.entries = mapOf(
            JobArchivePendingKeys.key(owner, 1) to JobArchivePendingEntry(id = "p-1", jobIndex = 1, ownerUserId = owner),
        )
        remote.rows = listOf(
            JobArchiveRemoteRow("p-1", JobArchivePayloadTestData.payload(1)),
            JobArchiveRemoteRow("r-2", JobArchivePayloadTestData.payload(2)),
        )
        val inserted = coordinator(local, remote, pending, mutableListOf())
            .pullFromServer(owner) { owner }
        assertEquals(1, inserted)
        assertEquals(listOf("r-2"), local.archives.map { it.id })
        assertTrue(pending.entries.values.single().uploaded)

        // 帳號在拉取期間變了 → 不寫入。
        remote.rows = listOf(JobArchiveRemoteRow("r-3", JobArchivePayloadTestData.payload(3)))
        assertEquals(0, coordinator(local, remote, pending, mutableListOf()).pullFromServer(owner) { "someone-else" })
        assertEquals(1, local.archives.size)
    }

    @Test
    fun `snapshot follows iOS makeSnapshot rules`() {
        val local = fixture()
        val payload = JobTransitionPolicy.makeSnapshot(local.settings, local.stamps.reversed(), local.plan, now, ZoneOffset.UTC)
        assertEquals(3, payload.cardCount)
        assertEquals("2026-03-02T01:15:00Z", payload.startDate)
        assertEquals(listOf(JobReasonCount("無效會議", 2), JobReasonCount("同事雷", 1)), payload.topReasons)
        assertEquals("2026-10-01T09:00:00Z", payload.receipt.completionDate)
        assertEquals(213, payload.receipt.totalDays)
        assertNotNull(payload.salary)

        val unlockedThird = JobTransitionPolicy.makeSnapshot(
            local.settings.copy(isCardLocked = false), local.stamps, ResignationPlan(), now, ZoneOffset.UTC,
        )
        assertEquals(JobReceiptSnapshot.KIND_RESIGNATION, unlockedThird.receipt.kind)
        assertEquals(BackupDateCodec.format(now), unlockedThird.receipt.completionDate)
        assertNull("no salary configured", unlockedThird.salary)

        val noStamps = JobTransitionPolicy.makeSnapshot(
            AppSettings(roundStartDates = listOf(BackupDateCodec.parse("2026-01-01T00:00:00Z"))), emptyList(), null, now,
        )
        assertEquals("2026-01-01T00:00:00Z", noStamps.startDate)
        assertNull(noStamps.receipt.firstStampDate)
    }

    @Test
    fun `run number and archive number follow section 19 5`() {
        assertEquals(3, JobTransitionPolicy.runNumber(AppSettings(currentRunNumber = 3, currentJobIndex = 9), emptyList()))
        assertEquals(1, JobTransitionPolicy.runNumber(AppSettings(currentJobIndex = 1), listOf("a")))
        assertEquals(1, JobTransitionPolicy.runNumber(AppSettings(currentJobIndex = 4), null))
        assertEquals(3, JobTransitionPolicy.runNumber(AppSettings(currentJobIndex = 4), listOf("a", "b", "b")))
        assertEquals(2, JobTransitionPolicy.runNumber(AppSettings(currentJobIndex = 2), listOf("a", "b", "c")))
        assertEquals(1, JobTransitionPolicy.nextArchiveNumber(1, null))
        assertEquals(3, JobTransitionPolicy.nextArchiveNumber(3, listOf(1)))
        assertEquals(5, JobTransitionPolicy.nextArchiveNumber(2, listOf(4, 1)))
        assertEquals(10, JobTransitionPolicy.resetSettingsForNewJob(AppSettings(), "", 7, null, null).maxStamps)
        assertEquals(1, JobTransitionPolicy.resetSettingsForNewJob(AppSettings(), "", 10, null, -3).currentRunNumber)
    }
}

internal object JobArchivePayloadTestData {
    fun payload(jobIndex: Int) = JobArchivePayload(
        jobIndex = jobIndex,
        companyName = "公司$jobIndex",
        endDate = "2026-0${jobIndex}-01T00:00:00Z",
        receipt = JobReceiptSnapshot(
            kind = JobReceiptSnapshot.KIND_RESIGNATION,
            companyName = "公司$jobIndex",
            completionDate = "2026-0${jobIndex}-01T00:00:00Z",
            totalDays = 1,
            totalStamps = 1,
            savedAmount = 0.0,
            issuedAt = "2026-0${jobIndex}-01T00:00:00Z",
        ),
    )
}
