package com.neilyin.nnowork.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class NnoworkPaletteTest {
    @Test
    fun everyThemeHasAccessibleSemanticTextContrast() {
        CardTheme.entries.forEach { theme ->
            val palette = theme.palette
            assertContrast(theme, "page", palette.onPage, palette.page)
            assertContrast(theme, "chrome", palette.onChrome, palette.chrome)
            assertContrast(theme, "content", palette.onContent, palette.content)
            assertContrast(theme, "contentAlt", palette.onContent, palette.contentAlt)
            assertContrast(theme, "primary", palette.onPrimary, palette.primary)
            assertContrast(theme, "accent", palette.onAccent, palette.accent)
            assertContrast(theme, "preview title", theme.titleColor, theme.backgroundColor)
            assertContrast(theme, "empty cell number", palette.onContent, theme.emptyCellColor)
        }
    }

    @Test
    fun everyStampedCellIconRemainsVisible() {
        CardTheme.entries.forEach { theme ->
            val ratio = contrastRatio(theme.stampIconColor, theme.stampedCellColor)
            assertTrue("${theme.name} stamp icon contrast was $ratio", ratio >= 3.0f)
        }
    }

    private fun assertContrast(theme: CardTheme, role: String, foreground: Color, background: Color) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("${theme.name} $role contrast was $ratio", ratio >= 4.5f)
    }

    private fun contrastRatio(a: Color, b: Color): Float {
        val lighter = max(a.luminance(), b.luminance())
        val darker = min(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
