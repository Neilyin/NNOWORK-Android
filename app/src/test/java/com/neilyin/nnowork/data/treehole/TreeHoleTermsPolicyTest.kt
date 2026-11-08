package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeHoleTermsPolicyTest {
    @Test
    fun `current terms include the zero tolerance ugc revision`() {
        assertEquals(3, CURRENT_TREE_HOLE_TERMS_VERSION)
        assertTrue(TREE_HOLE_DISCLAIMER_V3.contains("零容忍政策"))
    }

    @Test
    fun `unloaded or never accepted users cannot access tree hole`() {
        assertFalse(TreeHoleTermsPolicy.canAccess(null))
        assertFalse(TreeHoleTermsPolicy.canAccess(0))
    }

    @Test
    fun `older accepted versions require consent again`() {
        assertFalse(TreeHoleTermsPolicy.canAccess(CURRENT_TREE_HOLE_TERMS_VERSION - 1))
    }

    @Test
    fun `current accepted version allows access`() {
        assertTrue(TreeHoleTermsPolicy.canAccess(CURRENT_TREE_HOLE_TERMS_VERSION))
    }

    @Test
    fun `future accepted version remains accepted`() {
        assertTrue(TreeHoleTermsPolicy.canAccess(CURRENT_TREE_HOLE_TERMS_VERSION + 1))
    }
}
