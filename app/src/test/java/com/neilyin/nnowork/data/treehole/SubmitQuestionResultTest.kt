package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitQuestionResultTest {
    @Test
    fun `success maps quota values`() {
        assertEquals(
            SubmitQuestionResult.Submitted(limit = 3, remaining = 2),
            SubmitQuestionResponse(success = true, limit = 3, remaining = 2).toDomainResult(),
        )
    }

    @Test
    fun `known rejection reasons map to explicit domain results`() {
        assertSame(
            SubmitQuestionResult.NotJoined,
            SubmitQuestionResponse(reason = "not_joined").toDomainResult(),
        )
        assertEquals(
            SubmitQuestionResult.RateLimited(limit = 3, remaining = 0),
            SubmitQuestionResponse(reason = "rate_limited", limit = 3, remaining = 0).toDomainResult(),
        )
        assertSame(
            SubmitQuestionResult.InvalidLength,
            SubmitQuestionResponse(reason = "invalid_length").toDomainResult(),
        )
        assertSame(
            SubmitQuestionResult.Banned,
            SubmitQuestionResponse(reason = "banned").toDomainResult(),
        )
    }

    @Test
    fun `unknown reason remains inspectable`() {
        val result = SubmitQuestionResponse(reason = "future_reason").toDomainResult()
        assertTrue(result is SubmitQuestionResult.Rejected)
        assertEquals("future_reason", (result as SubmitQuestionResult.Rejected).reason)
    }
}
