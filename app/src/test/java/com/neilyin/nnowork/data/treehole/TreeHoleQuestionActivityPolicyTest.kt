package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreeHoleQuestionActivityPolicyTest {
    private val activity = listOf(
        QuestionActivity(postId = "newest", commentCount = 4, helpfulCount = 0),
        QuestionActivity(postId = "older", commentCount = 3, helpfulCount = 1),
    )

    @Test
    fun `counts only answers newer than local seen snapshot`() {
        assertEquals(
            3,
            TreeHoleQuestionActivityPolicy.newAnswerCount(
                activity,
                mapOf("newest" to 2, "older" to 2),
            ),
        )
        assertEquals(
            "newest",
            TreeHoleQuestionActivityPolicy.newestWithNewAnswers(
                activity,
                mapOf("newest" to 2, "older" to 2),
            )?.postId,
        )
    }

    @Test
    fun `no row is new when seen counts are current or ahead`() {
        val seen = mapOf("newest" to 4, "older" to 9)
        assertEquals(0, TreeHoleQuestionActivityPolicy.newAnswerCount(activity, seen))
        assertNull(TreeHoleQuestionActivityPolicy.newestWithNewAnswers(activity, seen))
    }

    @Test
    fun `reconcile clamps deleted comments and removes stale questions`() {
        assertEquals(
            mapOf("newest" to 4, "older" to 2),
            TreeHoleQuestionActivityPolicy.reconcileSeenCounts(
                activity,
                mapOf("newest" to 8, "older" to 2, "deleted" to 7),
            ),
        )
    }

    @Test
    fun `seen answer codec is deterministic and ignores malformed rows`() {
        val encoded = TreeHoleSeenAnswerCountsCodec.encode(mapOf("b" to 2, "a" to -1))
        assertEquals("a\t0\nb\t2", encoded)
        assertEquals(
            mapOf("a" to 0, "b" to 2),
            TreeHoleSeenAnswerCountsCodec.decode("$encoded\ninvalid\nc\tnot-a-number"),
        )
    }
}
