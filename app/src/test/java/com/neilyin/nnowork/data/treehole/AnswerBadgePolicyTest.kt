package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerBadgePolicyTest {
    @Test
    fun `badge thresholds match iOS contract`() {
        assertEquals(1, AnswerBadgePolicy.badgeFor(0).level)
        assertEquals(1, AnswerBadgePolicy.badgeFor(99).level)
        assertEquals(2, AnswerBadgePolicy.badgeFor(100).level)
        assertEquals(3, AnswerBadgePolicy.badgeFor(500).level)
        assertEquals(4, AnswerBadgePolicy.badgeFor(2_000).level)
        assertEquals(5, AnswerBadgePolicy.badgeFor(5_000).level)
    }

    @Test
    fun `community hides level one but profile progress starts immediately`() {
        assertFalse(AnswerBadgePolicy.isVisibleOnCommunity(99))
        assertTrue(AnswerBadgePolicy.isVisibleOnCommunity(100))
        assertEquals(100, AnswerBadgePolicy.expUntilNextBadge(0))
        assertEquals(1, AnswerBadgePolicy.expUntilNextBadge(499))
        assertNull(AnswerBadgePolicy.expUntilNextBadge(5_000))
    }
}
