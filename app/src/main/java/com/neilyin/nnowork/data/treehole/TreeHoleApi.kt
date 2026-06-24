package com.neilyin.nnowork.data.treehole

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface TreeHoleApi {
    @POST("rest/v1/rpc/get_feed_posts")
    suspend fun getFeedPosts(@Body request: FeedRequest): List<TreeHolePost>

    @POST("rest/v1/rpc/get_my_posts")
    suspend fun getMyPosts(@Body request: DeviceHashRequest): List<TreeHolePost>

    @POST("rest/v1/rpc/get_my_comments")
    suspend fun getMyComments(@Body request: DeviceHashRequest): List<MyTreeHoleComment>

    @Headers("Prefer: return=minimal")
    @POST("rest/v1/tree_hole_posts")
    suspend fun submitPost(@Body request: SubmitPostRequest): Response<Unit>

    @POST("rest/v1/rpc/delete_my_post")
    suspend fun deleteMyPost(@Body request: DeletePostRequest): DeleteResponse

    @POST("rest/v1/rpc/delete_my_comment")
    suspend fun deleteMyComment(@Body request: DeleteCommentRequest): Boolean

    @POST("rest/v1/rpc/get_comments")
    suspend fun getComments(@Body request: CommentsRequest): List<TreeHoleComment>

    @POST("rest/v1/rpc/submit_comment")
    suspend fun submitComment(@Body request: SubmitCommentRequest): BasicSuccessResponse

    @POST("rest/v1/rpc/react_post")
    suspend fun reactPost(@Body request: ReactPostRequest): BasicSuccessResponse

    @POST("rest/v1/rpc/check_new_approvals")
    suspend fun checkNewApprovals(@Body request: DeviceHashRequest): NewApprovalsResponse

    @POST("rest/v1/rpc/get_coin_balance")
    suspend fun getCoinBalance(@Body request: DeviceHashRequest): Int

    @POST("rest/v1/rpc/add_coins")
    suspend fun addCoins(@Body request: AddCoinsRequest): Response<Unit>
}
