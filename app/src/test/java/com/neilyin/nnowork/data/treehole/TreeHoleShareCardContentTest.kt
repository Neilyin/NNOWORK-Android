package com.neilyin.nnowork.data.treehole

import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Modifier

class TreeHoleShareCardContentTest {
    @Test
    fun `share content type exposes only approved fields`() {
        val exposedFields = TreeHoleShareCardContent::class.java.declaredFields
            .filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

        assertEquals(
            setOf("content", "industry", "role", "roundNumber", "stampCount", "postType"),
            exposedFields,
        )
    }

    @Test
    fun `post mapping excludes identity company and engagement data`() {
        val post = TreeHolePost(
            id = "private-post-id",
            content = "這是要分享的貼文內容",
            industry = "科技業",
            nickname = "不可外洩的暱稱",
            role = "工程師",
            companyName = "不可外洩的公司",
            roundNumber = 3,
            stampCount = 7,
            likesCount = 99,
            cheerCount = 88,
            cryCount = 77,
            commentCount = 66,
        )

        assertEquals(
            TreeHoleShareCardContent(
                content = "這是要分享的貼文內容",
                industry = "科技業",
                role = "工程師",
                roundNumber = 3,
                stampCount = 7,
                postType = POST_TYPE_STAMP,
            ),
            post.toShareCardContent(),
        )
    }

    @Test
    fun `question mapping keeps type but its footer never leaks stamp progress`() {
        val post = TreeHolePost(
            id = "private-question-id",
            content = "這是要分享的提問",
            industry = "科技業",
            nickname = "不可外洩的暱稱",
            role = "工程師",
            companyName = "不可外洩的公司",
            roundNumber = 12,
            stampCount = 34,
            postType = POST_TYPE_QUESTION,
            likesCount = 99,
            cheerCount = 88,
            cryCount = 77,
            commentCount = 66,
        )

        val shareContent = post.toShareCardContent()

        assertEquals(POST_TYPE_QUESTION, shareContent.postType)
        assertEquals(QUESTION_SHARE_CARD_LABEL, shareContent.shareCardFooterText())
        assertEquals("職場解惑｜提問", shareContent.shareCardFooterText())
    }

    @Test
    fun `stamp footer remains unchanged`() {
        val shareContent = TreeHoleShareCardContent(
            content = "原有的蓋章貼文",
            industry = null,
            role = null,
            roundNumber = 3,
            stampCount = 7,
            postType = POST_TYPE_STAMP,
        )

        assertEquals("第 3 張離職集點卡  ·  第 7 章", shareContent.shareCardFooterText())
    }

    @Test
    fun `share analytics sources match the cross platform contract`() {
        assertEquals("feed_list", TreeHoleShareSource.FeedList.analyticsValue)
        assertEquals("post_detail", TreeHoleShareSource.PostDetail.analyticsValue)
        assertEquals("card_stack", TreeHoleShareSource.CardStack.analyticsValue)
    }
}
