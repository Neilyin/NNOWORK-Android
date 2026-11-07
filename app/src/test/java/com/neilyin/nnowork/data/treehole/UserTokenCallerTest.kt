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

    // ── 一次獎勵同步固定一份身分＋token ──

    private fun jwt(sub: String, nonce: String): String {
        val encoder = java.util.Base64.getUrlEncoder().withoutPadding()
        return listOf("""{"alg":"HS256"}""", """{"sub":"$sub","n":"$nonce"}""", "sig")
            .joinToString(".") { encoder.encodeToString(it.toByteArray()) }
    }

    @Test
    fun `pinned caller keeps using the original account after a mid-sync account switch`() = runBlocking {
        val tokenA = jwt("account-a", "1")
        var live: String? = tokenA
        val pinned = UserTokenCaller({ live }) { false }.pinned(com.neilyin.nnowork.data.auth.JwtClaims::subject)
        val identity = rewardIdentityFor(pinned.currentToken(), "device-hash") { "account-b" }

        live = jwt("account-b", "2") // 使用者在同步途中切到 B
        val seen = mutableListOf<String?>()
        pinned.call { seen += it }
        pinned.call { seen += it }

        assertEquals("account:account-a", identity)
        assertEquals(listOf("Bearer $tokenA", "Bearer $tokenA"), seen)
    }

    @Test
    fun `pinned caller refuses a refreshed token that belongs to another account`() = runBlocking {
        val tokenA = jwt("account-a", "1")
        var live: String? = tokenA
        val outer = UserTokenCaller({ live }) { live = jwt("account-b", "2"); true }
        val pinned = outer.pinned(com.neilyin.nnowork.data.auth.JwtClaims::subject)
        var calls = 0
        val result = runCatching { pinned.call { calls++; throw httpError(401) } }
        assertEquals(401, (result.exceptionOrNull() as HttpException).code())
        assertEquals("no retry with account B's token", 1, calls)
    }

    @Test
    fun `pinned caller accepts a refreshed token of the same account`() = runBlocking {
        val tokenA1 = jwt("account-a", "1")
        val tokenA2 = jwt("account-a", "2")
        var live: String? = tokenA1
        val pinned = UserTokenCaller({ live }) { live = tokenA2; true }
            .pinned(com.neilyin.nnowork.data.auth.JwtClaims::subject)
        val seen = mutableListOf<String?>()
        val result = pinned.call { authorization ->
            seen += authorization
            if (authorization == "Bearer $tokenA1") throw httpError(401) else "ok"
        }
        assertEquals("ok", result)
        assertEquals(listOf("Bearer $tokenA1", "Bearer $tokenA2"), seen)
        assertEquals(tokenA2, pinned.currentToken())
    }

    @Test
    fun `anonymous sync stays anonymous even if the user signs in mid-sync`() = runBlocking {
        var live: String? = null
        val pinned = UserTokenCaller({ live }) { true }.pinned(com.neilyin.nnowork.data.auth.JwtClaims::subject)
        assertEquals("device:device-hash", rewardIdentityFor(pinned.currentToken(), "device-hash") { "account-b" })
        live = jwt("account-b", "1")
        assertEquals("anon", pinned.call { if (it == null) "anon" else "user" })
    }

    @Test
    fun `signed in only calls return null when anonymous`() = runBlocking {
        val caller = UserTokenCaller({ "  " }) { true }
        assertNull(caller.callSignedIn { "never" })
        assertEquals("anon", caller.call { authorization -> if (authorization == null) "anon" else "user" })
    }
}
