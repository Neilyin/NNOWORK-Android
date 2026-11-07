package com.neilyin.nnowork.data.treehole

import com.neilyin.nnowork.data.JsonProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeHoleModelsCompatibilityTest {
    @Test
    fun `post missing additive fields uses backward compatible defaults`() {
        val post = checkNotNull(
            JsonProvider.adapter<TreeHolePost>().fromJson(
                """{"id":"old","content":"legacy","future_field":"ignored"}""",
            ),
        )

        assertEquals("", post.avatarId)
        assertEquals(POST_TYPE_STAMP, post.postType)
        assertEquals(0, post.authorExp)
        assertEquals(null, post.authorKey)
        assertFalse(post.isQuestion)
    }

    @Test
    fun `post and comment decode latest fields`() {
        val post = checkNotNull(
            JsonProvider.adapter<TreeHolePost>().fromJson(
                """{"id":"question","content":"help","avatar_id":"cat","post_type":"question","author_answer_count":500,"author_key":"0123456789abcdef0123456789abcdef"}""",
            ),
        )
        val comment = checkNotNull(
            JsonProvider.adapter<TreeHoleComment>().fromJson(
                """{"id":"comment","nickname":"N","content":"ok","avatar_id":"dog","answer_count":100,"is_helpful":true,"author_key":"fedcba9876543210fedcba9876543210"}""",
            ),
        )

        assertTrue(post.isQuestion)
        assertEquals("cat", post.avatarId)
        assertEquals(500, post.authorExp)
        assertEquals("0123456789abcdef0123456789abcdef", post.authorKey)
        assertEquals("dog", comment.avatarId)
        assertEquals(100, comment.authorExp)
        assertTrue(comment.isHelpful)
        assertEquals("fedcba9876543210fedcba9876543210", comment.authorKey)
    }

    @Test
    fun `feed and post requests use latest wire keys`() {
        val feed = asMap(
            JsonProvider.adapter<FeedRequest>().toJson(
                FeedRequest(offset = 300, industry = "科技業", postType = POST_TYPE_QUESTION),
            ),
        )
        val post = asMap(
            JsonProvider.adapter<SubmitPostRequest>().toJson(
                SubmitPostRequest(
                    content = "今天又加班了",
                    deviceHash = "device",
                    nickname = "N",
                    industry = "科技業",
                    roundNumber = 2,
                    stampCount = 3,
                ),
            ),
        )

        assertEquals(setOf("p_limit", "p_offset", "p_industry", "p_post_type"), feed.keys)
        assertEquals(300.0, feed["p_limit"])
        assertEquals(300.0, feed["p_offset"])
        assertEquals(POST_TYPE_QUESTION, feed["p_post_type"])
        assertEquals(POST_TYPE_STAMP, post["post_type"])
    }

    @Test
    fun `question and industry requests use RPC parameter names`() {
        val question = asMap(
            JsonProvider.adapter<SubmitQuestionRequest>().toJson(
                SubmitQuestionRequest(
                    content = "這種主管該忍多久？",
                    deviceHash = "device",
                    nickname = "N",
                    industry = "科技業",
                    role = "工程師",
                    companyName = "某科技",
                ),
            ),
        )
        val update = asMap(
            JsonProvider.adapter<UpdatePostsIndustryRequest>().toJson(
                UpdatePostsIndustryRequest(deviceHash = "device", includeMismatched = true),
            ),
        )

        assertEquals(
            setOf("p_content", "p_device_hash", "p_nickname", "p_industry", "p_role", "p_company_name"),
            question.keys,
        )
        assertEquals(setOf("p_device_hash", "p_include_mismatched"), update.keys)
    }

    @Test
    fun `helpful answer request and response keep server authoritative state`() {
        val request = asMap(
            JsonProvider.adapter<MarkCommentHelpfulRequest>().toJson(
                MarkCommentHelpfulRequest(
                    commentId = "comment-id",
                    deviceHash = "device",
                    helpful = true,
                ),
            ),
        )

        assertEquals(
            setOf("p_comment_id", "p_device_hash", "p_helpful"),
            request.keys,
        )
        assertEquals(
            MarkCommentHelpfulResult.Updated(false),
            MarkCommentHelpfulResponse(success = true, isHelpful = false)
                .toDomainResult(requestedHelpful = true),
        )
        assertEquals(
            MarkCommentHelpfulResult.Rejected("not_post_owner"),
            MarkCommentHelpfulResponse(success = false, reason = "not_post_owner")
                .toDomainResult(requestedHelpful = true),
        )
    }

    @Test
    fun `author post request never contains a device hash`() {
        val request = asMap(
            JsonProvider.adapter<PostsByAuthorRequest>().toJson(
                PostsByAuthorRequest(
                    authorKey = "0123456789abcdef0123456789abcdef",
                    limit = 200,
                ),
            ),
        )

        assertEquals(setOf("p_author_key", "p_limit"), request.keys)
        assertFalse(request.keys.any { it.contains("device", ignoreCase = true) })
    }

    @Test
    fun `question activity reports any answer that is not marked helpful`() {
        assertTrue(QuestionActivity(commentCount = 3, helpfulCount = 2).hasUnmarkedHelpfulAnswer)
        assertFalse(QuestionActivity(commentCount = 3, helpfulCount = 3).hasUnmarkedHelpfulAnswer)
    }

    @Suppress("UNCHECKED_CAST")
    private fun asMap(json: String): Map<String, Any?> =
        checkNotNull(JsonProvider.moshi.adapter(Map::class.java).fromJson(json)) as Map<String, Any?>
}
