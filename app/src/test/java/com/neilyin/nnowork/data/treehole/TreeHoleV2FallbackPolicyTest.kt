package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeHoleV2FallbackPolicyTest {
    @Test
    fun `reward mutation does not retry ambiguous transport or decode failure`() {
        assertFalse(
            TreeHoleV2FallbackPolicy.shouldFallback(
                mode = V2FallbackMode.REWARD_MUTATION,
                httpStatus = null,
            ),
        )
    }

    @Test
    fun `reward mutation only retries explicit pre-execution rejection statuses`() {
        listOf(400, 401, 403, 404).forEach { status ->
            assertTrue(
                TreeHoleV2FallbackPolicy.shouldFallback(
                    mode = V2FallbackMode.REWARD_MUTATION,
                    httpStatus = status,
                ),
            )
        }
        assertFalse(
            TreeHoleV2FallbackPolicy.shouldFallback(
                mode = V2FallbackMode.REWARD_MUTATION,
                httpStatus = 500,
            ),
        )
    }

    @Test
    fun `read only v2 request remains backwards compatible`() {
        assertTrue(
            TreeHoleV2FallbackPolicy.shouldFallback(
                mode = V2FallbackMode.READ_ONLY,
                httpStatus = null,
            ),
        )
    }
}
