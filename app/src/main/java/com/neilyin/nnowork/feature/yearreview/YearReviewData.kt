package com.neilyin.nnowork.feature.yearreview

import com.neilyin.nnowork.ui.theme.CardTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/*
 * 年度回顧（android開發.md §22，對照 iOS YearReviewData.swift）的統計。
 *
 * 純函式、不碰 Android：輸入是值型別快照（[YearReviewInputs]），[YearReviewData.build] 可在
 * Dispatchers.Default 執行，各頁的統計都是快照上的計算屬性，方便單元測試。
 * 日期一律 java.time（ISO 西曆）＋裝置時區；不用 java.util.Calendar（和暦／佛曆會拿到錯的年份）。
 */

/** 一個章（目前卡片與封存工作的章都轉成這個）。 */
data class YearReviewStamp(
    val epochMillis: Long,
    val reason: String,
    val isEasterEgg: Boolean = false,
)

/** 目前工作的集點卡進度（AppSettings 的子集）。 */
data class YearReviewCardSnapshot(
    val currentRound: Int = 1,
    val isCardLocked: Boolean = false,
    val cardCompletedDate: Long? = null,
    val roundCompletedDates: List<Long> = emptyList(),
)

data class YearReviewArchivedStamp(val epochMillis: Long, val roundNumber: Int)

/** 一份封存工作裡算「集滿／忍耐大師／換工作」需要的欄位。 */
data class YearReviewArchiveSnapshot(
    val endDate: Long,
    val isMaster: Boolean,
    /** 收據上的完成日（最後一張卡集滿的日子；沒集滿則是封存當下）。 */
    val completionDate: Long,
    /** 封存時最後一張卡是否已集滿（見 [lastCardCompleted]）。 */
    val lastCardCompleted: Boolean,
    val stamps: List<YearReviewArchivedStamp>,
) {
    companion object {
        /**
         * iOS：`receipt.kind == "master"` 或 `|completionDate − issuedAt| ≥ 1 秒`。
         * 要跟同一份收據的 issuedAt 比，不要跟 endDate 比（§22.3）。
         */
        fun lastCardCompleted(isMaster: Boolean, completionDate: Long, issuedAt: Long): Boolean =
            isMaster || kotlin.math.abs(completionDate - issuedAt) >= 1_000L
    }
}

data class YearReviewCoinEntry(val amount: Int, val type: String, val createdAt: Long)

data class YearReviewPurchase(val productId: String, val purchasedAt: Long)

enum class YearReviewTreeHoleStatus { NOT_JOINED, LOADING, LOADED, FAILED }

/** 樹洞的年度數字。發文與留言只算今年、審核通過的；EXP 與有用解答次數是目前累積值。 */
data class YearReviewTreeHoleStats(
    val stampPosts: Int = 0,
    val questions: Int = 0,
    val hearts: Int = 0,
    val cheers: Int = 0,
    val cries: Int = 0,
    val comments: Int = 0,
    val exp: Int = 0,
    val helpful: Int = 0,
) {
    val reactions: Int get() = hearts + cheers + cries
    val hasContent: Boolean get() = stampPosts + questions + comments + exp + helpful > 0
}

/** [YearReviewData.build] 需要的全部輸入（值型別）。 */
data class YearReviewInputs(
    val year: Int,
    val stamps: List<YearReviewStamp>,
    val card: YearReviewCardSnapshot = YearReviewCardSnapshot(),
    val archives: List<YearReviewArchiveSnapshot> = emptyList(),
    val savedAmount: Double = 0.0,
    val targetAmount: Double = 0.0,
    val coinEntries: List<YearReviewCoinEntry> = emptyList(),
    val purchases: List<YearReviewPurchase> = emptyList(),
    val nickname: String? = null,
    val hasTreeHoleProfile: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis(),
    val zone: ZoneId = ZoneId.systemDefault(),
    /** 原因（去頭尾空白）→ 內建原因 key；只收內建原因。 */
    val builtInReasons: Map<String, String> = emptyMap(),
    /** 內建原因 key → 目前語系文字（關鍵字模式斷詞用）。 */
    val builtInDisplay: Map<String, String> = emptyMap(),
    /** 偵測不到語言時用的語言（zh-Hant／zh-Hans／ja／en）。 */
    val fallbackLanguage: String = "zh-Hant",
)

/** 播放的頁（順序固定，條件不成立的頁整頁拿掉）。 */
enum class YearReviewPage(val durationSeconds: Float) {
    OPENING(7f),
    ORIGIN(7f),
    STAMP_COUNT(6f),
    REASONS(7f),
    TIME_OF_DAY(7f),
    WEEKDAY(7f),
    STREAK(6f),
    CARDS(6f),
    SAVINGS(6f),
    TREE_HOLE(7f),
    COINS(6f),
    TITLE(7f),
    SUMMARY(8f);

    /** 開場、年度稱號、總結卡用像素星空；其他頁跟著主題。 */
    val isNight: Boolean get() = this == OPENING || this == TITLE || this == SUMMARY
}

/** 蓋章時段（依小時分五段）；順序即同數時的優先順序。 */
enum class YearReviewTimeSlot {
    EARLY_MORNING,
    WORK_MORNING,
    AFTERNOON,
    EVENING,
    LATE_NIGHT;

    companion object {
        fun forHour(hour: Int): YearReviewTimeSlot = when (hour) {
            in 5..8 -> EARLY_MORNING
            in 9..11 -> WORK_MORNING
            in 12..17 -> AFTERNOON
            in 18..22 -> EVENING
            else -> LATE_NIGHT
        }
    }
}

enum class YearReviewTitleRank {
    APPRENTICE,
    WARRIOR,
    KNIGHT,
    OVERLORD;

    companion object {
        fun forStamps(count: Int): YearReviewTitleRank = when {
            count < 20 -> APPRENTICE
            count < 60 -> WARRIOR
            count < 150 -> KNIGHT
            else -> OVERLORD
        }
    }
}

data class YearReviewStreak(val length: Int, val start: LocalDate, val end: LocalDate)

data class YearReviewTitleParts(
    val weekday: DayOfWeek,
    val slot: YearReviewTimeSlot,
    val rank: YearReviewTitleRank,
)

class YearReviewData private constructor(
    val year: Int,
    /** 樹洞暱稱（去頭尾空白）；沒有或空白為 null。 */
    val nickname: String?,
    /** 該年所有章，依時間排序。 */
    val stamps: List<YearReviewStamp>,
    /** 統計截止：min(現在, 該年 12/31 23:59:59)。 */
    val periodEnd: Long,
    val zone: ZoneId,
    /** 全期（不限今年）去重後最早的章。 */
    val firstStamp: YearReviewStamp?,
    val reasonInsight: YearReviewReasonInsight?,
    val card: YearReviewCardSnapshot,
    val archives: List<YearReviewArchiveSnapshot>,
    val savedAmount: Double,
    val targetAmount: Double,
    /** 該年的金幣流水帳與商店購買（購買依時間排序）。 */
    val coinEntries: List<YearReviewCoinEntry>,
    val purchases: List<YearReviewPurchase>,
    val treeHoleStatus: YearReviewTreeHoleStatus,
    val treeHole: YearReviewTreeHoleStats?,
    /** 資料還沒算好前給開場頁用的空殼。 */
    val isPlaceholder: Boolean,
) {

    fun withTreeHole(stats: YearReviewTreeHoleStats?): YearReviewData = YearReviewData(
        year, nickname, stamps, periodEnd, zone, firstStamp, reasonInsight, card, archives,
        savedAmount, targetAmount, coinEntries, purchases,
        treeHoleStatus = if (stats == null) YearReviewTreeHoleStatus.FAILED else YearReviewTreeHoleStatus.LOADED,
        treeHole = stats,
        isPlaceholder = isPlaceholder,
    )

    private fun dateOf(millis: Long): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(zone)
    private fun localDate(millis: Long): LocalDate = dateOf(millis).toLocalDate()
    private fun isInYear(millis: Long): Boolean = dateOf(millis).year == year

    // MARK: 頁面

    val isLite: Boolean get() = totalStamps < LITE_THRESHOLD

    val pages: List<YearReviewPage> by lazy {
        buildList {
            add(YearReviewPage.OPENING)
            if (adventureDays != null) add(YearReviewPage.ORIGIN)
            add(YearReviewPage.STAMP_COUNT)
            if (reasonInsight != null) add(YearReviewPage.REASONS)
            if (!isLite) {
                if (peakHour != null) add(YearReviewPage.TIME_OF_DAY)
                if (peakWeekday != null) add(YearReviewPage.WEEKDAY)
                if ((longestStreak?.length ?: 0) >= 2) add(YearReviewPage.STREAK)
            }
            if (hasCardsContent) add(YearReviewPage.CARDS)
            if (!isLite && targetAmount > 0) add(YearReviewPage.SAVINGS)
            if (showsTreeHolePage) add(YearReviewPage.TREE_HOLE)
            if (!isLite && hasCoinsContent) add(YearReviewPage.COINS)
            add(YearReviewPage.TITLE)
            add(YearReviewPage.SUMMARY)
        }
    }

    val showsTreeHolePage: Boolean
        get() = when (treeHoleStatus) {
            YearReviewTreeHoleStatus.LOADING -> true
            YearReviewTreeHoleStatus.LOADED -> treeHole?.hasContent == true
            YearReviewTreeHoleStatus.NOT_JOINED, YearReviewTreeHoleStatus.FAILED -> false
        }

    // MARK: 蓋章總數

    val totalStamps: Int get() = stamps.size

    /** 從該年第一個章那天到 max(periodEnd, 最後一個章) 那天（含頭尾）÷ 章數；少於 2 個章為 null。 */
    val averageDaysPerStamp: Double?
        get() {
            if (stamps.size < 2) return null
            val start = localDate(stamps.first().epochMillis)
            val end = localDate(maxOf(periodEnd, stamps.last().epochMillis))
            val days = ChronoUnit.DAYS.between(start, end) + 1
            return maxOf(days, 1L).toDouble() / stamps.size
        }

    // MARK: 起點

    /** 全期第一個章那天到 periodEnd 那天（含頭尾）；沒有章或第一個章在 periodEnd 之後為 null。 */
    val adventureDays: Int?
        get() {
            val first = firstStamp ?: return null
            val start = localDate(first.epochMillis)
            val end = localDate(periodEnd)
            if (start.isAfter(end)) return null
            return (ChronoUnit.DAYS.between(start, end) + 1).toInt()
        }

    /** periodEnd 是 12/31 時說「到今年底」。 */
    val endsAtYearEnd: Boolean
        get() = localDate(periodEnd).let { it.monthValue == 12 && it.dayOfMonth == 31 }

    // MARK: 時段

    val hourHistogram: IntArray by lazy {
        IntArray(24).also { bins -> stamps.forEach { bins[dateOf(it.epochMillis).hour]++ } }
    }

    /** 章最多的小時（同數取較早）；沒有章為 null。 */
    val peakHour: Int?
        get() {
            val max = hourHistogram.max()
            return if (max > 0) hourHistogram.indexOfFirst { it == max } else null
        }

    /** 某時段裡章最多的小時：比較順序 23, 0, 1, …, 22，同數取先出現的。 */
    fun peakHour(slot: YearReviewTimeSlot): Int? {
        var best: Int? = null
        for (hour in HOUR_ORDER) {
            if (YearReviewTimeSlot.forHour(hour) != slot || hourHistogram[hour] <= 0) continue
            if (best == null || hourHistogram[hour] > hourHistogram[best]) best = hour
        }
        return best
    }

    /** 各時段章數加總最多者；同數取順序較前的。 */
    val dominantTimeSlot: YearReviewTimeSlot?
        get() {
            if (stamps.isEmpty()) return null
            val counts = IntArray(YearReviewTimeSlot.entries.size)
            hourHistogram.forEachIndexed { hour, count -> counts[YearReviewTimeSlot.forHour(hour).ordinal] += count }
            var best: YearReviewTimeSlot? = null
            for (slot in YearReviewTimeSlot.entries) {
                if (counts[slot.ordinal] > (best?.let { counts[it.ordinal] } ?: 0)) best = slot
            }
            return best
        }

    // MARK: 星期與月份

    val weekdayCounts: Map<DayOfWeek, Int> by lazy {
        stamps.groupingBy { dateOf(it.epochMillis).dayOfWeek }.eachCount()
    }

    /** 章最多的星期；同數依週一→週日先出現的。 */
    val peakWeekday: DayOfWeek?
        get() {
            val max = weekdayCounts.values.maxOrNull() ?: return null
            if (max <= 0) return null
            return DayOfWeek.entries.first { weekdayCounts[it] == max }
        }

    val monthHistogram: IntArray by lazy {
        IntArray(12).also { bins -> stamps.forEach { bins[dateOf(it.epochMillis).monthValue - 1]++ } }
    }

    /** 1～12 月章數最多者（同數取較早）。 */
    val peakMonth: Int?
        get() {
            val max = monthHistogram.max()
            return if (max > 0) monthHistogram.indexOfFirst { it == max } + 1 else null
        }

    // MARK: 連續

    /** 今年有章的日期去重排序，相鄰差 1 天就接續；嚴格大於才換（同長度取較早的那段）。 */
    val longestStreak: YearReviewStreak? by lazy {
        val days = stamps.map { localDate(it.epochMillis) }.distinct().sorted()
        val first = days.firstOrNull() ?: return@lazy null
        var best = YearReviewStreak(1, first, first)
        var runStart = first
        var runLength = 1
        for (index in 1 until days.size) {
            val day = days[index]
            if (days[index - 1].plusDays(1) == day) {
                runLength += 1
            } else {
                runStart = day
                runLength = 1
            }
            if (runLength > best.length) best = YearReviewStreak(runLength, runStart, day)
        }
        best
    }

    // MARK: 卡片與工作

    val completedCardCount: Int by lazy {
        var count = card.roundCompletedDates.count(::isInYear)
        val completed = card.cardCompletedDate
        if (card.isCardLocked && completed != null && isInYear(completed) &&
            card.roundCompletedDates.size < card.currentRound
        ) {
            count += 1
        }
        for (archive in archives) {
            val rounds = archive.stamps.groupBy { it.roundNumber }
            val roundNumbers = rounds.keys.sorted()
            for (round in roundNumbers.dropLast(1)) {
                val last = rounds[round]?.maxOfOrNull { it.epochMillis }
                if (last != null && isInYear(last)) count += 1
            }
            if (archive.lastCardCompleted && isInYear(archive.completionDate)) count += 1
        }
        count
    }

    val becameMaster: Boolean by lazy {
        if (card.roundCompletedDates.sorted().drop(2).any(::isInYear)) return@lazy true
        val completed = card.cardCompletedDate
        if (card.currentRound >= 3 && card.isCardLocked && completed != null && isInYear(completed)) {
            return@lazy true
        }
        archives.any { archive ->
            if (archive.isMaster && isInYear(archive.completionDate)) return@any true
            val rounds = archive.stamps.groupBy { it.roundNumber }
            rounds.keys.sorted().dropLast(1).filter { it >= 3 }.any { round ->
                rounds[round]?.maxOfOrNull { it.epochMillis }?.let(::isInYear) == true
            }
        }
    }

    val jobsChanged: Int get() = archives.count { isInYear(it.endDate) }

    val hasCardsContent: Boolean get() = completedCardCount > 0 || becameMaster || jobsChanged > 0

    // MARK: 自由預備金

    /** 0～1；沒設目標為 null。 */
    val savingsProgress: Double?
        get() = if (targetAmount > 0) (savedAmount / targetAmount).coerceIn(0.0, 1.0) else null

    /** floor(progress × 100)。 */
    val savingsPercent: Int get() = kotlin.math.floor((savingsProgress ?: 0.0) * 100).toInt()

    // MARK: 金幣

    val coinsEarned: Int
        get() = coinEntries.filter { it.amount > 0 && it.type !in EXCLUDED_COIN_TYPES }.sumOf { it.amount }

    /** 今年買的主題（依購買順序、不重複）。 */
    val purchasedThemes: List<CardTheme>
        get() = purchases
            .mapNotNull { purchase -> CardTheme.entries.firstOrNull { it.shopProductId == purchase.productId } }
            .distinct()

    val hasCoinsContent: Boolean get() = coinsEarned > 0 || purchasedThemes.isNotEmpty()

    // MARK: 年度稱號

    /** 精簡版為 null（畫面改用專屬稱號）。 */
    val titleParts: YearReviewTitleParts?
        get() {
            if (isLite) return null
            val weekday = peakWeekday ?: return null
            val slot = dominantTimeSlot ?: return null
            return YearReviewTitleParts(weekday, slot, YearReviewTitleRank.forStamps(totalStamps))
        }

    companion object {
        const val LITE_THRESHOLD = 5
        private val HOUR_ORDER = listOf(23) + (0..22)
        /** 舊餘額轉換、開發期測試發幣不算「今年賺到」。 */
        private val EXCLUDED_COIN_TYPES = setOf("migration", "debugGrant", "debugSeed")

        /** 資料還沒算好前給開場頁用的空殼（開場只用到年份與暱稱）。 */
        fun placeholder(year: Int, nickname: String?, zone: ZoneId = ZoneId.systemDefault()): YearReviewData =
            build(YearReviewInputs(year = year, stamps = emptyList(), nickname = nickname, zone = zone), placeholder = true)

        /** 純函式：只吃值型別，可在背景執行緒計算。 */
        fun build(inputs: YearReviewInputs, placeholder: Boolean = false): YearReviewData {
            val zone = inputs.zone
            val year = inputs.year
            fun inYear(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).year == year

            // 同一秒＋同原因只算一次（封存的 ISO8601 只到秒、本機有小數秒，比到整數秒）。
            val seen = HashSet<String>()
            val allStamps = inputs.stamps
                .filter { seen.add("${Math.floorDiv(it.epochMillis, 1000L)}|${it.reason}") }
                .sortedBy { it.epochMillis }
            val inYearStamps = allStamps.filter { inYear(it.epochMillis) }

            val endOfYear = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli() - 1_000L
            val nickname = inputs.nickname?.trim()?.takeIf(String::isNotEmpty)

            return YearReviewData(
                year = year,
                nickname = nickname,
                stamps = inYearStamps,
                periodEnd = minOf(inputs.nowMillis, endOfYear),
                zone = zone,
                firstStamp = allStamps.firstOrNull(),
                reasonInsight = if (placeholder) null else YearReviewReasonAnalyzer.insight(
                    reasons = inYearStamps.map { it.reason },
                    builtInReasons = inputs.builtInReasons,
                    builtInDisplay = inputs.builtInDisplay,
                    fallbackLanguage = inputs.fallbackLanguage,
                ),
                card = inputs.card,
                archives = inputs.archives,
                savedAmount = inputs.savedAmount,
                targetAmount = inputs.targetAmount,
                coinEntries = inputs.coinEntries.filter { inYear(it.createdAt) },
                purchases = inputs.purchases.filter { inYear(it.purchasedAt) }.sortedBy { it.purchasedAt },
                treeHoleStatus = if (inputs.hasTreeHoleProfile) YearReviewTreeHoleStatus.LOADING else YearReviewTreeHoleStatus.NOT_JOINED,
                treeHole = null,
                isPlaceholder = placeholder,
            )
        }
    }
}
