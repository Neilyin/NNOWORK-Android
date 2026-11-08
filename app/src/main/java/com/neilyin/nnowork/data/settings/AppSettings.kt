package com.neilyin.nnowork.data.settings

import com.squareup.moshi.JsonClass

/**
 * 對應 iOS SwiftData 的 AppSettings。整體以 JSON 存於 DataStore。
 * 日期欄位以 epoch millis 表示（null 代表未設定）。
 */
@JsonClass(generateAdapter = false)
data class AppSettings(
    val companyName: String = "",
    val cardStyle: Int = 1,
    val maxStamps: Int = 10,
    val currentRound: Int = 1,
    val isOnboardingFinished: Boolean = false,
    val widgetRefreshInterval: Int = 60,

    // 集點卡追蹤
    val firstStampDate: Long? = null,
    val isCardLocked: Boolean = false,
    val cardCompletedDate: Long? = null,
    val roundStartDates: List<Long> = emptyList(),
    val roundCompletedDates: List<Long> = emptyList(),

    // 金幣系統
    val coinCount: Int = 0,
    /** 與 iOS v2+ 備份格式對齊；金幣流水帳實作後可用於一次性遷移。 */
    val hasMigratedToLedger: Boolean = false,
    val lastCoinEarnedDate: Long? = null,
    val lastTreeHoleShareDate: Long? = null,
    val unlockedStyles: List<Int> = listOf(1, 2, 3),

    // 版本通知
    val lastSeenNoticeVersion: String = "",
    val authGuideShownForV340: Boolean = false,

    // 常用蓋章原因
    val commonReasons: List<String> = DEFAULT_REASONS,

    // 換工作（android開發.md §19.5，與 iOS AppSettings 同名同語意）
    /** 封存編號（第幾份工作），只增不減；換工作或全部重來都 +1。 */
    val currentJobIndex: Int = 1,
    /**
     * 周目（顯示用），貼文 job_index 送這個。換工作 +1、全部重來設回 1。
     * 0 = 舊資料尚未記錄，一律透過 JobTransitionPolicy.runNumber 讀取（會推估）。
     */
    val currentRunNumber: Int = 0,
) {
    companion object {
        val DEFAULT_REASONS = listOf(
            "老闆廢話太多",
            "無效會議",
            "需求一直改",
            "同事雷",
            "加班沒錢",
            "心情不好",
        )
    }
}
