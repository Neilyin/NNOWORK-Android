package com.neilyin.nnowork.feature.yearreview

import android.content.Context
import android.content.SharedPreferences
import com.neilyin.nnowork.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 開放期間與年份（§22.1）：一律西曆（java.time ISO）＋裝置時區。
 *
 * | 今天        | 回顧哪一年 |
 * | 12/10～12/31 | 當年       |
 * | 1/1～1/31    | 前一年     |
 * | 其他         | 不開放     |
 */
object YearReviewWindow {
    const val RELEASE_START_DAY = 10

    fun today(nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

    fun reviewYear(date: LocalDate): Int? = when (date.monthValue) {
        12 -> if (date.dayOfMonth >= RELEASE_START_DAY) date.year else null
        1 -> date.year - 1
        else -> null
    }

    /** 設定頁入口：開放期間，或「提前開放」（看今年）。 */
    fun availableYear(date: LocalDate, earlyAccess: Boolean): Int? =
        reviewYear(date) ?: if (earlyAccess) date.year else null

    /** 邀請卡：開放期間，或「模擬 12/10 後」（看今年）。 */
    fun inviteWindowYear(date: LocalDate, simulateWindow: Boolean): Int? =
        reviewYear(date) ?: if (simulateWindow) date.year else null

    /** 「立即顯示邀請卡」：期間內用期間年份，否則看今年。 */
    fun forcedYear(date: LocalDate, simulateWindow: Boolean): Int =
        inviteWindowYear(date, simulateWindow) ?: date.year
}

/** 一次性邀請卡的規則（§22.12），純函式。 */
object YearReviewInvitePolicy {
    const val SHOWN_KEY_PREFIX = "year_review_invite_shown_"
    /** 等待：先等 2 秒，之後每 0.8 秒檢查，連續兩次都空著才顯示。 */
    const val INITIAL_DELAY_MS = 2_000L
    const val FORCED_INITIAL_DELAY_MS = 600L
    const val POLL_INTERVAL_MS = 800L
    const val REQUIRED_IDLE_CHECKS = 2

    fun shownKey(year: Int): String = SHOWN_KEY_PREFIX + year

    /**
     * 要不要邀請：在期間內、這一年還沒看過、這一年有章。
     * 沒章時不邀請也不記看過（之後蓋了章或雲端還原，下次回前景再判斷）。
     */
    fun shouldInvite(windowYear: Int?, alreadyShown: Boolean, hasStampsInYear: Boolean): Boolean =
        windowYear != null && !alreadyShown && hasStampsInYear

    /** 依序的「畫面是否空著」檢查結果 → 第幾次檢查後可以顯示（0 起算）；不到就回 null。 */
    fun firstShowableCheck(idleChecks: List<Boolean>): Int? {
        var streak = 0
        idleChecks.forEachIndexed { index, idle ->
            streak = if (idle) streak + 1 else 0
            if (streak >= REQUIRED_IDLE_CHECKS) return index
        }
        return null
    }
}

/** 測試開關的值（key 與 iOS UserDefaults 同名，§22.13）。 */
data class YearReviewTestFlags(
    val earlyAccess: Boolean = false,
    val simulateInviteWindow: Boolean = false,
    val loadDelay: Boolean = false,
    /** DEBUG 專用：用示範資料播放（不讀、不寫本機資料），看得到每一頁。 */
    val demoData: Boolean = false,
) {
    val anyOn: Boolean get() = earlyAccess || simulateInviteWindow || loadDelay || demoData

    companion object {
        const val EARLY_ACCESS_KEY = "year_review_test_early_access"
        const val SIMULATE_INVITE_WINDOW_KEY = "year_review_test_simulate_invite_window"
        const val LOAD_DELAY_KEY = "debug_year_review_load_delay"
        const val DEMO_DATA_KEY = "debug_year_review_demo_data"
    }
}

/**
 * 本機（不同步、不進備份）的年度回顧偏好：測試開關與邀請卡「看過」紀錄。
 *
 * 測試工具目前關閉（[TEST_TOOLS_ENABLED]）：不論 DEBUG／Release，開關一律視為關，只照 §22.1 在 12/10～1/31 開放。
 */
object YearReviewPrefs {
    /** 開發測試用：改成 true 才會讀測試開關、在設定頁顯示 DEBUG 測試卡。上架前保持 false。 */
    const val TEST_TOOLS_ENABLED = false

    private const val FILE = "year_review_prefs"
    private lateinit var prefs: SharedPreferences
    private val _flags = MutableStateFlow(YearReviewTestFlags())
    val flags: StateFlow<YearReviewTestFlags> = _flags.asStateFlow()

    private val _showInviteNow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** DEBUG「立即顯示邀請卡」。 */
    val showInviteNowRequests: SharedFlow<Unit> = _showInviteNow.asSharedFlow()

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        refresh()
    }

    private fun refresh() {
        if (!TEST_TOOLS_ENABLED) {
            _flags.value = YearReviewTestFlags()
            return
        }
        _flags.value = YearReviewTestFlags(
            earlyAccess = prefs.getBoolean(YearReviewTestFlags.EARLY_ACCESS_KEY, BuildConfig.DEBUG),
            simulateInviteWindow = prefs.getBoolean(YearReviewTestFlags.SIMULATE_INVITE_WINDOW_KEY, false),
            loadDelay = prefs.getBoolean(YearReviewTestFlags.LOAD_DELAY_KEY, false),
            demoData = BuildConfig.DEBUG && prefs.getBoolean(YearReviewTestFlags.DEMO_DATA_KEY, false),
        )
    }

    fun setFlag(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        refresh()
    }

    /** 全部關閉（DEBUG 的「提前開放」預設開，所以明確寫 false）。 */
    fun turnAllOff() {
        prefs.edit()
            .putBoolean(YearReviewTestFlags.EARLY_ACCESS_KEY, false)
            .putBoolean(YearReviewTestFlags.SIMULATE_INVITE_WINDOW_KEY, false)
            .putBoolean(YearReviewTestFlags.LOAD_DELAY_KEY, false)
            .putBoolean(YearReviewTestFlags.DEMO_DATA_KEY, false)
            .apply()
        refresh()
    }

    fun hasShownInvite(year: Int): Boolean = prefs.getBoolean(YearReviewInvitePolicy.shownKey(year), false)

    fun markInviteShown(year: Int) {
        prefs.edit().putBoolean(YearReviewInvitePolicy.shownKey(year), true).apply()
    }

    fun resetInviteShown() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(YearReviewInvitePolicy.SHOWN_KEY_PREFIX) }.forEach(editor::remove)
        editor.apply()
    }

    fun requestShowInviteNow() {
        _showInviteNow.tryEmit(Unit)
    }
}
