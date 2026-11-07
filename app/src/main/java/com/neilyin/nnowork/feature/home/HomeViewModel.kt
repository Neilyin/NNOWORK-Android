package com.neilyin.nnowork.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.analytics.Analytics
import com.neilyin.nnowork.data.coin.CoinLedgerRepository
import com.neilyin.nnowork.data.treehole.TreeHoleProfile
import com.neilyin.nnowork.data.treehole.TreeHoleRepository
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val stamps: List<StampRecordEntity> = emptyList(),
    val treeHoleProfile: TreeHoleProfile? = null,
    /** 離職計畫目前存到的錢（忍耐大師階段總結用）。 */
    val savedAmount: Double = 0.0,
) {
    val theme: CardTheme get() = CardTheme.fromId(settings.cardStyle)

    /** 當前回合已蓋章數（對應 iOS currentRoundStamps）。 */
    val currentRoundStamps: Int
        get() = stamps
            .count { it.roundNumber == settings.currentRound }
            .coerceIn(0, settings.maxStamps)

    val isCurrentCardComplete: Boolean
        get() = currentRoundStamps >= settings.maxStamps

    /** 第 3 張起集滿都是忍耐大師（iOS isMasterLevel：currentRound >= 3，與彩蛋觸發條件一致）。 */
    val isMasterLevel: Boolean
        get() = settings.currentRound >= 3 && isCurrentCardComplete

    val hasStampedToday: Boolean
        get() {
            val now = System.currentTimeMillis()
            return stamps.any { DateUtils.isSameDay(it.date, now) }
        }

    val canStamp: Boolean
        get() = !isMasterLevel && !hasStampedToday && !isCurrentCardComplete
}

/**
 * A stable snapshot of the stamp that was just created.
 *
 * Sharing can happen after the user starts the next card, so the tree-hole payload must not read
 * the mutable current round/count at send time.
 */
data class StampCreationResult(
    val justCompleted: Boolean,
    val shareContent: String,
    val roundNumber: Int,
    val stampCount: Int,
    /**
     * 蓋章當下的周目（§20.3，貼文 job_index 送這個）。之後換工作周目會 +1，所以不能在送出時才讀。
     * null = 當下讀取失敗，交給 repository 在送出時讀（與舊行為相同）。
     */
    val runNumber: Int? = null,
)

/** 蓋章當下擷取周目（與 round/stamp 同時），失敗回 null、不擋蓋章。 */
internal suspend fun captureRunNumber(provider: suspend () -> Int): Int? = try {
    provider()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

/** 分享到樹洞：一律送快照裡的 round／stamp／周目，不讀目前值。 */
internal suspend fun submitStampShare(
    stamp: StampCreationResult,
    submit: suspend (content: String, roundNumber: Int, stampCount: Int, runNumber: Int?) -> Unit,
) = submit(stamp.shareContent.trim(), stamp.roundNumber, stamp.stampCount, stamp.runNumber)

class HomeViewModel(
    private val stampDao: StampDao = ServiceLocator.stampDao,
    private val settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
    private val coinLedgerRepository: CoinLedgerRepository = ServiceLocator.coinLedgerRepository,
    private val treeHoleRepository: TreeHoleRepository = ServiceLocator.treeHoleRepository,
    private val runNumberProvider: suspend () -> Int = { ServiceLocator.jobArchiveRepository.currentRunNumber() },
    private val planRepository: PlanRepository = ServiceLocator.planRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            settingsRepository.settings,
            stampDao.observeAll(),
            treeHoleRepository.profile,
            planRepository.plan,
        ) { settings, stamps, profile, plan ->
            HomeUiState(settings = settings, stamps = stamps, treeHoleProfile = profile, savedAmount = plan.currentSaved)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    /** 蓋章，並回傳可安全延後分享的當下資料快照。 */
    fun addStamp(reason: String, addToCommon: Boolean, onResult: (StampCreationResult) -> Unit) {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val runNumber = captureRunNumber(runNumberProvider)
            val record = StampRecordEntity(
                date = System.currentTimeMillis(),
                reason = trimmed,
                isEasterEgg = false,
                roundNumber = settings.currentRound,
            )
            coinLedgerRepository.insertStampAndRewardDaily(record)

            settingsRepository.update { s ->
                var updated = s
                // 加入常用原因
                if (addToCommon && !s.commonReasons.contains(trimmed)) {
                    updated = updated.copy(commonReasons = updated.commonReasons + trimmed)
                }
                // 第一張章日期
                if (updated.firstStampDate == null) {
                    updated = updated.copy(firstStampDate = record.date)
                }
                updated
            }
            // 判斷是否剛好集滿
            val roundCount = stampDao.countForRound(record.roundNumber)
            val justCompleted = roundCount == settings.maxStamps
            Analytics.log("stamp_added", "total_stamps" to stampDao.getAll().size.toString())
            if (justCompleted) {
                settingsRepository.update {
                    it.copy(
                        isCardLocked = true,
                        cardCompletedDate = System.currentTimeMillis(),
                        roundCompletedDates = it.roundCompletedDates + System.currentTimeMillis(),
                    )
                }
                Analytics.log("stamp_round_completed", "round" to record.roundNumber.toString())
            }
            onResult(
                StampCreationResult(
                    justCompleted = justCompleted,
                    shareContent = trimmed,
                    roundNumber = record.roundNumber,
                    stampCount = roundCount,
                    runNumber = runNumber,
                ),
            )
        }
    }

    fun shareToTreeHole(stamp: StampCreationResult, onResult: (String) -> Unit) {
        val trimmed = stamp.shareContent.trim()
        if (trimmed.length < 5) {
            onResult("至少寫 5 個字才能分享到樹洞。")
            return
        }
        viewModelScope.launch {
            runCatching {
                submitStampShare(stamp) { content, round, count, run ->
                    treeHoleRepository.submitPost(content, round, count, run)
                }
            }.onSuccess {
                Analytics.log("treehole_post_created")
                onResult("已分享到樹洞，審核通過後會公開並發放金幣。")
            }.onFailure { error ->
                onResult(error.message?.takeIf { it.isNotBlank() } ?: "分享失敗，稍後再試。")
            }
        }
    }

    /** 彩蛋蓋章（不計每日限制、不額外給金幣，但同樣要填理由）。 */
    fun addEasterEggStamp(reason: String, addToCommon: Boolean, onResult: (StampCreationResult) -> Unit) {
        val trimmed = reason.trim().ifEmpty { "隱藏彩蛋" }
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val runNumber = captureRunNumber(runNumberProvider)
            val record = StampRecordEntity(
                date = System.currentTimeMillis(),
                reason = trimmed,
                isEasterEgg = true,
                roundNumber = settings.currentRound,
            )
            stampDao.insert(record)
            if (addToCommon && !settings.commonReasons.contains(trimmed)) {
                settingsRepository.update { it.copy(commonReasons = it.commonReasons + trimmed) }
            }
            Analytics.log(
                "stamp_added",
                "total_stamps" to stampDao.getAll().size.toString(),
                "easter_egg" to "true",
            )
            val roundCount = stampDao.countForRound(settings.currentRound)
            onResult(
                StampCreationResult(
                    justCompleted = roundCount == settings.maxStamps,
                    shareContent = trimmed,
                    roundNumber = record.roundNumber,
                    stampCount = roundCount,
                    runNumber = runNumber,
                ),
            )
        }
    }

    /** 開新卡片（回合 +1）。 */
    fun startNewCard(newMaxStamps: Int? = null) {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(
                    currentRound = it.currentRound + 1,
                    maxStamps = newMaxStamps ?: it.maxStamps,
                    isCardLocked = false,
                    cardCompletedDate = null,
                    roundStartDates = it.roundStartDates + System.currentTimeMillis(),
                )
            }
        }
    }
}
