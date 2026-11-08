package com.neilyin.nnowork.data.treehole

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class TreeHoleReportTargetType(val apiValue: String) {
    POST("post"),
    COMMENT("comment"),
}

enum class TreeHoleReportReason(val apiValue: String) {
    OFFENSIVE("offensive"),
    HARASSMENT("harassment"),
    PRIVACY("privacy"),
    SPAM("spam"),
    ILLEGAL("illegal"),
    OTHER("other"),
}

@JsonClass(generateAdapter = false)
data class BlockedTreeHoleAuthor(
    @Json(name = "author_key") val authorKey: String,
    val nickname: String,
)

sealed interface SubmitReportResult {
    data object Submitted : SubmitReportResult
    data object TargetNotFound : SubmitReportResult
    data object RateLimited : SubmitReportResult
    data class Rejected(val reason: String?) : SubmitReportResult
}

/** A banned write is different from a transient network failure and needs explicit UI copy. */
class TreeHoleAuthorBannedException : IllegalStateException("這個樹洞身份已被停權，無法發表內容。")

object TreeHoleBlockPolicy {
    private val authorKeyPattern = Regex("^[0-9a-f]{32}$")

    fun normalizeAuthorKey(authorKey: String?): String? = authorKey
        ?.trim()
        ?.lowercase()
        ?.takeIf(authorKeyPattern::matches)

    fun filterPosts(
        posts: List<TreeHolePost>,
        blockedAuthorKeys: Set<String>,
    ): List<TreeHolePost> {
        val normalizedKeys = blockedAuthorKeys.mapNotNull(::normalizeAuthorKey).toSet()
        if (normalizedKeys.isEmpty()) return posts
        return posts.filterNot { normalizeAuthorKey(it.authorKey) in normalizedKeys }
    }

    fun filterComments(
        comments: List<TreeHoleComment>,
        blockedAuthorKeys: Set<String>,
    ): List<TreeHoleComment> {
        val normalizedKeys = blockedAuthorKeys.mapNotNull(::normalizeAuthorKey).toSet()
        if (normalizedKeys.isEmpty()) return comments
        return comments.filterNot { normalizeAuthorKey(it.authorKey) in normalizedKeys }
    }
}

/**
 * p_note must be omitted when empty. Tree-hole Retrofit uses null serialization for profile
 * clearing, so a map is used here to preserve the report RPC's omitted-argument contract.
 */
internal fun buildSubmitReportBody(
    targetType: TreeHoleReportTargetType,
    targetId: String,
    deviceHash: String,
    reason: TreeHoleReportReason,
    note: String,
): Map<String, Any> {
    val normalizedTargetId = targetId.trim()
    val normalizedDeviceHash = deviceHash.trim().lowercase()
    val normalizedNote = note.trim()
    require(normalizedTargetId.isNotEmpty()) { "Report target id must not be blank" }
    require(Regex("^[0-9a-f]{64}$").matches(normalizedDeviceHash)) {
        "Device hash format is invalid"
    }
    require(normalizedNote.length <= 200) { "Report note must not exceed 200 characters" }

    return buildMap {
        put("p_target_type", targetType.apiValue)
        put("p_target_id", normalizedTargetId)
        put("p_device_hash", normalizedDeviceHash)
        put("p_reason", reason.apiValue)
        if (normalizedNote.isNotEmpty()) put("p_note", normalizedNote)
    }
}
