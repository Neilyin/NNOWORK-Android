package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** android開發.md §11 / §13.9：只有 HTTP 404 + PGRST202 才退回 v1。 */
class TreeHoleV2FallbackPolicyTest {
    private val identityModes = listOf(V2FallbackMode.USER_SCOPED_READ, V2FallbackMode.REWARD_MUTATION)

    @Test
    fun `identity scoped v2 does not fall back on 400 401 403 or 5xx`() {
        identityModes.forEach { mode ->
            listOf(400, 401, 403, 409, 500, 502, 503).forEach { status ->
                assertFalse("$mode $status", TreeHoleV2FallbackPolicy.shouldFallback(mode, status, null))
                assertFalse(
                    "$mode $status with PGRST202 body",
                    TreeHoleV2FallbackPolicy.shouldFallback(mode, status, PostgrestError.MISSING_FUNCTION),
                )
            }
        }
    }

    @Test
    fun `identity scoped v2 does not fall back on transport or decode failure`() {
        identityModes.forEach { mode ->
            assertFalse(TreeHoleV2FallbackPolicy.shouldFallback(mode, httpStatus = null, errorCode = null))
        }
    }

    @Test
    fun `404 without PGRST202 is not a missing function`() {
        identityModes.forEach { mode ->
            assertFalse(TreeHoleV2FallbackPolicy.shouldFallback(mode, 404, null))
            assertFalse(TreeHoleV2FallbackPolicy.shouldFallback(mode, 404, "PGRST116"))
            assertFalse(TreeHoleV2FallbackPolicy.shouldFallback(mode, 404, "pgrst202"))
        }
    }

    @Test
    fun `only 404 with PGRST202 falls back`() {
        identityModes.forEach { mode ->
            assertTrue(TreeHoleV2FallbackPolicy.shouldFallback(mode, 404, PostgrestError.MISSING_FUNCTION))
        }
    }

    @Test
    fun `public feed always falls back but only remembers missing function`() {
        listOf<Int?>(null, 400, 401, 403, 404, 500).forEach { status ->
            assertTrue(TreeHoleV2FallbackPolicy.shouldFallback(V2FallbackMode.PUBLIC_FEED_READ, status, null))
        }
        assertTrue(TreeHoleV2FallbackPolicy.shouldRememberV2Unavailable(404, "PGRST202"))
        assertFalse(TreeHoleV2FallbackPolicy.shouldRememberV2Unavailable(500, "PGRST202"))
        assertFalse(TreeHoleV2FallbackPolicy.shouldRememberV2Unavailable(404, null))
        assertFalse(TreeHoleV2FallbackPolicy.shouldRememberV2Unavailable(null, null))
    }

    @Test
    fun `postgrest error body parses code and tolerates garbage`() {
        val parsed = PostgrestError.parse(
            """{"code":"PGRST202","details":"Searched for the function public.get_feed_posts_v2","hint":null,"message":"Could not find the function"}""",
        )
        assertEquals("PGRST202", parsed?.code)
        assertTrue(isMissingRpc(404, parsed))
        assertFalse(isMissingRpc(400, parsed))
        assertNull(PostgrestError.parse("<html>gateway</html>"))
        assertNull(PostgrestError.parse(""))
        assertFalse(isMissingRpc(404, null))
    }

    @Test
    fun `missing job_index column needs 4xx code and an explicit job_index mention`() {
        val pgrst204 = PostgrestError(
            code = "PGRST204",
            message = "Could not find the 'job_index' column of 'tree_hole_posts' in the schema cache",
        )
        val undefinedColumn = PostgrestError(code = "42703", details = "column \"job_index\" does not exist")
        assertTrue(isMissingJobIndexColumn(400, pgrst204))
        assertTrue(isMissingJobIndexColumn(400, undefinedColumn))
        // 停權 403、別的欄位、5xx、constraint 都不重送。
        assertFalse(isMissingJobIndexColumn(500, pgrst204))
        assertFalse(isMissingJobIndexColumn(400, pgrst204.copy(message = "Could not find the 'role' column")))
        assertFalse(isMissingJobIndexColumn(403, PostgrestError(code = "42501", message = "banned")))
        assertFalse(isMissingJobIndexColumn(400, PostgrestError(code = "23514", message = "job_index check")))
        assertFalse(isMissingJobIndexColumn(400, null))
    }
}
