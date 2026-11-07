package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeHoleFeedPolicyTest {
    @Test
    fun `append page deduplicates by id and advances with consumed row count`() {
        val current = TreeHoleFeedPage(
            posts = listOf(post("a"), post("b")),
            nextOffset = 2,
        )

        val merged = TreeHoleFeedPolicy.mergePage(
            current = current,
            incoming = listOf(post("b"), post("c")),
            pageSize = 2,
        )

        assertEquals(listOf("a", "b", "c"), merged.posts.map(TreeHolePost::id))
        assertEquals(4, merged.nextOffset)
        assertFalse(merged.endReached)
    }

    @Test
    fun `fully duplicate page still advances offset`() {
        val current = TreeHoleFeedPage(
            posts = listOf(post("a"), post("b")),
            nextOffset = 300,
        )

        val merged = TreeHoleFeedPolicy.mergePage(
            current = current,
            incoming = listOf(post("a"), post("b")),
            pageSize = 2,
        )

        assertEquals(listOf("a", "b"), merged.posts.map(TreeHolePost::id))
        assertEquals(302, merged.nextOffset)
        assertFalse(merged.endReached)
    }

    @Test
    fun `short page marks end reached`() {
        val merged = TreeHoleFeedPolicy.mergePage(
            current = TreeHoleFeedPage(),
            incoming = listOf(post("a")),
            pageSize = 2,
        )

        assertTrue(merged.endReached)
    }

    @Test
    fun `industry and type filters compose with AND`() {
        val posts = listOf(
            post("tech-question", industry = "科技業", type = POST_TYPE_QUESTION),
            post("food-question", industry = "餐飲業", type = POST_TYPE_QUESTION),
            post("tech-stamp", industry = "科技業", type = POST_TYPE_STAMP),
        )

        val filtered = TreeHoleFeedPolicy.filter(
            posts = posts,
            industries = setOf("科技業"),
            postType = POST_TYPE_QUESTION,
        )

        assertEquals(listOf("tech-question"), filtered.map(TreeHolePost::id))
    }

    private fun post(
        id: String,
        industry: String? = null,
        type: String = POST_TYPE_STAMP,
    ) = TreeHolePost(id = id, industry = industry, postType = type)
}
