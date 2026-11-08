package com.neilyin.nnowork.data.treehole

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class TreeHoleProfile(
    val nickname: String = "",
    val industry: String = "",
    val role: String = "",
    @Json(name = "company_name") val companyName: String = "",
    @Json(name = "show_company") val showCompany: Boolean = false,
) {
    val isComplete: Boolean
        get() = nickname.isNotBlank() && industry.isNotBlank() && role.isNotBlank()
}

@JsonClass(generateAdapter = false)
data class TreeHolePost(
    val id: String = "",
    val content: String = "",
    val industry: String? = null,
    val nickname: String? = null,
    val role: String? = null,
    @Json(name = "company_name") val companyName: String? = null,
    @Json(name = "round_number") val roundNumber: Int? = null,
    @Json(name = "stamp_count") val stampCount: Int? = null,
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "cheer_count") val cheerCount: Int = 0,
    @Json(name = "cry_count") val cryCount: Int = 0,
    val status: String? = null,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "comment_count") val commentCount: Int = 0,
    @Json(name = "latest_comment") val latestComment: String? = null,
    @Json(name = "latest_comment_nickname") val latestCommentNickname: String? = null,
    @Json(name = "avatar_id") val avatarId: String = "",
    @Json(name = "post_type") val postType: String = POST_TYPE_STAMP,
    /** JSON key 沿用 author_answer_count，db/020 起內容是解答 EXP。 */
    @Json(name = "author_answer_count") val authorExp: Int = 0,
    /** 不可逆的作者識別碼；絕對不可以 device_hash 取代。 */
    @Json(name = "author_key") val authorKey: String? = null,
    /**
     * 周目快照（db/032，只有 *_v2 feed RPC 會回）。null 或 1 = 第一份工作；≥ 2 顯示「二周目」。
     * v1 RPC／舊資料沒有這欄，解碼為 null，向後相容。
     */
    @Json(name = "job_index") val jobIndex: Int? = null,
) {
    val isQuestion: Boolean
        get() = postType == POST_TYPE_QUESTION

    /** UI 用：要顯示的周目（≥ 2 才有值；提問沒有卡片資訊所以不顯示）。 */
    val displayRunNumber: Int?
        get() = jobIndex?.takeIf { it >= 2 && !isQuestion }
}

@JsonClass(generateAdapter = false)
data class TreeHoleComment(
    val id: String = "",
    val nickname: String = "",
    val industry: String? = null,
    val content: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "avatar_id") val avatarId: String = "",
    /** JSON key 沿用 answer_count，db/020 起內容是解答 EXP。 */
    @Json(name = "answer_count") val authorExp: Int = 0,
    @Json(name = "is_helpful") val isHelpful: Boolean = false,
    /** 不可逆的作者識別碼；舊資料可能缺值。 */
    @Json(name = "author_key") val authorKey: String? = null,
)

@JsonClass(generateAdapter = false)
data class MyTreeHoleComment(
    val id: String = "",
    @Json(name = "post_id") val postId: String = "",
    val content: String = "",
    val status: String = "pending",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "post_content") val postContent: String? = null,
)

@JsonClass(generateAdapter = false)
data class FeedRequest(
    @Json(name = "p_limit") val limit: Int = DEFAULT_FEED_PAGE_SIZE,
    @Json(name = "p_offset") val offset: Int = 0,
    @Json(name = "p_industry") val industry: String? = null,
    @Json(name = "p_post_type") val postType: String? = null,
)

@JsonClass(generateAdapter = false)
data class DeviceHashRequest(
    @Json(name = "p_device_hash") val deviceHash: String,
)

@JsonClass(generateAdapter = false)
data class PostsByAuthorRequest(
    @Json(name = "p_author_key") val authorKey: String,
    @Json(name = "p_limit") val limit: Int = 200,
)

@JsonClass(generateAdapter = false)
data class UpsertProfileRequest(
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_nickname") val nickname: String,
    @Json(name = "p_industry") val industry: String,
    @Json(name = "p_company_name") val companyName: String?,
    @Json(name = "p_show_company") val showCompany: Boolean,
    @Json(name = "p_role") val role: String,
)

/** 舊協定回應；缺 key 視為格式錯誤（null），不當成 0。 */
@JsonClass(generateAdapter = false)
data class NewApprovalsResponse(
    @Json(name = "new_approvals") val newApprovals: Int? = null,
)

@JsonClass(generateAdapter = false)
data class NewFeedbackRewardsResponse(
    @Json(name = "new_rewards") val newRewards: Int? = null,
)

@JsonClass(generateAdapter = false)
data class DeletePostRequest(
    @Json(name = "p_post_id") val postId: String,
    @Json(name = "p_device_hash") val deviceHash: String,
)

@JsonClass(generateAdapter = false)
data class DeleteCommentRequest(
    @Json(name = "p_comment_id") val commentId: String,
    @Json(name = "p_device_hash") val deviceHash: String,
)

@JsonClass(generateAdapter = false)
data class CommentsRequest(
    @Json(name = "p_post_id") val postId: String,
)

@JsonClass(generateAdapter = false)
data class SubmitCommentRequest(
    @Json(name = "p_post_id") val postId: String,
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_nickname") val nickname: String,
    @Json(name = "p_industry") val industry: String,
    @Json(name = "p_content") val content: String,
)

@JsonClass(generateAdapter = false)
data class ReactPostRequest(
    @Json(name = "p_post_id") val postId: String,
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_reaction_type") val reactionType: String,
)

data class SubmitPostRequest(
    val content: String,
    val deviceHash: String,
    val nickname: String,
    val industry: String,
    val role: String? = null,
    val roundNumber: Int,
    val stampCount: Int,
    val companyName: String? = null,
    val postType: String = POST_TYPE_STAMP,
    /** 周目（currentRunNumber），**不是**封存編號 currentJobIndex。 */
    val jobIndex: Int? = null,
)

/** 送文要不要帶 job_index：第一份工作（≤ 1 或未知）不帶，伺服器存 NULL＝第一份（§20.3）。 */
internal fun postJobIndexValue(runNumber: Int?): Int? = runNumber?.takeIf { it >= 2 }

/**
 * tree_hole_posts insert 的 wire body。舊欄位（含 role/company_name 的明確 null）與既有行為相同；
 * job_index 只在周目 ≥ 2 才出現，第一份工作的 body 與舊版一模一樣。
 */
internal fun buildSubmitPostBody(
    request: SubmitPostRequest,
    includeJobIndex: Boolean = true,
): Map<String, Any?> = buildMap {
    put("content", request.content)
    put("device_hash", request.deviceHash)
    put("nickname", request.nickname)
    put("industry", request.industry)
    put("role", request.role)
    put("round_number", request.roundNumber)
    put("stamp_count", request.stampCount)
    put("company_name", request.companyName)
    put("post_type", request.postType)
    if (includeJobIndex) postJobIndexValue(request.jobIndex)?.let { put("job_index", it) }
}

@JsonClass(generateAdapter = false)
data class SubmitQuestionRequest(
    @Json(name = "p_content") val content: String,
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_nickname") val nickname: String,
    @Json(name = "p_industry") val industry: String,
    @Json(name = "p_role") val role: String? = null,
    @Json(name = "p_company_name") val companyName: String? = null,
)

@JsonClass(generateAdapter = false)
data class SubmitQuestionResponse(
    val success: Boolean = false,
    val reason: String? = null,
    val limit: Int? = null,
    val remaining: Int? = null,
)

sealed interface SubmitQuestionResult {
    data class Submitted(val limit: Int, val remaining: Int) : SubmitQuestionResult
    data object NotJoined : SubmitQuestionResult
    data class RateLimited(val limit: Int, val remaining: Int) : SubmitQuestionResult
    data object InvalidLength : SubmitQuestionResult
    data object Banned : SubmitQuestionResult
    data class Rejected(val reason: String?) : SubmitQuestionResult
}

internal fun SubmitQuestionResponse.toDomainResult(): SubmitQuestionResult = when {
    success -> SubmitQuestionResult.Submitted(limit = limit ?: 3, remaining = remaining ?: 0)
    reason == "not_joined" -> SubmitQuestionResult.NotJoined
    reason == "rate_limited" -> SubmitQuestionResult.RateLimited(
        limit = limit ?: 3,
        remaining = remaining ?: 0,
    )
    reason == "invalid_length" -> SubmitQuestionResult.InvalidLength
    reason == "banned" -> SubmitQuestionResult.Banned
    else -> SubmitQuestionResult.Rejected(reason)
}

@JsonClass(generateAdapter = false)
data class QuestionQuotaResponse(
    val limit: Int = 3,
    val used: Int = 0,
    val remaining: Int = 3,
    val joined: Boolean = false,
)

@JsonClass(generateAdapter = false)
data class IndustrySyncBreakdown(
    val industry: String = "",
    val count: Int = 0,
)

@JsonClass(generateAdapter = false)
data class IndustrySyncStatusResponse(
    val eligible: Boolean = false,
    @Json(name = "current_industry") val currentIndustry: String? = null,
    @Json(name = "unset_count") val unsetCount: Int = 0,
    @Json(name = "mismatched_count") val mismatchedCount: Int = 0,
    val breakdown: List<IndustrySyncBreakdown> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class UpdatePostsIndustryRequest(
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_include_mismatched") val includeMismatched: Boolean,
)

@JsonClass(generateAdapter = false)
data class UpdatePostsIndustryResponse(
    val success: Boolean = false,
    val updated: Int = 0,
    val industry: String? = null,
)

@JsonClass(generateAdapter = false)
data class MyAnswerStatsResponse(
    @Json(name = "answer_count") val answerCount: Int = 0,
    @Json(name = "helpful_count") val helpfulCount: Int = 0,
    val exp: Int = 0,
)

data class TreeHoleAnswerStats(
    val answerCount: Int = 0,
    val helpfulCount: Int = 0,
    val exp: Int = 0,
)

@JsonClass(generateAdapter = false)
data class NicknameSyncResponse(
    val success: Boolean = false,
    val nickname: String? = null,
    @Json(name = "posts_updated") val postsUpdated: Int = 0,
    @Json(name = "comments_updated") val commentsUpdated: Int = 0,
    val reason: String? = null,
) {
    val totalUpdated: Int
        get() = postsUpdated.coerceAtLeast(0) + commentsUpdated.coerceAtLeast(0)
}

@JsonClass(generateAdapter = false)
data class MarkCommentHelpfulRequest(
    @Json(name = "p_comment_id") val commentId: String,
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_helpful") val helpful: Boolean,
)

@JsonClass(generateAdapter = false)
data class MarkCommentHelpfulResponse(
    val success: Boolean = false,
    @Json(name = "is_helpful") val isHelpful: Boolean? = null,
    val reason: String? = null,
)

sealed interface MarkCommentHelpfulResult {
    data class Updated(val isHelpful: Boolean) : MarkCommentHelpfulResult
    data class Rejected(val reason: String?) : MarkCommentHelpfulResult
}

internal fun MarkCommentHelpfulResponse.toDomainResult(
    requestedHelpful: Boolean,
): MarkCommentHelpfulResult = if (success) {
    MarkCommentHelpfulResult.Updated(isHelpful ?: requestedHelpful)
} else {
    MarkCommentHelpfulResult.Rejected(reason)
}

@JsonClass(generateAdapter = false)
data class QuestionActivity(
    @Json(name = "post_id") val postId: String = "",
    val content: String = "",
    @Json(name = "comment_count") val commentCount: Int = 0,
    @Json(name = "helpful_count") val helpfulCount: Int = 0,
    @Json(name = "latest_comment_at") val latestCommentAt: String? = null,
) {
    val hasUnmarkedHelpfulAnswer: Boolean
        get() = commentCount.coerceAtLeast(0) > helpfulCount.coerceAtLeast(0)
}

@JsonClass(generateAdapter = false)
data class DeleteResponse(val success: Boolean = false)

@JsonClass(generateAdapter = false)
data class SubmitFeedbackRequest(
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_content") val content: String,
)

@JsonClass(generateAdapter = false)
data class FeedbackResponse(
    val success: Boolean = false,
    val reason: String? = null,
)

@JsonClass(generateAdapter = false)
data class RewardCapabilitiesResponse(
    val version: Int? = null,
)

@JsonClass(generateAdapter = false)
data class ClaimRewardEventsRequest(
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_delivery_id") val deliveryId: String,
)

@JsonClass(generateAdapter = false)
data class RewardEventResponse(
    val id: String = "",
    val kind: String = "",
    val amount: Int = 0,
)

@JsonClass(generateAdapter = false)
data class AckRewardEventsRequest(
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_delivery_id") val deliveryId: String,
    @Json(name = "p_event_ids") val eventIds: List<String>,
)

@JsonClass(generateAdapter = false)
data class AckRewardEventsResponse(
    val acknowledged: Boolean = false,
    @Json(name = "acknowledged_ids") val acknowledgedIds: List<String> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class ArchiveMyJobResponse(
    val id: String? = null,
)

@JsonClass(generateAdapter = false)
data class DevicePushTokenRequest(
    @Json(name = "device_hash") val deviceHash: String,
    @Json(name = "fcm_token") val fcmToken: String,
    val platform: String = "android",
)

@JsonClass(generateAdapter = false)
data class BasicSuccessResponse(
    val success: Boolean = false,
    val reason: String? = null,
    val action: String? = null,
)

/**
 * 留言內容規則（db/033 起不限字數，§5.9）：去頭尾空白／tab／換行後至少 1 字，沒有上限。
 * 回傳要送出的內容；空白回 null。
 */
object TreeHoleCommentPolicy {
    const val MIN_LENGTH = 1

    fun normalizedContentOrNull(content: String): String? =
        content.trim().takeIf { it.length >= MIN_LENGTH }
}

sealed interface SubmitCommentResult {
    data object Submitted : SubmitCommentResult
    data object InvalidLength : SubmitCommentResult
    data object RateLimited : SubmitCommentResult
    data object Banned : SubmitCommentResult
    data class Rejected(val reason: String?) : SubmitCommentResult
}

internal fun BasicSuccessResponse.toSubmitCommentResult(): SubmitCommentResult = when {
    success -> SubmitCommentResult.Submitted
    reason == "invalid_length" || reason == "too_long" -> SubmitCommentResult.InvalidLength
    reason == "rate_limit" || reason == "rate_limited" -> SubmitCommentResult.RateLimited
    reason == "banned" -> SubmitCommentResult.Banned
    else -> SubmitCommentResult.Rejected(reason)
}

const val DEFAULT_FEED_PAGE_SIZE = 300
const val POST_TYPE_STAMP = "stamp"
const val POST_TYPE_QUESTION = "question"

/** 舊 UI 名稱的相容 alias；canonical 值只能由 catalog 提供。 */
val industries: List<String> = TreeHoleIndustryCatalog.values
