package com.neilyin.nnowork.data.treehole

/** 站內新回答提示的純邏輯，伺服器不儲存已讀狀態。 */
object TreeHoleQuestionActivityPolicy {
    fun newAnswerCount(
        activity: List<QuestionActivity>,
        seenCounts: Map<String, Int>,
    ): Int = activity.sumOf { row ->
        (row.commentCount.coerceAtLeast(0) - seenCounts.getOrDefault(row.postId, 0).coerceAtLeast(0))
            .coerceAtLeast(0)
    }

    fun newestWithNewAnswers(
        activity: List<QuestionActivity>,
        seenCounts: Map<String, Int>,
    ): QuestionActivity? = activity.firstOrNull { row ->
        row.commentCount.coerceAtLeast(0) > seenCounts.getOrDefault(row.postId, 0).coerceAtLeast(0)
    }

    /**
     * 留言被管理員刪除時回答數可能變小。將已讀數下修到當前數量，
     * 否則之後的新回答可能永遠追不上舊的高水位。
     */
    fun reconcileSeenCounts(
        activity: List<QuestionActivity>,
        seenCounts: Map<String, Int>,
    ): Map<String, Int> {
        val currentById = activity.associate { it.postId to it.commentCount.coerceAtLeast(0) }
        return seenCounts.mapNotNull { (postId, seen) ->
            currentById[postId]?.let { current -> postId to seen.coerceIn(0, current) }
        }.toMap()
    }
}

/** DataStore 的簡單、可向後相容編碼；post id 是 UUID，不會包含 tab 或換行。 */
internal object TreeHoleSeenAnswerCountsCodec {
    fun encode(counts: Map<String, Int>): String = counts
        .asSequence()
        .filter { (postId, _) -> postId.isNotBlank() }
        .sortedBy { (postId, _) -> postId }
        .joinToString("\n") { (postId, count) -> "$postId\t${count.coerceAtLeast(0)}" }

    fun decode(raw: String?): Map<String, Int> = raw
        .orEmpty()
        .lineSequence()
        .mapNotNull { line ->
            val separator = line.lastIndexOf('\t')
            if (separator <= 0) return@mapNotNull null
            val postId = line.substring(0, separator).trim()
            val count = line.substring(separator + 1).toIntOrNull()?.coerceAtLeast(0)
            if (postId.isEmpty() || count == null) null else postId to count
        }
        .toMap()
}
