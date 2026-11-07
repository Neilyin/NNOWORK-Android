package com.neilyin.nnowork.data.reward

import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.coin.CoinTransactionType
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.UUID

/**
 * 金幣獎勵 delivery + ACK（android開發.md §18，對照 iOS `RewardDelivery.swift`）。
 *
 * 伺服器發出有 UUID 的事件 → client 先落地 journal → 以事件 UUID 冪等入帳 → ACK。
 * 沒 ACK 的事件下次 claim 會原封不動重播；入帳以事件 id 當流水帳 id，重播不會加兩次幣。
 */
enum class RewardKind(val rawValue: String, val coinsPerUnit: Int, val transactionType: CoinTransactionType) {
    TREE_HOLE_POST_APPROVED("treeHolePostApproved", 5, CoinTransactionType.TREE_HOLE_POST_APPROVED),
    FEEDBACK_REWARD("feedbackReward", 10, CoinTransactionType.FEEDBACK_REWARD),
    ;

    companion object {
        fun fromRaw(raw: String): RewardKind? = entries.firstOrNull { it.rawValue == raw }
    }
}

data class RewardEvent(
    /** 正規化為小寫 UUID；同時是本機金幣流水帳的 id。 */
    val id: String,
    val kind: RewardKind,
    val amount: Int,
    /** 舊協定回傳的是則數，沒有伺服器事件可 ACK。 */
    val requiresAcknowledgement: Boolean = true,
)

/** 協定回應格式不符（未知 kind、amount ≤ 0、同 id 不同內容…）。 */
class RewardProtocolException(message: String) : IOException(message)

/** 後端明確沒有新協定（404 + PGRST202）。 */
class RewardRpcMissingException(rpc: String) : IOException("Reward RPC $rpc is not deployed")

object RewardEventIds {
    /** 伺服器 UUID（大小寫皆可）→ 小寫標準格式；不是 UUID 就是協定錯誤。 */
    fun normalize(raw: String): String {
        val parsed = runCatching { UUID.fromString(raw.trim()) }.getOrNull()
            ?: throw RewardProtocolException("Reward event id is not a UUID")
        val canonical = parsed.toString()
        if (!canonical.equals(raw.trim(), ignoreCase = true)) {
            throw RewardProtocolException("Reward event id is not a canonical UUID")
        }
        return canonical.lowercase(Locale.ROOT)
    }
}

/** 已登入用 `account:<auth user id>`、未登入用 `device:<device_hash>`（§18.2）。 */
object RewardIdentity {
    fun of(accountId: String?, deviceHash: String): String {
        val account = accountId?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)
        return if (account != null) "account:$account" else "device:$deviceHash"
    }
}

interface RewardDeliveryRemote {
    /** version == 1 → true；404 + PGRST202 → false；其他錯誤拋出（這次先不領）。 */
    suspend fun supportsRewardEvents(): Boolean
    suspend fun claimRewardEvents(deliveryId: String): List<RewardEvent>
    /** 回傳伺服器真的確認的 id（acknowledged_ids）。 */
    suspend fun acknowledgeRewardEvents(ids: List<String>, deliveryId: String): List<String>
    /** 舊協定：check_new_approvals(_v2) / check_new_feedback_rewards(_v2) 回傳則數。 */
    suspend fun claimLegacyRewardCount(kind: RewardKind): Int
}

@JsonClass(generateAdapter = false)
data class RewardJournalEvent(
    val id: String = "",
    val kind: String = "",
    val amount: Int = 0,
)

@JsonClass(generateAdapter = false)
data class RewardJournalPending(
    val event: RewardJournalEvent = RewardJournalEvent(),
    val requiresAcknowledgement: Boolean = true,
    val ownerIdentity: String = "",
)

@JsonClass(generateAdapter = false)
data class RewardJournalState(
    val version: Int = 1,
    /** 每個身分一個 delivery_id，第一次需要時產生並持久化，之後永遠沿用。 */
    val scopedDeliveryIds: Map<String, String> = emptyMap(),
    /** 一旦確認新協定就持久化，之後任何錯誤都不准退回舊 RPC。 */
    val eventProtocolConfirmed: Boolean = false,
    val pending: List<RewardJournalPending> = emptyList(),
)

/** 落地儲存；讀取失敗（損毀）必須拋錯，讓這次同步停止而不是當成空 journal。 */
interface RewardJournalStore {
    fun load(): RewardJournalState?
    fun save(state: RewardJournalState)
}

/** filesDir 下的 JSON（不放 cache）；以 temp + atomic rename 寫入。 */
class FileRewardJournalStore(private val file: File) : RewardJournalStore {
    private val adapter = JsonProvider.adapter<RewardJournalState>()

    override fun load(): RewardJournalState? {
        if (!file.exists()) return null
        val text = file.readText(StandardCharsets.UTF_8)
        return adapter.fromJson(text) ?: throw IOException("Reward journal is empty")
    }

    override fun save(state: RewardJournalState) {
        file.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("Cannot create ${it.name}") }
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(adapter.toJson(state), StandardCharsets.UTF_8)
        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

class RewardDeliveryJournal(
    private val store: RewardJournalStore,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
) {
    var state: RewardJournalState = store.load() ?: RewardJournalState()
        private set

    fun persist() = store.save(state)

    fun deliveryId(identity: String): String {
        state.scopedDeliveryIds[identity]?.let { return it }
        val created = newUuid().lowercase(Locale.ROOT)
        val updated = state.copy(scopedDeliveryIds = state.scopedDeliveryIds + (identity to created))
        store.save(updated) // delivery_id 必須在呼叫伺服器前就落地。
        state = updated
        return created
    }

    fun confirmEventProtocol() {
        val updated = state.copy(eventProtocolConfirmed = true)
        store.save(updated) // 第一次 mutating RPC 前就要提交這個不可逆的選擇。
        state = updated
    }

    /** 同 id 已存在則保留舊的（內容必須一致）；寫入失敗就拋出，呼叫端不得入帳。 */
    fun append(events: List<RewardEvent>, ownerIdentity: String) {
        var pending = state.pending
        for (event in events) {
            if (event.amount <= 0) throw RewardProtocolException("Reward amount must be positive")
            val existing = pending.firstOrNull { it.event.id == event.id }
            if (existing != null) {
                if (existing.event.kind != event.kind.rawValue || existing.event.amount != event.amount) {
                    throw RewardProtocolException("Reward event ${event.id} changed content")
                }
            } else {
                pending = pending + RewardJournalPending(
                    event = RewardJournalEvent(event.id, event.kind.rawValue, event.amount),
                    requiresAcknowledgement = event.requiresAcknowledgement,
                    ownerIdentity = ownerIdentity,
                )
            }
        }
        val updated = state.copy(pending = pending)
        store.save(updated)
        state = updated
    }

    fun remove(ids: Set<String>) {
        if (ids.isEmpty()) return
        val updated = state.copy(pending = state.pending.filterNot { it.event.id in ids })
        store.save(updated)
        state = updated
    }

    fun pendingFor(identity: String): List<Pair<RewardEvent, Boolean>> = state.pending
        .filter { it.ownerIdentity == identity }
        .mapNotNull { item ->
            val kind = RewardKind.fromRaw(item.event.kind) ?: return@mapNotNull null
            RewardEvent(item.event.id, kind, item.event.amount, item.requiresAcknowledgement) to
                item.requiresAcknowledgement
        }
}

/** 同一時間只跑一個 synchronize；已確認新協定後絕不退回舊 RPC。 */
class RewardDelivery(
    private val journal: RewardDeliveryJournal,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
) {
    private val running = Mutex()

    /**
     * @param credit 以事件 id 冪等入帳；回傳 true 代表這次真的新增了流水帳。
     * @return 這次新入帳的事件（重播但已入過帳的不會出現）。
     */
    suspend fun synchronize(
        remote: RewardDeliveryRemote,
        identity: String,
        credit: suspend (RewardEvent) -> Boolean,
    ): List<RewardEvent> {
        if (!running.tryLock()) return emptyList()
        try {
            journal.persist() // journal 必須可寫，才可以聯絡伺服器。
            val deliveryId = journal.deliveryId(identity)
            val credited = deliverPending(remote, identity, deliveryId, credit).toMutableList()

            if (!journal.state.eventProtocolConfirmed && remote.supportsRewardEvents()) {
                journal.confirmEventProtocol()
            }
            if (journal.state.eventProtocolConfirmed) {
                val events = remote.claimRewardEvents(deliveryId)
                journal.append(events, identity) // 先落地；失敗就拋出、不入帳，下次 claim 重播。
                credited += deliverPending(remote, identity, deliveryId, credit)
            } else {
                // 只有明確「函式不存在」才會走到這裡；timeout/401/500 不會觸發第二次 claim。
                for (kind in listOf(RewardKind.TREE_HOLE_POST_APPROVED, RewardKind.FEEDBACK_REWARD)) {
                    val count = remote.claimLegacyRewardCount(kind)
                    if (count < 0) throw RewardProtocolException("Legacy reward count must not be negative")
                    val amount = try {
                        Math.multiplyExact(count, kind.coinsPerUnit)
                    } catch (_: ArithmeticException) {
                        throw RewardProtocolException("Legacy reward count overflow")
                    }
                    if (amount > 0) {
                        journal.append(
                            listOf(
                                RewardEvent(
                                    id = newUuid().lowercase(Locale.ROOT),
                                    kind = kind,
                                    amount = amount,
                                    requiresAcknowledgement = false,
                                ),
                            ),
                            identity,
                        )
                        credited += deliverPending(remote, identity, deliveryId, credit)
                    }
                }
            }
            return credited
        } finally {
            running.unlock()
        }
    }

    private suspend fun deliverPending(
        remote: RewardDeliveryRemote,
        identity: String,
        deliveryId: String,
        credit: suspend (RewardEvent) -> Boolean,
    ): List<RewardEvent> {
        val pending = journal.pendingFor(identity)
        val credited = mutableListOf<RewardEvent>()
        for ((event, _) in pending) {
            if (credit(event)) credited += event
        }
        val ackIds = pending.filter { it.second }.map { it.first.id }
        val completed = pending.filterNot { it.second }.map { it.first.id }.toMutableSet()
        if (ackIds.isNotEmpty()) {
            // ACK 失敗不擋流程：事件留在 journal，下次重播（入帳冪等）。
            val acknowledged = try {
                remote.acknowledgeRewardEvents(ackIds, deliveryId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (acknowledged != null) {
                val normalized = acknowledged.mapNotNull { runCatching { RewardEventIds.normalize(it) }.getOrNull() }
                // 只移除這次真的送出、且伺服器確認屬於目前身分的 id。
                completed += normalized.filter { it in ackIds }
            }
        }
        journal.remove(completed)
        return credited
    }
}

/** 一次同步的結果，依 kind 彙總（UI 顯示「獲得 N 枚離職金幣」）。 */
data class RewardSyncSummary(
    val approvedPostCoins: Int = 0,
    val feedbackRewardCoins: Int = 0,
) {
    val approvedPostCount: Int get() = approvedPostCoins / RewardKind.TREE_HOLE_POST_APPROVED.coinsPerUnit
    val feedbackRewardCount: Int get() = feedbackRewardCoins / RewardKind.FEEDBACK_REWARD.coinsPerUnit

    companion object {
        fun of(events: List<RewardEvent>) = RewardSyncSummary(
            approvedPostCoins = events.filter { it.kind == RewardKind.TREE_HOLE_POST_APPROVED }.sumOf { it.amount },
            feedbackRewardCoins = events.filter { it.kind == RewardKind.FEEDBACK_REWARD }.sumOf { it.amount },
        )
    }
}
