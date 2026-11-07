package com.neilyin.nnowork.feature.manual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualContentTest {
    @Test fun sectionStructureMatchesIos() {
        assertEquals(8, ManualSections.size)
        assertEquals(listOf(4, 3, 6, 2, 9, 4, 5, 3), ManualSections.map { it.bullets.size })
        assertEquals(ManualSections.size, ManualSections.map { it.id }.toSet().size)
    }

    @Test fun demosOnStampSalaryTreehole() {
        assertEquals(
            listOf(ManualDemo.StampCard, null, ManualDemo.Salary, null, ManualDemo.TreeHole, null, null, null),
            ManualSections.map { it.demo },
        )
    }

    @Test fun toggleAddsAndRemoves() {
        val open = toggleExpanded(emptySet(), "a")
        assertTrue("a" in open)
        val both = toggleExpanded(open, "b")
        assertEquals(setOf("a", "b"), both)
        assertEquals(setOf("b"), toggleExpanded(both, "a"))
    }
}
