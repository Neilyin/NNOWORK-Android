package com.neilyin.nnowork.data.treehole

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

internal fun httpError(status: Int, body: String = "{}"): HttpException =
    HttpException(Response.error<Any>(status, body.toResponseBody("application/json".toMediaType())))

internal const val MISSING_FUNCTION_BODY =
    """{"code":"PGRST202","details":null,"hint":null,"message":"Could not find the function"}"""

class TreeHoleV2RouterTest {
    private fun signedInRouter(
        token: String? = "user-jwt",
        refresh: suspend (String) -> Boolean = { false },
    ) = TreeHoleV2Router(UserTokenCaller({ token }, refresh))

    @Test
    fun `user scoped v2 errors 400 401 403 500 and network never fall back to v1`() = runBlocking {
        val failures: List<Exception> = listOf(
            httpError(400), httpError(401), httpError(403), httpError(500),
            httpError(404, """{"code":"PGRST116"}"""), IOException("timeout"),
        )
        for (failure in failures) {
            for (mode in listOf(V2FallbackMode.USER_SCOPED_READ, V2FallbackMode.REWARD_MUTATION)) {
                var v1Calls = 0
                try {
                    signedInRouter().userScoped<String>(
                        mode = mode,
                        v2 = { throw failure },
                        v1 = { v1Calls++; "v1" },
                    )
                    fail("Expected $failure to propagate for $mode")
                } catch (error: Exception) {
                    assertTrue(error === failure || error is HttpException)
                }
                assertEquals("v1 must not run after $failure", 0, v1Calls)
            }
        }
    }

    @Test
    fun `user scoped v2 falls back once on 404 PGRST202 and keeps the user token`() = runBlocking {
        var v1Authorization: String? = "unset"
        val result = signedInRouter().userScoped(
            mode = V2FallbackMode.REWARD_MUTATION,
            v2 = { throw httpError(404, MISSING_FUNCTION_BODY) },
            v1 = { authorization -> v1Authorization = authorization; "v1" },
        )
        assertEquals("v1", result)
        assertEquals("Bearer user-jwt", v1Authorization)
    }

    @Test
    fun `anonymous callers go straight to v1 with the anon header`() = runBlocking {
        var v2Calls = 0
        var v1Authorization: String? = "unset"
        signedInRouter(token = null).userScoped(
            mode = V2FallbackMode.USER_SCOPED_READ,
            v2 = { v2Calls++; "v2" },
            v1 = { authorization -> v1Authorization = authorization; "v1" },
        )
        assertEquals(0, v2Calls)
        assertEquals(null, v1Authorization)
    }

    @Test
    fun `401 refreshes once then retries v2 without falling back`() = runBlocking {
        var token = "old"
        val caller = UserTokenCaller({ token }) { stale -> assertEquals("old", stale); token = "new"; true }
        val seen = mutableListOf<String>()
        val result = TreeHoleV2Router(caller).userScoped(
            mode = V2FallbackMode.USER_SCOPED_READ,
            v2 = { authorization ->
                seen += authorization
                if (authorization == "Bearer old") throw httpError(401) else "v2"
            },
            v1 = { fail("v1 must not run"); "v1" },
        )
        assertEquals("v2", result)
        assertEquals(listOf("Bearer old", "Bearer new"), seen)
    }

    @Test
    fun `public feed falls back on any failure and remembers only PGRST202`() = runBlocking {
        val router = signedInRouter()
        var v2Calls = 0
        router.publicFeed("get_feed_posts_v2", v2 = { v2Calls++; throw httpError(500) }, v1 = { "v1" })
        router.publicFeed("get_feed_posts_v2", v2 = { v2Calls++; throw IOException("x") }, v1 = { "v1" })
        assertEquals(2, v2Calls)
        assertTrue(!router.isPublicV2Unavailable("get_feed_posts_v2"))

        router.publicFeed("get_feed_posts_v2", v2 = { v2Calls++; throw httpError(404, MISSING_FUNCTION_BODY) }, v1 = { "v1" })
        assertTrue(router.isPublicV2Unavailable("get_feed_posts_v2"))
        val result = router.publicFeed("get_feed_posts_v2", v2 = { v2Calls++; "v2" }, v1 = { "v1" })
        assertEquals("v1", result)
        assertEquals(3, v2Calls)
        // 另一支 v2 不受影響。
        assertEquals("v2", router.publicFeed("get_posts_by_author_v2", v2 = { "v2" }, v1 = { "v1" }))
    }
}
