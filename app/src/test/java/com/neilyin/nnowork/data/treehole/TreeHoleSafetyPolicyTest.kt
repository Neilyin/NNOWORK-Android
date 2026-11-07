package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class TreeHoleSafetyPolicyTest {
    private val blockedKey = "a".repeat(32)
    private val visibleKey = "b".repeat(32)
    private val deviceHash = "c".repeat(64)

    @Test
    fun `author keys are normalized only when they match the public identifier contract`() {
        assertEquals(blockedKey, TreeHoleBlockPolicy.normalizeAuthorKey("  ${blockedKey.uppercase()}  "))
        assertNull(TreeHoleBlockPolicy.normalizeAuthorKey("short"))
        assertNull(TreeHoleBlockPolicy.normalizeAuthorKey(null))
    }

    @Test
    fun `blocked authors are removed from posts and comments`() {
        val posts = listOf(
            TreeHolePost(id = "blocked", authorKey = blockedKey),
            TreeHolePost(id = "visible", authorKey = visibleKey),
            TreeHolePost(id = "legacy", authorKey = null),
        )
        val comments = listOf(
            TreeHoleComment(id = "blocked-comment", authorKey = blockedKey),
            TreeHoleComment(id = "visible-comment", authorKey = visibleKey),
        )

        assertEquals(
            listOf("visible", "legacy"),
            TreeHoleBlockPolicy.filterPosts(posts, setOf(blockedKey)).map(TreeHolePost::id),
        )
        assertEquals(
            listOf("visible-comment"),
            TreeHoleBlockPolicy.filterComments(comments, setOf(blockedKey)).map(TreeHoleComment::id),
        )
    }

    @Test
    fun `blocked feed rows still count toward the backend offset`() {
        val merged = TreeHoleFeedPolicy.mergePage(
            current = TreeHoleFeedPage(nextOffset = 20),
            incoming = listOf(
                TreeHolePost(id = "blocked", authorKey = blockedKey),
                TreeHolePost(id = "visible", authorKey = visibleKey),
            ),
            pageSize = 2,
        )

        assertEquals(22, merged.nextOffset)
        assertEquals(
            listOf("visible"),
            TreeHoleBlockPolicy.filterPosts(merged.posts, setOf(blockedKey)).map(TreeHolePost::id),
        )
        assertFalse(merged.endReached)
    }

    @Test
    fun `report body omits blank optional note and uses rpc field names`() {
        val body = buildSubmitReportBody(
            targetType = TreeHoleReportTargetType.COMMENT,
            targetId = "comment-id",
            deviceHash = deviceHash,
            reason = TreeHoleReportReason.HARASSMENT,
            note = "   ",
        )

        assertEquals("comment", body["p_target_type"])
        assertEquals("comment-id", body["p_target_id"])
        assertEquals(deviceHash, body["p_device_hash"])
        assertEquals("harassment", body["p_reason"])
        assertFalse(body.containsKey("p_note"))
    }

    @Test
    fun `report body trims note and enforces its limit`() {
        val body = buildSubmitReportBody(
            targetType = TreeHoleReportTargetType.POST,
            targetId = "post-id",
            deviceHash = deviceHash,
            reason = TreeHoleReportReason.OTHER,
            note = "  details  ",
        )

        assertEquals("details", body["p_note"])
        assertThrows(IllegalArgumentException::class.java) {
            buildSubmitReportBody(
                targetType = TreeHoleReportTargetType.POST,
                targetId = "post-id",
                deviceHash = deviceHash,
                reason = TreeHoleReportReason.OTHER,
                note = "x".repeat(201),
            )
        }
    }

    @Test
    fun `comment moderation reasons map to explicit results`() {
        assertSame(
            SubmitCommentResult.RateLimited,
            BasicSuccessResponse(reason = "rate_limited").toSubmitCommentResult(),
        )
        assertSame(
            SubmitCommentResult.Banned,
            BasicSuccessResponse(reason = "banned").toSubmitCommentResult(),
        )
        assertSame(
            SubmitCommentResult.InvalidLength,
            BasicSuccessResponse(reason = "too_long").toSubmitCommentResult(),
        )
    }
}
