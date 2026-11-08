package com.neilyin.nnowork.data.treehole

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeHoleProfileSyncPolicyTest {
    @Test
    fun `successful signed in upsert reclaims device data`() = runBlocking {
        var claims = 0

        val result = completeProfileSync(upsertSucceeded = true, signedIn = true) {
            claims += 1
            true
        }

        assertTrue(result)
        assertEquals(1, claims)
    }

    @Test
    fun `failed claim leaves profile sync retryable`() = runBlocking {
        assertFalse(
            completeProfileSync(upsertSucceeded = true, signedIn = true) { false },
        )
    }

    @Test
    fun `anonymous or failed upsert never claims`() = runBlocking {
        var claims = 0
        val claim = suspend {
            claims += 1
            true
        }

        assertTrue(
            completeProfileSync(
                upsertSucceeded = true,
                signedIn = false,
                claimDeviceData = claim,
            ),
        )
        assertFalse(
            completeProfileSync(
                upsertSucceeded = false,
                signedIn = true,
                claimDeviceData = claim,
            ),
        )
        assertEquals(0, claims)
    }

    @Test
    fun `industry mutation runs only after complete profile sync`() = runBlocking {
        val calls = mutableListOf<String>()
        val profile = TreeHoleProfile(
            nickname = "nickname",
            industry = "科技業",
            role = "engineer",
        )

        val result = mutateAfterProfileSync(
            localProfile = profile,
            syncProfile = {
                calls += "sync"
                true
            },
            mutation = {
                calls += "mutation"
                7
            },
        )

        assertEquals(7, result)
        assertEquals(listOf("sync", "mutation"), calls)
    }

    @Test
    fun `failed profile sync blocks industry mutation`() {
        var mutated = false
        val profile = TreeHoleProfile(
            nickname = "nickname",
            industry = "科技業",
            role = "engineer",
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                mutateAfterProfileSync(
                    localProfile = profile,
                    syncProfile = { false },
                    mutation = {
                        mutated = true
                    },
                )
            }
        }
        assertFalse(mutated)
    }
}
