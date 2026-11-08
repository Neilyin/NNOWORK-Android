package com.neilyin.nnowork.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.isSpecified
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class NnoworkTokensTest {
    /** 9 個既有主題＋第 10 個付費主題「夜空金」。新增主題時必須同步補齊 palette 與 token。 */
    private val expectedThemeCount = 10

    @Test
    fun themeCountMatchesThemeDefinition() {
        assertEquals(expectedThemeCount, CardTheme.entries.size)
        assertEquals((1..expectedThemeCount).toList(), CardTheme.entries.map { it.id }.sorted())
    }

    @Test
    fun existingThemeIdsAreUnchanged() {
        // 已寫入 iOS／Supabase 正式資料與使用者已購狀態，不可重新編號。
        val expected = mapOf(
            CardTheme.RPG to 1, CardTheme.CYBERPUNK to 2, CardTheme.VACATION to 3,
            CardTheme.SAKURA to 4, CardTheme.LAZY_CAT to 5, CardTheme.NOTION to 6,
            CardTheme.SUMMER to 7, CardTheme.PANDA to 8, CardTheme.SLOTH to 9,
            CardTheme.NIGHT to 10,
        )
        expected.forEach { (theme, id) -> assertEquals(theme.name, id, theme.id) }
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
    fun typeScaleIsConvergedToAboutEightLevels() {
        CardTheme.entries.forEach { theme ->
            val t = theme.typeScale
            val sizes = listOf(
                t.caption, t.small, t.body, t.bodyLarge, t.subtitle,
                t.title, t.heading, t.pageTitle, t.display, t.hero,
            )
            assertTrue("${theme.name} has an unspecified type token", sizes.all { it.isSpecified })
            assertEquals("${theme.name} type scale must be non-decreasing", sizes.map { it.value }.sorted(), sizes.map { it.value })
            val distinct = sizes.map { it.value }.toSet()
            assertTrue("${theme.name} has ${distinct.size} type sizes, expected ≤ 9", distinct.size <= 9)
            assertEquals(11f, t.caption.value)
            assertEquals(32f, t.display.value)
        }
    }

    // ── WCAG 對比 ──────────────────────────────────────────────────────

    @Test
    fun textTokensMeetWcagAaOnTheirBackgrounds() {
        val failures = mutableListOf<String>()
        CardTheme.entries.forEach { theme ->
            val c = theme.nnoworkColors
            fun check(role: String, fg: Color, bg: Color, min: Double = 4.5) {
                val ratio = wcagContrast(fg, bg)
                if (ratio < min) failures += "${theme.name} $role = ${"%.2f".format(ratio)} (< $min)"
            }
            // 驗收指定的四組
            check("primaryText/appBG", c.primaryText, c.appBG)
            check("primaryText/surface", c.primaryText, c.surface)
            check("secondaryText/appBG", c.secondaryText, c.appBG)
            check("secondaryText/surface", c.secondaryText, c.surface)
            check("onAccent/accent", c.onAccent, c.accent)
            check("buttonText/buttonBG", c.buttonText, c.buttonBG)
            // 其他實際出現在畫面上的組合
            check("primaryText/appBGBottom", c.primaryText, c.appBGBottom)
            check("primaryText/elevatedSurface", c.primaryText, c.elevatedSurface)
            check("secondaryText/elevatedSurface", c.secondaryText, c.elevatedSurface)
            check("onElevated/dialogBG", c.onElevated, c.dialogBG)
            check("primaryText/disabledBG (tonal button)", c.disabledText, c.disabledBG)
            check("accentForeground/accentSoft (badge)", c.accentForeground, c.accentSoft)
            check("primaryText/accentSoft", c.primaryText, c.accentSoft)
            check("secondaryText/accentSoft (selected tile, timer hint)", c.secondaryText, c.accentSoft)
            check("inputText/inputBG", c.inputText, c.inputBG)
            check("danger/surface", c.danger, c.surface)
            check("success/surface", c.success, c.surface)
            check("warning/surface", c.warning, c.surface)
            check("info/surface", c.info, c.surface)
            check("special/surface", c.special, c.surface)
            check("identityBadgeText/identityBadgeBG", c.identityBadgeText, c.identityBadgeBG)
            check("questionTitle/questionSoft", c.treeHole.questionTitle, c.treeHole.questionSoft)
            check("noticeText/noticeSoft", c.treeHole.noticeText, c.treeHole.noticeSoft)
            check("errorText/errorSoft", c.treeHole.errorText, c.treeHole.errorSoft)
            // 導覽列 icon＋文字：大字／icon 門檻 3:1
            check("tabSelected/tabBarBG", c.tabSelected, c.tabBarBG, 3.0)
            // 未選取分頁的文字標籤是 13sp，屬一般字級：4.5:1。
            check("tabUnselected/tabBarBG", c.tabUnselected, c.tabBarBG)
            check("navBarText/navBarBG", c.navBarText, c.navBarBG)
        }
        assertTrue("WCAG contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun lightThemesNeverUseDarkDialogsOrSheets() {
        CardTheme.entries.filter { it.usesAppWideLightPalette }.forEach { theme ->
            val c = theme.nnoworkColors
            assertTrue("${theme.name} dialogBG must be light", relativeLuminance(c.dialogBG) > 0.8)
            assertTrue("${theme.name} elevatedSurface must be light", relativeLuminance(c.elevatedSurface) > 0.8)
            assertTrue("${theme.name} surface must be light", relativeLuminance(c.surface) > 0.8)
            assertTrue("${theme.name} tab bar must be light", relativeLuminance(c.tabBarBG) > 0.8)
        }
    }

    @Test
    fun materialRolesMapToSurfaceTokens() {
        CardTheme.entries.forEach { theme ->
            val c = theme.nnoworkColors
            val scheme = c.toColorScheme(theme.usesAppWideLightPalette)
            assertEquals(theme.name, c.surface, scheme.surface)
            assertEquals(theme.name, c.surface, scheme.surfaceVariant)
            assertEquals(theme.name, c.elevatedSurface, scheme.surfaceContainerHigh)
            assertEquals(theme.name, c.elevatedSurface, scheme.surfaceContainer)
            assertEquals(theme.name, c.appBG, scheme.background)
            assertEquals(theme.name, c.primaryText, scheme.onSurface)
            assertEquals(theme.name, c.primaryText, scheme.onBackground)
            assertEquals(theme.name, c.accentSoft, scheme.secondaryContainer)
            assertEquals(theme.name, c.accent, scheme.primary)
            // 深色導覽色只給導覽列，不會出現在 surface 角色。
            if (theme.usesAppWideLightPalette) {
                assertTrue(theme.name, relativeLuminance(scheme.surface) > 0.8)
            }
        }
    }

    @Test
    fun nightThemeUsesIosYearReviewNightValues() {
        val c = CardTheme.NIGHT.nnoworkColors
        assertEquals(colorHex("#181D26"), c.appBG)
        assertEquals(colorHex("#232A36"), c.appBGBottom)
        assertEquals(colorHex("#1F2633"), c.surface)
        assertEquals(colorHex("#EAEFF5"), c.primaryText)
        assertEquals(colorHex("#F7C244"), c.accent)
        assertEquals(colorHex("#181D26"), c.onAccent)
        // 次要字 = starWhite 72% 疊在 surface 上
        val soft = colorHex("#EAEFF5").copy(alpha = 0.72f).compositeOver(colorHex("#1F2633"))
        assertTrue(wcagContrast(soft, c.secondaryText) < 1.05)
    }

    /** WCAG 2.x 相對亮度（sRGB 線性化後 0.2126R + 0.7152G + 0.0722B）。 */
    private fun relativeLuminance(color: Color): Double {
        fun channel(v: Float): Double {
            val c = v.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private fun wcagContrast(a: Color, b: Color): Double {
        // 半透明前景先疊到背景上再算。
        val fg = if (a.alpha < 1f) a.compositeOver(b) else a
        val la = relativeLuminance(fg)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
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
