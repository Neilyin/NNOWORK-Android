package com.neilyin.nnowork.feature.settings

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAccountFlowTest {
    @Test
    fun `owner is captured before deletion clears the session and local archives follow`() = runBlocking {
        var owner: String? = "user-1"
        val deletedFor = mutableListOf<String?>()
        val ok = deleteAccountAndLocalArchives(
            currentOwnerId = { owner },
            deleteAccount = {
                owner = null // deleteAccount 會清掉 session
                true
            },
            deleteLocalArchives = { deletedFor += it },
        )
        assertTrue(ok)
        assertEquals(listOf<String?>("user-1"), deletedFor)
    }

    @Test
    fun `failed deletion keeps the local archives`() = runBlocking {
        val deletedFor = mutableListOf<String?>()
        val ok = deleteAccountAndLocalArchives(
            currentOwnerId = { "user-1" },
            deleteAccount = { false },
            deleteLocalArchives = { deletedFor += it },
        )
        assertFalse(ok)
        assertTrue(deletedFor.isEmpty())
    }
}
