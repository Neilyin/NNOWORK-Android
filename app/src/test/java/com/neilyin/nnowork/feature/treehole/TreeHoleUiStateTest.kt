package com.neilyin.nnowork.feature.treehole

import com.neilyin.nnowork.data.treehole.IndustrySyncStatusResponse
import com.neilyin.nnowork.data.treehole.TreeHoleProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeHoleUiStateTest {
    @Test
    fun `author key takes priority over duplicate or changed nicknames`() {
        assertFalse(
            sameTreeHoleAuthor(
                firstAuthorKey = "a",
                secondAuthorKey = "b",
                firstNickname = "same name",
                secondNickname = "same name",
            ),
        )
        assertTrue(
            sameTreeHoleAuthor(
                firstAuthorKey = "a",
                secondAuthorKey = "a",
                firstNickname = "old name",
                secondNickname = "new name",
            ),
        )
    }

    @Test
    fun `legacy rows fall back to nickname only when an author key is missing`() {
        assertTrue(
            sameTreeHoleAuthor(
                firstAuthorKey = null,
                secondAuthorKey = "b",
                firstNickname = "legacy",
                secondNickname = "legacy",
            ),
        )
        assertFalse(sameTreeHoleAuthor(null, null, null, "legacy"))
    }

    private val profile = TreeHoleProfile(
        nickname = "tester",
        industry = "科技業",
        role = "engineer",
    )

    @Test
    fun `industry sync is pending only when server industry exactly matches local profile`() {
        val matching = stateWithServerIndustry("科技業")
        val different = stateWithServerIndustry("金融業")
        val padded = stateWithServerIndustry(" 科技業 ")

        assertNotNull(matching.pendingIndustrySyncStatus)
        assertNull(different.pendingIndustrySyncStatus)
        assertNull(padded.pendingIndustrySyncStatus)
    }

    @Test
    fun `declined current industry is not pending`() {
        val state = stateWithServerIndustry("科技業").copy(
            declinedIndustrySyncFor = "科技業",
        )

        assertNull(state.pendingIndustrySyncStatus)
    }

    private fun stateWithServerIndustry(industry: String) = TreeHoleUiState(
        profile = profile,
        industrySyncStatus = IndustrySyncStatusResponse(
            eligible = true,
            currentIndustry = industry,
            unsetCount = 1,
        ),
    )
}
