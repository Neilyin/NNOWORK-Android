package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreeHoleIndustryCatalogTest {
    @Test
    fun `catalog matches the canonical iOS industry order`() {
        assertEquals(
            listOf(
                "科技業",
                "金融業",
                "餐飲業",
                "零售業",
                "教育業",
                "醫療業",
                "製造業",
                "媒體業",
                "服務業",
                "公職",
                "營建業",
                "物流運輸",
                "旅宿觀光",
                "美容美髮",
                "設計創意",
                "法律",
                "不動產",
                "行銷公關",
                "保險",
                "農林漁牧",
                "航空",
                "社福社工",
                "自由接案",
                "其他",
            ),
            TreeHoleIndustryCatalog.values,
        )
    }

    @Test
    fun `catalog contains 24 unique values with other last`() {
        val values = TreeHoleIndustryCatalog.values

        assertEquals(24, values.size)
        assertEquals(values.size, values.distinct().size)
        assertEquals("其他", values.last())
    }

    @Test
    fun `every canonical industry has its own semantic icon key`() {
        assertEquals(24, TreeHoleIndustryCatalog.entries.map { it.iconKey }.distinct().size)
    }

    @Test
    fun `every canonical value resolves to a semantic string resource`() {
        assertEquals(
            TreeHoleIndustryCatalog.values,
            TreeHoleIndustryCatalog.entries.map(TreeHoleIndustryDefinition::canonicalValue),
        )

        TreeHoleIndustryCatalog.entries.forEach { entry ->
            assertNotEquals("${entry.canonicalValue} must have a string resource", 0, entry.labelRes)
            assertEquals(entry, TreeHoleIndustryCatalog.find(entry.canonicalValue))
            assertEquals(entry.labelRes, TreeHoleIndustryCatalog.stringResFor(entry.canonicalValue))
        }

        assertNull(TreeHoleIndustryCatalog.stringResFor("未知舊行業"))
    }
}
