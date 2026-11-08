package com.neilyin.nnowork.feature.yearreview

import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale

/** 原因頁的一列。 */
data class YearReviewReasonCount(
    /** 比對用 key：內建原因是繁中原文；自訂原因與關鍵字是正規化後的字串。 */
    val key: String,
    /** 顯示用原文（自訂原因與關鍵字取第一次出現的寫法）。 */
    val text: String,
    val count: Int,
    val isBuiltIn: Boolean,
) {
    val builtInKey: String? get() = if (isBuiltIn) key else null
}

/** 有重複出現的原因就排原因；原因都只出現一次就改看斷詞後最常提到的詞。 */
sealed class YearReviewReasonInsight {
    abstract val items: List<YearReviewReasonCount>

    data class Reasons(override val items: List<YearReviewReasonCount>) : YearReviewReasonInsight()
    data class Keywords(override val items: List<YearReviewReasonCount>) : YearReviewReasonInsight()

    val isKeywords: Boolean get() = this is Keywords
}

/**
 * 斷詞（iOS NLTokenizer）。Android 端用 android.icu 的字典斷詞（見 YearReviewIcuTokenizer）；
 * JVM 單元測試用 [JavaWordTokenizer]。
 */
fun interface YearReviewTokenizer {
    /** [language]：zh-Hant／zh-Hans／ja／en。 */
    fun tokens(text: String, language: String): List<String>
}

/** java.text.BreakIterator 版本（不含中日文字典，測試與後備用）。 */
object JavaWordTokenizer : YearReviewTokenizer {
    override fun tokens(text: String, language: String): List<String> {
        val iterator = BreakIterator.getWordInstance(YearReviewReasonAnalyzer.localeFor(language))
        iterator.setText(text)
        val result = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val token = text.substring(start, end)
            if (token.any { Character.isLetterOrDigit(it) }) result += token
            start = end
            end = iterator.next()
        }
        return result
    }
}

/** 原因排行與關鍵字斷詞（純函式）。 */
object YearReviewReasonAnalyzer {
    const val MINIMUM_REPEAT = 2

    /** Android 端在 App 啟動時換成 ICU 斷詞；預設 java.text（單元測試）。 */
    @Volatile
    var tokenizer: YearReviewTokenizer = JavaWordTokenizer

    fun insight(
        reasons: List<String>,
        builtInReasons: Map<String, String>,
        builtInDisplay: Map<String, String>,
        fallbackLanguage: String,
        tokenizer: YearReviewTokenizer = this.tokenizer,
    ): YearReviewReasonInsight? {
        val ranked = rankReasons(reasons, builtInReasons).filter { it.count >= MINIMUM_REPEAT }
        if (ranked.isNotEmpty()) return YearReviewReasonInsight.Reasons(ranked.take(3))

        val texts = reasons.map { raw ->
            val trimmed = raw.trim()
            val key = builtInReasons[trimmed] ?: return@map trimmed
            builtInDisplay[key] ?: key
        }
        val keywords = rankKeywords(texts, fallbackLanguage, tokenizer).filter { it.count >= MINIMUM_REPEAT }
        return if (keywords.isEmpty()) null else YearReviewReasonInsight.Keywords(keywords.take(5))
    }

    /** 原因次數排行；空白原因不算；同次數依第一次出現先後。 */
    fun rankReasons(reasons: List<String>, builtInReasons: Map<String, String>): List<YearReviewReasonCount> {
        val counts = LinkedHashMap<String, Int>()
        val display = HashMap<String, String>()
        val builtInKeys = HashSet<String>()
        for (raw in reasons) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue
            val canonical = builtInReasons[trimmed]
            val key = if (canonical != null) {
                builtInKeys += canonical
                canonical
            } else {
                val normalized = normalizeForMatching(trimmed)
                if (normalized.isEmpty()) trimmed else "custom:$normalized"
            }
            counts[key] = (counts[key] ?: 0) + 1
            display.putIfAbsent(key, trimmed)
        }
        // LinkedHashMap 保留首次出現順序；sortedByDescending 是穩定排序。
        return counts.entries.sortedByDescending { it.value }.map { (key, count) ->
            val isBuiltIn = key in builtInKeys
            YearReviewReasonCount(key, if (isBuiltIn) key else display[key] ?: key, count, isBuiltIn)
        }
    }

    /** 每句各自偵測語言再斷詞；同一句裡同一個詞只算一次。 */
    fun rankKeywords(
        texts: List<String>,
        fallbackLanguage: String,
        tokenizer: YearReviewTokenizer = this.tokenizer,
    ): List<YearReviewReasonCount> {
        val counts = LinkedHashMap<String, Int>()
        val display = HashMap<String, String>()
        for (text in texts) {
            if (text.isEmpty()) continue
            val language = when (val detected = detectLanguage(text)) {
                null -> fallbackLanguage
                "zh" -> if (fallbackLanguage.startsWith("zh")) fallbackLanguage else "zh-Hant"
                else -> detected
            }
            val seenInText = HashSet<String>()
            for (token in tokenizer.tokens(text, language)) {
                val key = normalizeForMatching(token)
                if (!isMeaningfulKeyword(key) || !seenInText.add(key)) continue
                counts[key] = (counts[key] ?: 0) + 1
                display.putIfAbsent(key, token)
            }
        }
        return counts.entries.sortedByDescending { it.value }.map { (key, count) ->
            YearReviewReasonCount(key, display[key] ?: key, count, isBuiltIn = false)
        }
    }

    /**
     * 依字元集粗判語言（iOS NLLanguageRecognizer，限繁中、簡中、日、英）：有假名＝日文、
     * 有漢字＝中文（繁簡交給呼叫端的 App 語系，斷詞字典兩者共用）、有拉丁字母＝英文；都沒有為 null。
     */
    fun detectLanguage(text: String): String? {
        var han = false
        var latin = false
        for (cp in text.codePoints().toArray()) {
            when (Character.UnicodeScript.of(cp)) {
                Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> return "ja"
                Character.UnicodeScript.HAN -> han = true
                Character.UnicodeScript.LATIN -> latin = true
                else -> Unit
            }
        }
        return when {
            han -> "zh"
            latin -> "en"
            else -> null
        }
    }

    fun localeFor(language: String): Locale = when (language) {
        "zh-Hans" -> Locale.SIMPLIFIED_CHINESE
        "zh-Hant", "zh" -> Locale.TRADITIONAL_CHINESE
        "ja" -> Locale.JAPANESE
        else -> Locale.ENGLISH
    }

    /** 比對用：全形轉半形、不分大小寫，只留文字與數字（去掉空白、標點、符號）。 */
    fun normalizeForMatching(text: String): String {
        val builder = StringBuilder()
        text.codePoints().forEach { raw ->
            // 只對「半形及全形字元」區塊做相容分解（= iOS widthInsensitive），其他字不動。
            val folded = if (raw in 0xFF00..0xFFEF) Normalizer.normalize(String(Character.toChars(raw)), Normalizer.Form.NFKC) else String(Character.toChars(raw))
            folded.lowercase(Locale.ROOT).codePoints().forEach { cp ->
                if (isAlphanumeric(cp)) builder.appendCodePoint(cp)
            }
        }
        return builder.toString()
    }

    /** iOS CharacterSet.alphanumerics：L*、M*、N*。 */
    private fun isAlphanumeric(cp: Int): Boolean = when (Character.getType(cp).toByte()) {
        Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
        Character.MODIFIER_LETTER, Character.OTHER_LETTER,
        Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK,
        Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER,
        -> true
        else -> false
    }

    /** iOS CharacterSet.letters：L*、M*。 */
    private fun isLetter(cp: Int): Boolean = when (Character.getType(cp).toByte()) {
        Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
        Character.MODIFIER_LETTER, Character.OTHER_LETTER,
        Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK,
        -> true
        else -> false
    }

    /** 長度用字素（grapheme）計。 */
    fun graphemeCount(text: String): Int {
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(text)
        var count = 0
        while (iterator.next() != BreakIterator.DONE) count++
        return count
    }

    fun isMeaningfulKeyword(key: String): Boolean {
        if (key.isEmpty()) return false
        if (graphemeCount(key) < 2) return key in SINGLE_CHARACTER_KEYWORDS
        if (key.codePoints().noneMatch { isLetter(it) }) return false
        return key !in KEYWORD_STOPWORDS
    }

    val SINGLE_CHARACTER_KEYWORDS: Set<String> = setOf(
        "累", "煩", "烦", "爛", "烂", "氣", "气", "窮", "穷", "忙", "慘", "惨", "怒", "悶", "闷", "苦", "吵",
        "疲", "眠", "暇",
    )

    /** 停用詞（四語，已正規化）；與 iOS YearReviewData.swift keywordStopwords 同一份。 */
    val KEYWORD_STOPWORDS: Set<String> by lazy {
        val zhHant = """
            我們 你們 他們 她們 自己 大家 別人 這個 那個 這樣 那樣 這些 那些 什麼 怎麼 為什麼 為何
            因為 所以 但是 可是 然後 而且 還是 就是 不是 只是 已經 一直 一樣 一下 一點 一個 有點
            真的 好想 想要 不想 不要 沒有 不能 不會 可以 應該 覺得 感覺 知道 今天 明天 昨天 每天
            現在 剛剛 到底 根本 實在 非常 超級 還要 又要 離職 辭職 工作 上班 下班 公司 時候 事情 東西
            老是 總是 一定 如果 的話 起來 出來 下去 這麼 那麼 多少 怎樣
            其實 可能 需要 好像 還有 一些 開始 最近 今年 這種 不用 一起
        """
        val zhHans = """
            我们 你们 他们 她们 别人 这个 那个 这样 那样 这些 那些 什么 怎么 为什么 为何
            因为 但是 然后 而且 还是 就是 不是 只是 已经 一直 一样 一点 一个 有点
            真的 好想 想要 不想 不要 没有 不能 不会 可以 应该 觉得 感觉 知道 今天 明天 昨天 每天
            现在 刚刚 到底 根本 实在 非常 超级 还要 又要 离职 辞职 工作 上班 下班 公司 时候 事情 东西
            老是 总是 一定 如果 的话 起来 出来 下去 这么 那么 怎样
            其实 可能 需要 好像 还有 一些 开始 最近 今年 这种 不用 一起
        """
        val ja = """
            わたし あなた 自分 みんな これ それ あれ どれ この その あの こと もの ため よう
            です ます でした ました ない いる ある する した して しまう しまっ なる なっ れる られる
            から まで より けど でも また もう まだ とても すごく ほんと 本当 今日 明日 昨日 毎日
            仕事 会社 退職 辞職 辞め 辞めたい やめ やめたい 思う 思っ ちょっと なんで どうして 出社 退勤
            たい すぎ って ので のに だけ いい てる なんか やっぱり
        """
        val en = """
            me my myself we our ours you your yours he him his she her it its they them their
            this that these those am is are was were be been being have has had do does did
            an the and but if or because as of at by for with about to from in on up out off over
            so than too very can could will would should just not no nor only own same such
            im ive id ill dont doesnt didnt cant wont isnt arent wasnt its thats theres
            all any some more most other again really today tomorrow yesterday every day days
            work working job quit quitting want wanna wants wanted get got go going gonna still
            always keep like feel feeling much many even also what when where who why how then
            now need know think time make back there here one keeps
        """
        listOf(zhHant, zhHans, ja, en).joinToString(" ")
            .split(' ', '\n')
            .map { normalizeForMatching(it.trim()) }
            .filter(String::isNotEmpty)
            .toSet()
    }
}
