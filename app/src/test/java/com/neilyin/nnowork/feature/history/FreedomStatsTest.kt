package com.neilyin.nnowork.feature.history

import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/** 自由畫面（iOS FreedomView）的冒險成果與對話框文字組合。 */
class FreedomStatsTest {
    private fun at(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }.timeInMillis

    @Test
    fun `cards is the current round and stamps counts every record`() {
        val stamps = listOf(
            StampRecordEntity(date = at(2026, 1, 10), roundNumber = 1),
            StampRecordEntity(date = at(2026, 1, 11), roundNumber = 1),
            StampRecordEntity(date = at(2026, 2, 1), roundNumber = 2),
        )
        val settings = AppSettings(companyName = "Acme", currentRound = 2, cardCompletedDate = at(2026, 2, 9))
        assertEquals(FreedomStats("Acme", cards = 2, stamps = 3, days = 30), computeFreedomStats(settings, stamps))
    }

    @Test
    fun `days fall back to today when the card has no completion date`() {
        val stats = computeFreedomStats(
            AppSettings(currentRound = 0),
            listOf(StampRecordEntity(date = at(2026, 3, 1))),
            nowMillis = at(2026, 3, 4),
        )
        assertEquals(3, stats.days)
        assertEquals(1, stats.cards)
        assertEquals("", stats.companyName)
    }

    @Test
    fun `full width punctuation joins directly and latin text gets a space`() {
        assertEquals("恭喜你！祝你奔向自由！", joinFreedomLine("恭喜你！", "祝你奔向自由！"))
        assertEquals("再見了，牛馬科技！", joinFreedomLine("再見了，", "牛馬科技！"))
        assertEquals("Congratulations! Run toward freedom!", joinFreedomLine("Congratulations!", "Run toward freedom!"))
        assertEquals("Goodbye, Acme!", joinFreedomLine("Goodbye,", "Acme!"))
        assertEquals("only", joinFreedomLine("", "only"))
    }

    @Test
    fun `dialog text has the three ios paragraphs`() {
        val text = freedomDialogText("恭喜你！", "祝你奔向自由！", "再見了，", "公司！", "願你找到更好的歸屬", "人生苦短，不要委屈自己")
        assertEquals("恭喜你！祝你奔向自由！\n再見了，公司！\n願你找到更好的歸屬\n人生苦短，不要委屈自己", text)
    }
}
