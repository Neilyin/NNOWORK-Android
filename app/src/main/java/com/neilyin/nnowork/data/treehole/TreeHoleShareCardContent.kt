package com.neilyin.nnowork.data.treehole

import java.util.Locale

/**
 * 樹洞分享圖唯一可用的內容型別。
 *
 * 分享圖的障蔽要求是一份白名單，因此渲染與分享層不得接收 [TreeHolePost]；
 * 這個型別僅提供貼文類型與可公開的卡片內容，刻意不提供暱稱、公司、貼文 id、
 * 時間或互動統計。
 */
data class TreeHoleShareCardContent(
    val content: String,
    val industry: String?,
    val role: String?,
    val roundNumber: Int?,
    val stampCount: Int?,
    val postType: String,
    /** 周目（[TreeHolePost.displayRunNumber]）；null 或 1 不顯示（§20.1）。 */
    val jobIndex: Int? = null,
) {
    val isQuestion: Boolean
        get() = postType == POST_TYPE_QUESTION
}

enum class TreeHoleShareSource(val analyticsValue: String) {
    FeedList("feed_list"),
    PostDetail("post_detail"),
    CardStack("card_stack"),
}

fun TreeHolePost.toShareCardContent(): TreeHoleShareCardContent =
    TreeHoleShareCardContent(
        content = content,
        industry = industry?.takeIf { it.isNotBlank() },
        role = role?.takeIf { it.isNotBlank() },
        roundNumber = roundNumber,
        stampCount = stampCount,
        postType = postType,
        jobIndex = displayRunNumber,
    )

/**
 * 分享圖的底部識別。提問不具有離職集點進度，因此即使後端夾帶舊的
 * round/stamp 欄位，也只能顯示提問識別。非提問貼文則維持原有文案與組合方式。
 */
internal fun TreeHoleShareCardContent.shareCardFooterText(): String? {
    if (isQuestion) return QUESTION_SHARE_CARD_LABEL

    return buildList {
        // 分享圖文案目前只有繁中；周目前綴跟著用繁中國字（iOS TreeHolePostShareView 同位置）。
        roundNumber?.let {
            add(TreeHoleProgressLabel.applyingRunPrefix("第 $it 張離職集點卡", jobIndex, Locale.TRADITIONAL_CHINESE))
        }
        stampCount?.let { add("第 $it 章") }
    }.joinToString("  ·  ")
        .takeIf(String::isNotEmpty)
}

internal const val QUESTION_SHARE_CARD_LABEL = "職場解惑｜提問"
