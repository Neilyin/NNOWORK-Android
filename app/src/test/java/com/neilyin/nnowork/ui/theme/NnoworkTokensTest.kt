package com.neilyin.nnowork.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.isSpecified
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NnoworkTokensTest {
    /** Theme.kt 目前定義的主題數；新增主題時必須同步補齊 palette 與 token。 */
    private val expectedThemeCount = 9

    @Test
    fun themeCountMatchesThemeDefinition() {
        assertEquals(expectedThemeCount, CardTheme.entries.size)
        assertEquals((1..expectedThemeCount).toList(), CardTheme.entries.map { it.id }.sorted())
    }

    @Test
    fun everyThemeHasEverySemanticColorToken() {
        CardTheme.entries.forEach { theme ->
            val missing = mutableListOf<String>()
            collectColors("", theme.nnoworkColors, missing)
            assertTrue("${theme.name} has unspecified color tokens: $missing", missing.isEmpty())
        }
    }

    @Test
    fun colorTokenSetIsNotEmpty() {
        // 防呆：反射掃不到欄位時，上一個測試會空過。
        val count = countColors(CardTheme.RPG.nnoworkColors)
        assertTrue("expected at least 40 color tokens, found $count", count >= 40)
    }

    @Test
    fun everyThemeHasEveryTypeToken() {
        CardTheme.entries.forEach { theme ->
            val t = theme.typeScale
            val sizes = listOf(
                t.caption, t.small, t.body, t.bodyLarge, t.subtitle,
                t.title, t.heading, t.pageTitle, t.display, t.hero,
            )
            assertTrue("${theme.name} has an unspecified type token", sizes.all { it.isSpecified })
            assertEquals("${theme.name} type scale must be strictly increasing", sizes.map { it.value }.sorted(), sizes.map { it.value })
            assertEquals(sizes.size, sizes.map { it.value }.toSet().size)
        }
    }

    @Test
    fun semanticColorsStayTiedToThePalette() {
        CardTheme.entries.forEach { theme ->
            val colors = theme.nnoworkColors
            val palette = theme.palette
            assertEquals(theme.name, palette.page, colors.appBG)
            assertEquals(theme.name, palette.content, colors.surface)
            assertEquals(theme.name, palette.chrome, colors.navBarBG)
            assertEquals(theme.name, palette.border, colors.cardBorder)
            assertEquals(theme.name, palette.accent, colors.accent)
            assertEquals(theme.name, palette.onAccent, colors.onAccent)
        }
    }

    private fun countColors(target: Any): Int =
        target.javaClass.declaredFields.filter { !it.isSynthetic }.sumOf { field ->
            field.isAccessible = true
            when {
                field.type == java.lang.Long.TYPE -> 1
                field.type.name.startsWith("com.neilyin.nnowork") -> countColors(field.get(target)!!)
                else -> 0
            }
        }

    private fun collectColors(path: String, target: Any, missing: MutableList<String>) {
        target.javaClass.declaredFields.filter { !it.isSynthetic }.forEach { field ->
            field.isAccessible = true
            val name = path + field.name
            when {
                // Compose 的 Color 是 value class，在 JVM 上就是一個 long 欄位。
                field.type == java.lang.Long.TYPE -> {
                    if (!Color(field.getLong(target).toULong()).isSpecified) missing += name
                }
                field.type.name.startsWith("com.neilyin.nnowork") ->
                    collectColors("$name.", field.get(target)!!, missing)
            }
        }
    }
}
