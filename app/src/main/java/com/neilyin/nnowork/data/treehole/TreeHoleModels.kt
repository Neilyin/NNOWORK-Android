package com.neilyin.nnowork.data.treehole

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class TreeHoleProfile(
    val nickname: String = "",
    val industry: String = "",
    @Json(name = "company_name") val companyName: String = "",
    @Json(name = "show_company") val showCompany: Boolean = false,
) {
    val isComplete: Boolean get() = nickname.isNotBlank() && industry.isNotBlank()
}

@JsonClass(generateAdapter = false)
data class TreeHolePost(
    val id: String = "",
    val content: String = "",
    val industry: String? = null,
    val nickname: String? = null,
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
)

@JsonClass(generateAdapter = false)
data class TreeHoleComment(
    val id: String = "",
    val nickname: String = "",
    val industry: String? = null,
    val content: String = "",
    @Json(name = "created_at") val createdAt: String = "",
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
    @Json(name = "p_limit") val limit: Int = 60,
    @Json(name = "p_industry") val industry: String? = null,
)

@JsonClass(generateAdapter = false)
data class DeviceHashRequest(
    @Json(name = "p_device_hash") val deviceHash: String,
)

@JsonClass(generateAdapter = false)
data class AddCoinsRequest(
    @Json(name = "p_device_hash") val deviceHash: String,
    @Json(name = "p_amount") val amount: Int,
)

@JsonClass(generateAdapter = false)
data class NewApprovalsResponse(
    @Json(name = "new_approvals") val newApprovals: Int = 0,
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

@JsonClass(generateAdapter = false)
data class SubmitPostRequest(
    val content: String,
    @Json(name = "device_hash") val deviceHash: String,
    val nickname: String,
    val industry: String,
    @Json(name = "round_number") val roundNumber: Int,
    @Json(name = "stamp_count") val stampCount: Int,
    @Json(name = "company_name") val companyName: String? = null,
)

@JsonClass(generateAdapter = false)
data class DeleteResponse(val success: Boolean = false)

@JsonClass(generateAdapter = false)
data class BasicSuccessResponse(
    val success: Boolean = false,
    val reason: String? = null,
    val action: String? = null,
)

val industries = listOf(
    "科技業",
    "金融業",
    "服務業",
    "教育業",
    "醫療業",
    "設計業",
    "媒體業",
    "公職",
    "自由業",
    "其他",
)
