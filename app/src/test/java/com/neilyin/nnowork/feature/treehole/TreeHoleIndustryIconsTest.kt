package com.neilyin.nnowork.feature.treehole

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
import com.neilyin.nnowork.data.treehole.TreeHoleIndustryCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TreeHoleIndustryIconsTest {
    @Test
    fun `all canonical industries resolve to distinct non-generic icons`() {
        val iconNames = TreeHoleIndustryCatalog.values.map { canonical ->
            treeHoleIndustryIcon(canonical).name.also { iconName ->
                assertNotEquals(Icons.Filled.Work.name, iconName)
            }
        }

        assertEquals(24, iconNames.distinct().size)
    }

    @Test
    fun `legacy unknown industry falls back to generic work icon`() {
        assertEquals(Icons.Filled.Work.name, treeHoleIndustryIcon("舊版未知職業").name)
    }
}
