package com.neilyin.nnowork.data.treehole

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException

class UserTokenCallerTest {
    @Test
    fun `failed refresh rethrows the original 401 without a second call`() = runBlocking {
        var calls = 0
        val caller = UserTokenCaller({ "t" }) { false }
        try {
            caller.call { calls++; throw httpError(401) }
            fail("Expected 401")
        } catch (error: HttpException) {
            assertEquals(401, error.code())
        }
        assertEquals(1, calls)
    }

    @Test
    fun `403 is never refreshed`() = runBlocking {
        var refreshes = 0
        val caller = UserTokenCaller({ "t" }) { refreshes++; true }
        runCatching { caller.call { throw httpError(403) } }
        assertEquals(0, refreshes)
    }

    @Test
    fun `second 401 after refresh is surfaced, not retried forever`() = runBlocking {
        var calls = 0
        val caller = UserTokenCaller({ "t" }) { true }
        val result = runCatching { caller.call { calls++; throw httpError(401) } }
        assertEquals(2, calls)
        assertEquals(401, (result.exceptionOrNull() as HttpException).code())
    }

    @Test
    fun `signed in only calls return null when anonymous`() = runBlocking {
        val caller = UserTokenCaller({ "  " }) { true }
        assertNull(caller.callSignedIn { "never" })
        assertEquals("anon", caller.call { authorization -> if (authorization == null) "anon" else "user" })
    }
}
