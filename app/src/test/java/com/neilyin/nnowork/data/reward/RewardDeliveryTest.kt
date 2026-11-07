package com.neilyin.nnowork.data.reward

import com.neilyin.nnowork.data.coin.CoinLedgerEntryEntity
import com.neilyin.nnowork.data.coin.CoinLedgerPolicy
import com.neilyin.nnowork.data.coin.CoinTransactionType
import com.neilyin.nnowork.data.coin.RewardLedgerEntries
import com.neilyin.nnowork.data.treehole.RewardEventResponse
import com.neilyin.nnowork.data.treehole.toRewardEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.nio.file.Files

/** android開發.md §18：delivery_id 持久化、先落地再入帳、以事件 UUID 冪等、ACK 後才移除。 */
class RewardDeliveryTest {
    private val eventA = RewardEvent("6f1c4a52-1111-4f5b-9c1d-000000000001", RewardKind.TREE_HOLE_POST_APPROVED, 5)
    private val eventB = RewardEvent("a93e0000-2222-4f5b-9c1d-000000000002", RewardKind.FEEDBACK_REWARD, 10)
    private val identity = "device:abc"

    /** 模擬 Room `INSERT OR IGNORE`（主鍵 = 流水帳 id）。 */
    private class FakeLedger {
        val entries = linkedMapOf<String, CoinLedgerEntryEntity>()
        fun credit(event: RewardEvent): Boolean {
            val entry = RewardLedgerEntries.entryFor(event, createdAt = 1L)
            if (entry.id in entries) return false
            entries[entry.id] = entry
            return true
        }
        val balance: Int get() = CoinLedgerPolicy.balance(entries.values.toList())
    }

    private class MemoryStore(
        var failSaves: Boolean = false,
        var failSavesWithPending: Boolean = false,
    ) : RewardJournalStore {
        var saved: RewardJournalState? = null
        var saveCount = 0
        override fun load(): RewardJournalState? = saved
        override fun save(state: RewardJournalState) {
            if (failSaves) throw IOException("disk full")
            if (failSavesWithPending && state.pending.isNotEmpty()) throw IOException("disk full")
            saveCount++
            saved = state
        }
    }

    /** 模擬 db/030：未 ACK 的事件以同一 delivery_id 重播，不產生新事件。 */
    private class FakeServer(
        var supports: Boolean? = true,
        var capabilityError: Exception? = null,
    ) : RewardDeliveryRemote {
        val issued = mutableListOf<RewardEvent>()
        val unacked = linkedMapOf<String, RewardEvent>()
        val acked = mutableSetOf<String>()
        var failAck = false
        var capabilityCalls = 0
        var claimCalls = 0
        val claimDeliveryIds = mutableListOf<String>()
        var legacyCalls = 0
        var legacyCounts = mutableMapOf(RewardKind.TREE_HOLE_POST_APPROVED to 0, RewardKind.FEEDBACK_REWARD to 0)
        var claimError: Exception? = null

        fun issue(event: RewardEvent) { issued += event }

        override suspend fun supportsRewardEvents(): Boolean {
            capabilityCalls++
            capabilityError?.let { throw it }
            // 真實 remote 把 404 + PGRST202 轉成 false；null 代表「後端沒有新協定」。
            return supports ?: false
        }

        override suspend fun claimRewardEvents(deliveryId: String): List<RewardEvent> {
            claimCalls++
            claimDeliveryIds += deliveryId
            claimError?.let { throw it }
            if (unacked.isEmpty()) {
                issued.filter { it.id !in acked }.forEach { unacked[it.id] = it }
                issued.clear()
            }
            return unacked.values.toList()
        }

        override suspend fun acknowledgeRewardEvents(ids: List<String>, deliveryId: String): List<String> {
            if (failAck) throw IOException("ack lost")
            val ok = ids.filter { it in unacked }
            ok.forEach { unacked.remove(it); acked += it }
            return ok
        }

        override suspend fun claimLegacyRewardCount(kind: RewardKind): Int {
            legacyCalls++
            return legacyCounts.getValue(kind).also { legacyCounts[kind] = 0 }
        }
    }

    @Test
    fun `replaying the same event credits it only once`() = runBlocking {
        val store = MemoryStore()
        val delivery = RewardDelivery(RewardDeliveryJournal(store))
        val server = FakeServer().apply { issue(eventA) }
        val ledger = FakeLedger()

        // 第一次：入帳成功但 ACK 回應遺失 → 事件留在 journal 與伺服器。
        server.failAck = true
        val first = delivery.synchronize(server, identity) { ledger.credit(it) }
        assertEquals(listOf(eventA), first)
        assertEquals(5, ledger.balance)
        assertEquals(1, store.saved!!.pending.size)

        // 第二次：journal 重播＋伺服器重播同一事件 → 不可再加幣。
        server.failAck = false
        val second = delivery.synchronize(server, identity) { ledger.credit(it) }
        assertEquals(emptyList<RewardEvent>(), second)
        assertEquals(5, ledger.balance)
        assertEquals(1, ledger.entries.size)
        assertTrue(store.saved!!.pending.isEmpty())
        assertEquals(setOf(eventA.id), server.acked)

        // 第三次：沒有新事件。
        delivery.synchronize(server, identity) { ledger.credit(it) }
        assertEquals(5, ledger.balance)
    }

    @Test
    fun `crash after credit before ack is idempotent on restart`() = runBlocking {
        val store = MemoryStore()
        val server = FakeServer().apply { issue(eventA); issue(eventB) }
        val ledger = FakeLedger()
        server.failAck = true
        RewardDelivery(RewardDeliveryJournal(store)).synchronize(server, identity) { ledger.credit(it) }

        // App 重啟：新的 journal 物件從磁碟讀回同一份 state。
        server.failAck = false
        val restarted = RewardDelivery(RewardDeliveryJournal(store))
        restarted.synchronize(server, identity) { ledger.credit(it) }
        assertEquals(15, ledger.balance)
        assertTrue(store.saved!!.pending.isEmpty())
    }

    @Test
    fun `ledger entry id is the server event uuid`() {
        val entry = RewardLedgerEntries.entryFor(eventB, createdAt = 42L)
        assertEquals(eventB.id, entry.id)
        assertEquals(10, entry.amount)
        assertEquals(CoinTransactionType.FEEDBACK_REWARD.rawValue, entry.type)
    }

    @Test
    fun `events are journaled before credit and a journal write failure credits nothing`() = runBlocking {
        val store = MemoryStore()
        val journal = RewardDeliveryJournal(store)
        journal.deliveryId(identity)
        journal.confirmEventProtocol()
        store.failSavesWithPending = true
        val server = FakeServer().apply { issue(eventA) }
        val ledger = FakeLedger()
        var creditCalls = 0
        try {
            RewardDelivery(journal).synchronize(server, identity) { creditCalls++; ledger.credit(it) }
            fail("Expected journal failure")
        } catch (_: IOException) {
        }
        assertEquals("claim happened", 1, server.claimCalls)
        assertEquals("nothing credited before the journal is durable", 0, creditCalls)
        assertEquals(0, ledger.balance)
        assertEquals("server keeps the event for replay", setOf(eventA.id), server.unacked.keys)

        store.failSavesWithPending = false
        RewardDelivery(RewardDeliveryJournal(store)).synchronize(server, identity) { ledger.credit(it) }
        assertEquals(5, ledger.balance)
    }

    @Test
    fun `delivery id is persisted before claiming and reused per identity`() = runBlocking {
        val store = MemoryStore()
        var counter = 0
        val journal = RewardDeliveryJournal(store) { "00000000-0000-4000-8000-00000000000${++counter}" }
        val server = FakeServer()
        val delivery = RewardDelivery(journal)
        delivery.synchronize(server, identity) { true }
        delivery.synchronize(server, identity) { true }
        delivery.synchronize(server, "account:user-1") { true }

        assertEquals(server.claimDeliveryIds[0], server.claimDeliveryIds[1])
        assertNotEquals(server.claimDeliveryIds[0], server.claimDeliveryIds[2])
        assertEquals(server.claimDeliveryIds[0], store.saved!!.scopedDeliveryIds[identity])
        // 重新載入仍沿用同一個 delivery_id。
        assertEquals(server.claimDeliveryIds[0], RewardDeliveryJournal(store).deliveryId(identity))
    }

    @Test
    fun `pending events of another identity are not credited or acknowledged`() = runBlocking {
        val store = MemoryStore()
        val server = FakeServer().apply { issue(eventA) }
        val ledger = FakeLedger()
        server.failAck = true
        RewardDelivery(RewardDeliveryJournal(store)).synchronize(server, "account:a") { ledger.credit(it) }
        val otherLedger = FakeLedger()
        server.failAck = false
        server.unacked.clear() // 另一個身分在伺服器上沒有待 ACK 的事件
        RewardDelivery(RewardDeliveryJournal(store)).synchronize(server, "account:b") { otherLedger.credit(it) }
        assertEquals(0, otherLedger.balance)
        assertEquals(1, store.saved!!.pending.size)
        assertEquals("account:a", store.saved!!.pending.single().ownerIdentity)
    }

    @Test
    fun `missing capability uses legacy counts once and never asks for ack`() = runBlocking {
        val store = MemoryStore()
        val server = FakeServer(supports = null).apply {
            legacyCounts[RewardKind.TREE_HOLE_POST_APPROVED] = 2
            legacyCounts[RewardKind.FEEDBACK_REWARD] = 1
        }
        val ledger = FakeLedger()
        val credited = RewardDelivery(RewardDeliveryJournal(store)).synchronize(server, identity) { ledger.credit(it) }
        assertEquals(20, ledger.balance)
        assertEquals(RewardSyncSummary(approvedPostCoins = 10, feedbackRewardCoins = 10), RewardSyncSummary.of(credited))
        assertEquals(2, RewardSyncSummary.of(credited).approvedPostCount)
        assertEquals(0, server.claimCalls)
        assertTrue(store.saved!!.pending.isEmpty())
        assertFalse(store.saved!!.eventProtocolConfirmed)
    }

    @Test
    fun `transient capability failure neither claims nor falls back to legacy`() = runBlocking {
        val server = FakeServer(capabilityError = IOException("timeout"))
        try {
            RewardDelivery(RewardDeliveryJournal(MemoryStore())).synchronize(server, identity) { true }
            fail("Expected failure")
        } catch (_: IOException) {
        }
        assertEquals(0, server.claimCalls)
        assertEquals(0, server.legacyCalls)
    }

    @Test
    fun `confirmed protocol is sticky and claim failures never use legacy RPCs`() = runBlocking {
        val store = MemoryStore()
        val server = FakeServer()
        RewardDelivery(RewardDeliveryJournal(store)).synchronize(server, identity) { true }
        assertTrue(store.saved!!.eventProtocolConfirmed)

        server.supports = null
        server.claimError = IOException("response lost")
        val delivery = RewardDelivery(RewardDeliveryJournal(store))
        runCatching { delivery.synchronize(server, identity) { true } }
        assertEquals("capability is not re-probed after confirmation", 1, server.capabilityCalls)
        assertEquals(0, server.legacyCalls)
    }

    @Test
    fun `ack only removes ids the server confirmed for this delivery`() = runBlocking {
        val store = MemoryStore()
        val journal = RewardDeliveryJournal(store)
        val stranger = "ffffffff-ffff-4fff-8fff-ffffffffffff"
        val remote = object : RewardDeliveryRemote {
            override suspend fun supportsRewardEvents() = true
            override suspend fun claimRewardEvents(deliveryId: String) = listOf(eventA, eventB)
            override suspend fun acknowledgeRewardEvents(ids: List<String>, deliveryId: String) =
                listOf(eventA.id.uppercase(), stranger)
            override suspend fun claimLegacyRewardCount(kind: RewardKind) = 0
        }
        RewardDelivery(journal).synchronize(remote, identity) { true }
        assertEquals(listOf(eventB.id), store.saved!!.pending.map { it.event.id })
    }

    @Test
    fun `same event id with different content is a protocol error`() {
        val journal = RewardDeliveryJournal(MemoryStore())
        journal.append(listOf(eventA), identity)
        journal.append(listOf(eventA), identity)
        assertEquals(1, journal.state.pending.size)
        try {
            journal.append(listOf(eventA.copy(amount = 50)), identity)
            fail("Expected protocol error")
        } catch (_: RewardProtocolException) {
        }
    }

    @Test
    fun `server event parsing normalizes uuid and rejects unknown kinds`() {
        val parsed = RewardEventResponse("6F1C4A52-1111-4F5B-9C1D-000000000001", "treeHolePostApproved", 5).toRewardEvent()
        assertEquals(eventA.id, parsed.id)
        assertEquals(RewardKind.TREE_HOLE_POST_APPROVED, parsed.kind)
        listOf(
            RewardEventResponse(eventA.id, "mysteryBox", 5),
            RewardEventResponse(eventA.id, "feedbackReward", 0),
            RewardEventResponse("not-a-uuid", "feedbackReward", 10),
        ).forEach { bad ->
            try {
                bad.toRewardEvent()
                fail("Expected protocol error for $bad")
            } catch (_: RewardProtocolException) {
            }
        }
    }

    @Test
    fun `identity scopes by account when signed in`() {
        assertEquals("account:abc-123", RewardIdentity.of(" ABC-123 ", "hash"))
        assertEquals("device:hash", RewardIdentity.of(null, "hash"))
        assertEquals("device:hash", RewardIdentity.of("  ", "hash"))
    }

    @Test
    fun `file journal survives a new instance and is written atomically`() {
        val dir = Files.createTempDirectory("reward-journal").toFile()
        try {
            val file = dir.resolve("reward_delivery/pending-v1.json")
            val journal = RewardDeliveryJournal(FileRewardJournalStore(file))
            val deliveryId = journal.deliveryId(identity)
            journal.append(listOf(eventA), identity)

            val reloaded = RewardDeliveryJournal(FileRewardJournalStore(file))
            assertEquals(deliveryId, reloaded.deliveryId(identity))
            assertEquals(listOf(eventA.id), reloaded.state.pending.map { it.event.id })
            assertFalse(dir.resolve("reward_delivery/pending-v1.json.tmp").exists())

            file.writeText("{not json")
            try {
                RewardDeliveryJournal(FileRewardJournalStore(file))
                fail("A corrupt journal must stop claims instead of looking empty")
            } catch (_: Exception) {
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
