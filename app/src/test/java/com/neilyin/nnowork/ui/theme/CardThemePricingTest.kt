package com.neilyin.nnowork.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class CardThemePricingTest {
    @Test
    fun `exactly seven premium themes cost fifteen coins`() {
        val premiumThemes = CardTheme.entries.filter(CardTheme::requiresCoins)

        assertEquals(
            listOf(
                CardTheme.SAKURA,
                CardTheme.LAZY_CAT,
                CardTheme.NOTION,
                CardTheme.SUMMER,
                CardTheme.PANDA,
                CardTheme.SLOTH,
                // 第 10 個主題：比照既有付費主題的最高定價（15 金幣），同一套購買流程。
                CardTheme.NIGHT,
            ),
            premiumThemes,
        )
        premiumThemes.forEach { assertEquals(15, it.coinCost) }
        CardTheme.entries.filterNot(CardTheme::requiresCoins).forEach {
            assertEquals(0, it.coinCost)
        }
    }

    @Test
    fun `free and owned themes apply app wide while locked themes fall back safely`() {
        val owned = listOf(CardTheme.SAKURA.id, CardTheme.NOTION.id)

        assertEquals(CardTheme.RPG, CardTheme.RPG.appWideTheme(owned))
        assertEquals(CardTheme.CYBERPUNK, CardTheme.CYBERPUNK.appWideTheme(owned))
        assertEquals(CardTheme.VACATION, CardTheme.VACATION.appWideTheme(owned))
        assertEquals(CardTheme.SAKURA, CardTheme.SAKURA.appWideTheme(owned))
        assertEquals(CardTheme.NOTION, CardTheme.NOTION.appWideTheme(owned))
        assertEquals(CardTheme.RPG, CardTheme.LAZY_CAT.appWideTheme(owned))
        assertEquals(CardTheme.RPG, CardTheme.SUMMER.appWideTheme(owned))
        assertEquals(CardTheme.RPG, CardTheme.NIGHT.appWideTheme(owned))
        // 夜空金暫不上架：即使已購買也退回預設主題，且不出現在商店。
        assertEquals(CardTheme.RPG, CardTheme.NIGHT.appWideTheme(owned + CardTheme.NIGHT.id))
    }

    @Test
    fun `unreleased night theme is hidden from the shop`() {
        assertEquals(false, CardTheme.NIGHT.isReleased)
        assertEquals(false, CardTheme.NIGHT in CardTheme.shopEntries)
        assertEquals(CardTheme.entries.size - 1, CardTheme.shopEntries.size)
    }

    @Test
    fun `shop product ids exactly match the shared ios contract`() {
        assertEquals(
            listOf(
                "theme.rpg",
                "theme.cyberpunk",
                "theme.vacation",
                "theme.sakura",
                "theme.lazy_cat",
                "theme.notion",
                "theme.summer",
                "theme.panda",
                "theme.sloth",
                "theme.night",
            ),
            CardTheme.entries.map(CardTheme::shopProductId),
        )
    }

    @Test
    fun `custom stamp variants and tilt stay deterministic`() {
        assertEquals(CardTheme.PANDA.stampImageResource(0), CardTheme.PANDA.stampImageResource(3))
        assertEquals(CardTheme.SLOTH.stampImageResource(2), CardTheme.SLOTH.stampImageResource(5))
        assertEquals(null, CardTheme.SUMMER.stampImageResource(0))
        assertEquals(stampTiltDegrees(1), stampTiltDegrees(11))
    }
}
