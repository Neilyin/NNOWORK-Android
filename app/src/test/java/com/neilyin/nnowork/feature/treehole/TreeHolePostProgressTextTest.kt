package com.neilyin.nnowork.feature.treehole

import com.neilyin.nnowork.data.treehole.POST_TYPE_QUESTION
import com.neilyin.nnowork.data.treehole.TreeHolePost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class TreeHolePostProgressTextTest {
    private val zh: (Int, Int) -> String = { round, stamps -> "第 $round 張 · 第 $stamps 個印章" }
    private val en: (Int, Int) -> String = { round, stamps -> "Card $round · Stamp $stamps" }
    private val post = TreeHolePost(id = "p", content = "c", roundNumber = 2, stampCount = 5)

    @Test
    fun `first job shows the plain card progress`() {
        assertEquals("第 2 張 · 第 5 個印章", TreeHolePostProgressText.of(post, Locale.TRADITIONAL_CHINESE, zh))
        assertEquals("第 2 張 · 第 5 個印章", TreeHolePostProgressText.of(post.copy(jobIndex = 1), Locale.TRADITIONAL_CHINESE, zh))
    }

    @Test
    fun `run two plus gets the localized run prefix`() {
        val second = post.copy(jobIndex = 2)
        assertEquals("二周目 · 第 2 張 · 第 5 個印章", TreeHolePostProgressText.of(second, Locale.TRADITIONAL_CHINESE, zh))
        assertEquals("十一周目 · 第 2 張 · 第 5 個印章", TreeHolePostProgressText.of(post.copy(jobIndex = 11), Locale.SIMPLIFIED_CHINESE, zh))
        assertEquals("Run 2 · Card 2 · Stamp 5", TreeHolePostProgressText.of(second, Locale.ENGLISH, en))
        assertEquals("2周目 · Card 2 · Stamp 5", TreeHolePostProgressText.of(second, Locale.JAPANESE, en))
    }

    @Test
    fun `questions and posts without card info show nothing`() {
        assertNull(TreeHolePostProgressText.of(post.copy(postType = POST_TYPE_QUESTION, jobIndex = 3), Locale.TRADITIONAL_CHINESE, zh))
        assertNull(TreeHolePostProgressText.of(post.copy(roundNumber = null), Locale.TRADITIONAL_CHINESE, zh))
        assertNull(TreeHolePostProgressText.of(post.copy(stampCount = 0), Locale.TRADITIONAL_CHINESE, zh))
    }
}
