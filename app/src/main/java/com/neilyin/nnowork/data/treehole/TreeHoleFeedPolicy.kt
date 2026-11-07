package com.neilyin.nnowork.data.treehole

data class TreeHoleFeedPage(
    val posts: List<TreeHolePost> = emptyList(),
    val nextOffset: Int = 0,
    val endReached: Boolean = false,
)

/** 無 Android 依賴的 Feed 合併與篩選規則，供 ViewModel 與 JVM 測試共用。 */
object TreeHoleFeedPolicy {
    fun mergePage(
        current: TreeHoleFeedPage,
        incoming: List<TreeHolePost>,
        pageSize: Int = DEFAULT_FEED_PAGE_SIZE,
    ): TreeHoleFeedPage {
        require(pageSize > 0) { "Page size must be positive" }
        val merged = (current.posts + incoming).distinctBy(TreeHolePost::id)
        return TreeHoleFeedPage(
            posts = merged,
            // The server offset tracks rows consumed, not unique rows displayed. Advancing by
            // the deduplicated count can request the same page forever when pages overlap.
            nextOffset = current.nextOffset + incoming.size,
            endReached = incoming.size < pageSize,
        )
    }

    fun filter(
        posts: List<TreeHolePost>,
        industries: Set<String> = emptySet(),
        postType: String? = null,
    ): List<TreeHolePost> {
        val normalizedIndustries = industries.map(String::trim).filter(String::isNotEmpty).toSet()
        val normalizedPostType = postType?.trim()?.takeIf(String::isNotEmpty)
        return posts.filter { post ->
            (normalizedIndustries.isEmpty() || post.industry in normalizedIndustries) &&
                (normalizedPostType == null || post.postType == normalizedPostType)
        }
    }
}
