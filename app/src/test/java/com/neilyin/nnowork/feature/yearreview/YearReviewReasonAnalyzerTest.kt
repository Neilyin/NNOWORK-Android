package com.neilyin.nnowork.feature.yearreview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YearReviewReasonAnalyzerTest {
    private val builtIn = mapOf(
        "老闆廢話太多" to "老闆廢話太多",
        "Boss talks too much" to "老闆廢話太多",
        "上司の話が長すぎる" to "老闆廢話太多",
        "無效會議" to "無效會議",
        "Useless meetings" to "無效會議",
    )
    private val display = mapOf("老闆廢話太多" to "Boss talks too much", "無效會議" to "Useless meetings")

    private fun insight(reasons: List<String>, fallback: String = "en") =
        YearReviewReasonAnalyzer.insight(reasons, builtIn, display, fallback, JavaWordTokenizer)

    @Test
    fun builtInReasonsMatchAcrossLocales() {
        val result = insight(listOf("老闆廢話太多", "Boss talks too much", "上司の話が長すぎる", "無效會議", "Useless meetings ", "x"))
        assertTrue(result is YearReviewReasonInsight.Reasons)
        val items = result!!.items
        assertEquals(listOf("老闆廢話太多", "無效會議"), items.map { it.key })
        assertEquals(listOf(3, 2), items.map { it.count })
        assertTrue(items.all { it.isBuiltIn })
    }

    @Test
    fun customReasonsNormalizeWidthCaseAndPunctuation() {
        val result = insight(listOf("ＯＴ太多!!", "ot 太多", "OT太多。", "  "))!!
        val first = result.items.single()
        assertEquals(3, first.count)
        assertEquals("ＯＴ太多!!", first.text) // 顯示第一次出現的寫法
        assertFalse(first.isBuiltIn)
        // 整串 emoji 正規化後是空的：用原字串比
        val emoji = insight(listOf("😡😡", "😡😡", "🙃"))!!
        assertEquals("😡😡", emoji.items.single().text)
    }

    @Test
    fun topThreeOnlyRepeatedAndTiesByFirstAppearance() {
        val result = insight(listOf("b", "a", "a", "b", "c", "c", "d", "d", "e"))!!
        assertEquals(listOf("b", "a", "c"), result.items.map { it.text })
    }

    @Test
    fun fallsBackToKeywordsWhenNoReasonRepeats() {
        val result = insight(listOf("Meetings about meetings again", "Endless meetings today", "Boss talks too much"))
        assertTrue(result is YearReviewReasonInsight.Keywords)
        val keyword = result!!.items.first()
        assertEquals("meetings", keyword.key)
        assertEquals("Meetings", keyword.text)
        // 同一句裡同一個詞只算一次
        assertEquals(2, keyword.count)
    }

    @Test
    fun noRepeatsAnywhereSkipsPage() {
        assertNull(insight(listOf("alpha", "bravo")))
        assertNull(insight(emptyList()))
    }

    @Test
    fun keywordFilters() {
        assertFalse(YearReviewReasonAnalyzer.isMeaningfulKeyword("the"))
        assertFalse(YearReviewReasonAnalyzer.isMeaningfulKeyword("離職"))
        assertFalse(YearReviewReasonAnalyzer.isMeaningfulKeyword("2026"))
        assertFalse(YearReviewReasonAnalyzer.isMeaningfulKeyword("我"))
        assertTrue(YearReviewReasonAnalyzer.isMeaningfulKeyword("累"))
        assertTrue(YearReviewReasonAnalyzer.isMeaningfulKeyword("deadline"))
        assertEquals("ot", YearReviewReasonAnalyzer.normalizeForMatching("ＯＴ"))
    }

    @Test
    fun detectsLanguageByScript() {
        assertEquals("ja", YearReviewReasonAnalyzer.detectLanguage("会議がながい"))
        assertEquals("zh", YearReviewReasonAnalyzer.detectLanguage("開會好累"))
        assertEquals("en", YearReviewReasonAnalyzer.detectLanguage("tired"))
        assertNull(YearReviewReasonAnalyzer.detectLanguage("123 !!"))
    }
}
