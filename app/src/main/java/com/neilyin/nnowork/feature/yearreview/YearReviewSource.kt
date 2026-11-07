package com.neilyin.nnowork.feature.yearreview

import android.content.Context
import android.content.res.Configuration
import com.neilyin.nnowork.R
import com.neilyin.nnowork.data.backup.BackupDateCodec
import com.neilyin.nnowork.data.jobarchive.JobArchiveEntity
import com.neilyin.nnowork.data.jobarchive.JobArchiveVisibility
import com.neilyin.nnowork.data.jobarchive.JobReceiptSnapshot
import com.neilyin.nnowork.data.treehole.MyTreeHoleComment
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.di.ServiceLocator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale

/** android.icu 的字典斷詞（API 24+，中日文有字典），對應 iOS NLTokenizer(unit: .word)。 */
object YearReviewIcuTokenizer : YearReviewTokenizer {
    override fun tokens(text: String, language: String): List<String> {
        val iterator = android.icu.text.BreakIterator.getWordInstance(YearReviewReasonAnalyzer.localeFor(language))
        iterator.setText(text)
        val result = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != android.icu.text.BreakIterator.DONE) {
            // ruleStatus 為 NONE 的是空白與標點
            if (iterator.ruleStatus != android.icu.text.BreakIterator.WORD_NONE) result += text.substring(start, end)
            start = end
            end = iterator.next()
        }
        return result
    }
}

/** 內建六個原因（iOS BuiltInStampReason）：繁中 key 與各語系文字資源。 */
object YearReviewBuiltInReasons {
    val entries: List<Pair<String, Int>> = listOf(
        "老闆廢話太多" to R.string.yr_builtin_boss,
        "無效會議" to R.string.yr_builtin_meeting,
        "需求一直改" to R.string.yr_builtin_requirements,
        "同事雷" to R.string.yr_builtin_coworker,
        "加班沒錢" to R.string.yr_builtin_overtime,
        "心情不好" to R.string.yr_builtin_mood,
    )

    private val bundledLocales = listOf(
        Locale.TRADITIONAL_CHINESE, Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH, Locale.JAPANESE,
    )

    /** 任何語系存的內建原因文字 → 繁中 key。 */
    fun canonicalMap(context: Context): Map<String, String> {
        val map = HashMap<String, String>()
        entries.forEach { (key, _) -> map[key] = key }
        for (locale in bundledLocales) {
            val localized = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })
            entries.forEach { (key, res) -> map.putIfAbsent(localized.getString(res).trim(), key) }
        }
        return map
    }

    /** 繁中 key → 目前語系文字。 */
    fun displayMap(context: Context): Map<String, String> = entries.associate { (key, res) -> key to context.getString(res) }

    fun display(context: Context, raw: String): String {
        val trimmed = raw.trim()
        val key = canonicalMap(context)[trimmed] ?: return raw
        return displayMap(context)[key] ?: raw
    }
}

/** App 目前語系 → 斷詞語言。 */
fun yearReviewAppLanguage(locale: Locale): String = when {
    locale.language == "ja" -> "ja"
    locale.language == "zh" && (locale.script == "Hans" || locale.country == "CN" || locale.country == "SG") -> "zh-Hans"
    locale.language == "zh" -> "zh-Hant"
    else -> "en"
}

/** 從本機資料收集年度回顧的輸入（§22.2）。 */
object YearReviewSource {

    suspend fun inputs(context: Context, year: Int, nowMillis: Long = System.currentTimeMillis()): YearReviewInputs =
        withContext(Dispatchers.IO) {
            val stamps = ServiceLocator.stampDao.getAll()
            val archives = visibleArchives()
            val settings = ServiceLocator.settingsRepository.current()
            val plan = ServiceLocator.planRepository.current()
            val coins = ServiceLocator.coinLedgerDao.getAll()
            val purchases = ServiceLocator.database.shopPurchaseDao().getAll()
            val profile = runCatching { ServiceLocator.treeHoleRepository.profile.first() }.getOrNull()

            val all = stamps.map { YearReviewStamp(it.date, it.reason, it.isEasterEgg) } +
                archives.flatMap { (_, payload) ->
                    payload.stamps.mapNotNull { stamp ->
                        parseIso(stamp.date)?.let { YearReviewStamp(it, stamp.reason, stamp.isEasterEgg) }
                    }
                }
            val archiveSnapshots = archives.map { (entity, payload) ->
                val receipt = payload.receipt
                val parsedCompletion = parseIso(receipt.completionDate)
                val parsedIssued = parseIso(receipt.issuedAt)
                // iOS 備援收據：日期解析不了時 completion＝issued＝endDate（判成沒集滿，保守），封存仍算進換工作。
                val completion = if (parsedCompletion != null && parsedIssued != null) parsedCompletion else entity.endDate
                val issued = if (parsedCompletion != null && parsedIssued != null) parsedIssued else entity.endDate
                val isMaster = receipt.kind == JobReceiptSnapshot.KIND_MASTER
                YearReviewArchiveSnapshot(
                    endDate = entity.endDate,
                    isMaster = isMaster,
                    completionDate = completion,
                    lastCardCompleted = YearReviewArchiveSnapshot.lastCardCompleted(isMaster, completion, issued),
                    stamps = payload.stamps.mapNotNull { stamp ->
                        parseIso(stamp.date)?.let { YearReviewArchivedStamp(it, stamp.roundNumber) }
                    },
                )
            }
            val locale = context.resources.configuration.locales[0]
            YearReviewInputs(
                year = year,
                stamps = all,
                card = YearReviewCardSnapshot(
                    currentRound = settings.currentRound,
                    isCardLocked = settings.isCardLocked,
                    cardCompletedDate = settings.cardCompletedDate,
                    roundCompletedDates = settings.roundCompletedDates,
                ),
                archives = archiveSnapshots,
                savedAmount = plan.currentSaved,
                targetAmount = plan.targetAmount,
                coinEntries = coins.map { YearReviewCoinEntry(it.amount, it.type, it.createdAt) },
                purchases = purchases.map { YearReviewPurchase(it.productId, it.purchasedAt) },
                nickname = profile?.nickname,
                hasTreeHoleProfile = profile != null,
                nowMillis = nowMillis,
                zone = ZoneId.systemDefault(),
                builtInReasons = YearReviewBuiltInReasons.canonicalMap(context),
                builtInDisplay = YearReviewBuiltInReasons.displayMap(context),
                fallbackLanguage = yearReviewAppLanguage(locale),
            )
        }

    /** 樹洞暱稱（開場空殼用）。 */
    suspend fun nickname(): String? =
        runCatching { ServiceLocator.treeHoleRepository.profile.first()?.nickname }.getOrNull()

    /** 邀請卡：該年至少有一個章（目前卡片＋目前帳號封存工作，同 §22.2）。 */
    suspend fun hasStamps(year: Int, zone: ZoneId = ZoneId.systemDefault()): Boolean = withContext(Dispatchers.IO) {
        fun inYear(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).year == year
        if (ServiceLocator.stampDao.getAll().any { inYear(it.date) }) return@withContext true
        visibleArchives().any { (_, payload) -> payload.stamps.any { stamp -> parseIso(stamp.date)?.let(::inYear) == true } }
    }

    /** 只取目前帳號的封存（未登入時沒有）。 */
    private suspend fun visibleArchives(): List<Pair<JobArchiveEntity, com.neilyin.nnowork.data.jobarchive.JobArchivePayload>> {
        val owner = ServiceLocator.authRepository.currentUserId
        return JobArchiveVisibility.visible(ServiceLocator.database.jobArchiveDao().getAll(), owner)
            .mapNotNull { entity -> entity.payload?.let { entity to it } }
    }

    private fun parseIso(value: String): Long? = runCatching { BackupDateCodec.parse(value) }.getOrNull()
}

/** 「取暖樹洞」頁資料（iOS YearReviewTreeHoleLoader）：失敗、逾時都回 null（該頁跳過）。 */
object YearReviewTreeHoleLoader {
    const val TIMEOUT_MS = 10_000L

    suspend fun load(year: Int, zone: ZoneId = ZoneId.systemDefault()): YearReviewTreeHoleStats? =
        withTimeoutOrNull(TIMEOUT_MS) {
            try {
                val repository = ServiceLocator.treeHoleRepository
                coroutineScope {
                    val posts = async(Dispatchers.IO) { repository.fetchMyPostsForReview() }
                    val comments = async(Dispatchers.IO) { repository.fetchMyCommentsForReview() }
                    // 解答成績失敗只回 0，不算整體失敗
                    val answer = async(Dispatchers.IO) { runCatching { repository.fetchMyAnswerStatsForReview() }.getOrNull() }
                    val stats = answer.await()
                    YearReviewTreeHoleStatsBuilder.make(
                        posts = posts.await(),
                        comments = comments.await(),
                        exp = stats?.exp ?: 0,
                        helpful = stats?.helpfulCount ?: 0,
                        year = year,
                        zone = zone,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
}

/** 純函式：只算今年、審核通過的貼文與留言。 */
object YearReviewTreeHoleStatsBuilder {
    fun make(
        posts: List<TreeHolePost>,
        comments: List<MyTreeHoleComment>,
        exp: Int,
        helpful: Int,
        year: Int,
        zone: ZoneId,
    ): YearReviewTreeHoleStats {
        fun inYear(raw: String) = parseDate(raw)?.atZone(zone)?.year == year
        var stampPosts = 0
        var questions = 0
        var hearts = 0
        var cheers = 0
        var cries = 0
        posts.filter { it.status == "approved" && inYear(it.createdAt) }.forEach { post ->
            if (post.isQuestion) questions++ else stampPosts++
            hearts += post.likesCount
            cheers += post.cheerCount
            cries += post.cryCount
        }
        return YearReviewTreeHoleStats(
            stampPosts = stampPosts,
            questions = questions,
            hearts = hearts,
            cheers = cheers,
            cries = cries,
            comments = comments.count { it.status == "approved" && inYear(it.createdAt) },
            exp = exp.coerceAtLeast(0),
            helpful = helpful.coerceAtLeast(0),
        )
    }

    /** created_at 可能有小數秒也可能沒有。 */
    fun parseDate(raw: String): Instant? =
        runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
            ?: runCatching { Instant.parse(raw) }.getOrNull()
}
