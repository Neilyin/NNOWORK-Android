package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeHoleProfileTest {
    @Test
    fun `profile requires nickname industry and role`() {
        assertFalse(TreeHoleProfile(nickname = "小 N", industry = "科技業").isComplete)
        assertFalse(TreeHoleProfile(nickname = "小 N", industry = "科技業", role = "  ").isComplete)
        assertTrue(TreeHoleProfile(nickname = "小 N", industry = "科技業", role = "工程師").isComplete)
    }

    @Test
    fun `industries match ios order`() {
        assertEquals(24, industries.size)
        assertSame(TreeHoleIndustryCatalog.values, industries)
        assertEquals("其他", industries.last())
    }
}
