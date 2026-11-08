package com.neilyin.nnowork.data.treehole

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TreeHoleApi {
    @POST("rest/v1/rpc/get_feed_posts")
    suspend fun getFeedPosts(@Body request: FeedRequest): List<TreeHolePost>

    @POST("rest/v1/rpc/get_posts_by_author")
    suspend fun getPostsByAuthor(@Body request: PostsByAuthorRequest): List<TreeHolePost>

    /** db/032：參數與 v1 完全相同，只多回 job_index。 */
    @POST("rest/v1/rpc/get_feed_posts_v2")
    suspend fun getFeedPostsV2(@Body request: FeedRequest): List<TreeHolePost>

    @POST("rest/v1/rpc/get_posts_by_author_v2")
    suspend fun getPostsByAuthorV2(@Body request: PostsByAuthorRequest): List<TreeHolePost>

    /**
     * 只給 db/025 之前、缺 author_key 的舊資料 fallback。db/034 起 device_hash 欄位不可讀：
     * `fields` 必須明確列欄位（見 TreeHoleRepository.PUBLIC_AUTHOR_POST_FIELDS），不可為 `*`。
     */
    @GET("rest/v1/tree_hole_posts")
    suspend fun getPostsByNickname(
        @Query("select") fields: String,
        @Query("nickname") nicknameFilter: String,
        @Query("status") statusFilter: String = "eq.approved",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 200,
    ): List<TreeHolePost>

    /** authorization 為 null 時由 interceptor 補 anon；v2 不存在退回時仍帶 user token（§11）。 */
    @POST("rest/v1/rpc/get_my_posts")
    suspend fun getMyPosts(
        @Header("Authorization") authorization: String?,
        @Body request: DeviceHashRequest,
    ): List<TreeHolePost>

    @POST("rest/v1/rpc/get_my_posts_v2")
    suspend fun getMyPostsV2(
        @Header("Authorization") authorization: String,
        @Body request: DeviceHashRequest,
    ): List<TreeHolePost>

    @POST("rest/v1/rpc/get_my_comments")
    suspend fun getMyComments(
        @Header("Authorization") authorization: String?,
        @Body request: DeviceHashRequest,
    ): List<MyTreeHoleComment>

    @POST("rest/v1/rpc/get_my_comments_v2")
    suspend fun getMyCommentsV2(
        @Header("Authorization") authorization: String,
        @Body request: DeviceHashRequest,
    ): List<MyTreeHoleComment>

    @POST("rest/v1/rpc/upsert_profile")
    suspend fun upsertProfile(@Body request: UpsertProfileRequest): Response<Unit>

    @POST("rest/v1/rpc/sync_my_nickname")
    suspend fun syncMyNickname(
        @Header("Authorization") authorization: String?,
        @Body request: DeviceHashRequest,
    ): NicknameSyncResponse

    @POST("rest/v1/rpc/submit_question")
    suspend fun submitQuestion(@Body request: SubmitQuestionRequest): SubmitQuestionResponse

    @POST("rest/v1/rpc/question_quota")
    suspend fun getQuestionQuota(@Body request: DeviceHashRequest): QuestionQuotaResponse

    @POST("rest/v1/rpc/my_industry_sync_status")
    suspend fun getIndustrySyncStatus(
        @Header("Authorization") authorization: String?,
        @Body request: DeviceHashRequest,
    ): IndustrySyncStatusResponse

    @POST("rest/v1/rpc/update_my_posts_industry")
    suspend fun updateMyPostsIndustry(
        @Header("Authorization") authorization: String?,
        @Body request: UpdatePostsIndustryRequest,
    ): UpdatePostsIndustryResponse

    @POST("rest/v1/rpc/my_answer_count")
    suspend fun getMyAnswerStats(
        @Header("Authorization") authorization: String?,
        @Body request: DeviceHashRequest,
    ): MyAnswerStatsResponse

    @POST("rest/v1/rpc/my_question_activity")
    suspend fun getMyQuestionActivity(
        @Header("Authorization") authorization: String?,
        @Body request: DeviceHashRequest,
    ): List<QuestionActivity>

    /** db/034：必須維持 return=minimal。body 由 [buildSubmitPostBody] 產生（job_index 只在周目 ≥ 2 才帶）。 */
    @Headers("Prefer: return=minimal")
    @POST("rest/v1/tree_hole_posts")
    suspend fun submitPost(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Unit>

    @POST("rest/v1/rpc/delete_my_post")
    suspend fun deleteMyPost(@Body request: DeletePostRequest): DeleteResponse

    @POST("rest/v1/rpc/delete_my_account")
    suspend fun deleteMyAccount(@Body request: DeviceHashRequest): DeleteResponse

    @POST("rest/v1/rpc/delete_my_comment")
    suspend fun deleteMyComment(@Body request: DeleteCommentRequest): Boolean

    @POST("rest/v1/rpc/get_comments")
    suspend fun getComments(@Body request: CommentsRequest): List<TreeHoleComment>

    @POST("rest/v1/rpc/mark_comment_helpful")
    suspend fun markCommentHelpful(
        @Header("Authorization") authorization: String?,
        @Body request: MarkCommentHelpfulRequest,
    ): MarkCommentHelpfulResponse

    @POST("rest/v1/rpc/submit_comment")
    suspend fun submitComment(@Body request: SubmitCommentRequest): BasicSuccessResponse

    @POST("rest/v1/rpc/react_post")
    suspend fun reactPost(@Body request: ReactPostRequest): BasicSuccessResponse

    // 舊協定（§5.15／§5.16）：只在 reward_delivery_capabilities 為 404 + PGRST202 時使用。
    @POST("rest/v1/rpc/check_new_approvals")
    suspend fun checkNewApprovals(
        @Header("Authorization") authorization: String?,
        @Body request: DeviceHashRequest,
    ): NewApprovalsResponse

    @POST("rest/v1/rpc/check_new_approvals_v2")
    suspend fun checkNewApprovalsV2(
        @Header("Authorization") authorization: String,
        @Body request: DeviceHashRequest,
    ): NewApprovalsResponse

    @POST("rest/v1/rpc/check_new_feedback_rewards")
    suspend fun checkNewFeedbackRewards(
        @Header("Authorization") authorization: String?,
        @Body request: DeviceHashRequest,
    ): NewFeedbackRewardsResponse

    @POST("rest/v1/rpc/check_new_feedback_rewards_v2")
    suspend fun checkNewFeedbackRewardsV2(
        @Header("Authorization") authorization: String,
        @Body request: DeviceHashRequest,
    ): NewFeedbackRewardsResponse

    @POST("rest/v1/rpc/submit_user_feedback")
    suspend fun submitFeedback(@Body request: SubmitFeedbackRequest): FeedbackResponse

    @POST("rest/v1/rpc/submit_report")
    suspend fun submitReport(
        @Body request: Map<String, @JvmSuppressWildcards Any>,
    ): BasicSuccessResponse

    // §18 金幣獎勵 delivery + ACK（db/030）。已登入帶 user token，未登入 null → anon。
    @POST("rest/v1/rpc/reward_delivery_capabilities")
    suspend fun rewardDeliveryCapabilities(
        @Header("Authorization") authorization: String?,
        @Body body: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
    ): RewardCapabilitiesResponse

    @POST("rest/v1/rpc/claim_reward_events")
    suspend fun claimRewardEvents(
        @Header("Authorization") authorization: String?,
        @Body request: ClaimRewardEventsRequest,
    ): List<RewardEventResponse>

    @POST("rest/v1/rpc/ack_reward_events")
    suspend fun ackRewardEvents(
        @Header("Authorization") authorization: String?,
        @Body request: AckRewardEventsRequest,
    ): AckRewardEventsResponse

    // §19 換工作封存（db/031）：只授權給 authenticated，一定要帶 user token。
    @POST("rest/v1/rpc/archive_my_job")
    suspend fun archiveMyJob(
        @Header("Authorization") authorization: String,
        @Body body: RequestBody,
    ): ArchiveMyJobResponse

    @POST("rest/v1/rpc/get_my_job_archives")
    suspend fun getMyJobArchives(
        @Header("Authorization") authorization: String,
        @Body body: RequestBody,
    ): ResponseBody

    /**
     * ⛔ §5.17：db/034 刪除了 device_push_tokens 的 policy，直接 POST 會被拒。
     * 保留定義備查；呼叫端以 [PUSH_TOKEN_REGISTRATION_ENABLED] 關閉。
     */
    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    @POST("rest/v1/device_push_tokens")
    suspend fun registerDeviceToken(
        @Query("on_conflict") onConflict: String = "device_hash",
        @Body request: DevicePushTokenRequest,
    ): Response<Unit>
}

/**
 * Tree Hole RPCs distinguish an omitted argument from an explicit SQL null. In particular,
 * clearing a company name must put `p_company_name: null` on the wire instead of omitting it.
 */
internal fun treeHoleMoshiConverterFactory(): MoshiConverterFactory =
    MoshiConverterFactory.create(com.neilyin.nnowork.data.JsonProvider.moshi)
        .withNullSerialization()
