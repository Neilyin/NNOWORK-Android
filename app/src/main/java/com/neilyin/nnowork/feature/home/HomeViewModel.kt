package com.neilyin.nnowork.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val stamps: List<StampRecordEntity> = emptyList(),
) {
    val theme: CardTheme get() = CardTheme.fromId(settings.cardStyle)

    /** 當前回合已蓋章數（對應 iOS currentRoundStamps）。 */
    val currentRoundStamps: Int
        get() {
            val total = stamps.size
            val previous = settings.maxStamps * (settings.currentRound - 1)
            return (total - previous).coerceIn(0, settings.maxStamps)
        }

    val isCurrentCardComplete: Boolean
        get() = currentRoundStamps >= settings.maxStamps

    val isMasterLevel: Boolean
        get() = settings.currentRound == 3 && isCurrentCardComplete

    val hasStampedToday: Boolean
        get() {
            val now = System.currentTimeMillis()
            return stamps.any { DateUtils.isSameDay(it.date, now) }
        }

    val canStamp: Boolean
        get() = !isMasterLevel && !hasStampedToday && !isCurrentCardComplete
}

class HomeViewModel(
    private val stampDao: StampDao = ServiceLocator.stampDao,
    private val settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(settingsRepository.settings, stampDao.observeAll()) { settings, stamps ->
            HomeUiState(settings = settings, stamps = stamps)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    /**
     * 蓋章。回傳是否剛好集滿（供 UI 觸發完成流程）。
     */
    fun addStamp(reason: String, addToCommon: Boolean, onResult: (justCompleted: Boolean) -> Unit) {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val record = StampRecordEntity(
                date = System.currentTimeMillis(),
                reason = trimmed,
                isEasterEgg = false,
                roundNumber = settings.currentRound,
            )
            stampDao.insert(record)

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
                // 每日金幣（一天上限一顆）
                val today = DateUtils.startOfDay(System.currentTimeMillis())
                val last = updated.lastCoinEarnedDate
                if (last == null || !DateUtils.isSameDay(last, today)) {
                    updated = updated.copy(
                        coinCount = updated.coinCount + 1,
                        lastCoinEarnedDate = today,
                    )
                }
                updated
            }

            // 判斷是否剛好集滿
            val settingsAfter = settingsRepository.current()
            val roundCount = stampDao.countForRound(settingsAfter.currentRound)
            val justCompleted = roundCount == settingsAfter.maxStamps
            if (justCompleted) {
                settingsRepository.update {
                    it.copy(
                        isCardLocked = true,
                        cardCompletedDate = System.currentTimeMillis(),
                        roundCompletedDates = it.roundCompletedDates + System.currentTimeMillis(),
                    )
                }
            }
            onResult(justCompleted)
        }
    }

    /** 彩蛋蓋章（不計每日限制）。 */
    fun addEasterEggStamp(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            val record = StampRecordEntity(
                date = System.currentTimeMillis(),
                reason = "隱藏彩蛋",
                isEasterEgg = true,
                roundNumber = settings.currentRound,
            )
            stampDao.insert(record)
            val roundCount = stampDao.countForRound(settings.currentRound)
            onResult(roundCount == settings.maxStamps)
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
