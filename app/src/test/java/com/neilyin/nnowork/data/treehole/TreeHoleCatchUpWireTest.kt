package com.neilyin.nnowork.data.treehole

import com.neilyin.nnowork.data.JsonProvider
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

/** 追版 iOS V3.7.1 的 wire contract：留言、周目、獎勵 RPC、直連 GET 欄位、推播暫停。 */
class TreeHoleCatchUpWireTest {

    // ── 1. 留言不限字數（db/033） ──

    @Test
    fun `comments longer than 50 characters are accepted and only blank is rejected`() {
        val long = "我".repeat(51)
        val veryLong = "加班".repeat(2_500)
        assertEquals(long, TreeHoleCommentPolicy.normalizedContentOrNull(long))
        assertEquals(5_000, TreeHoleCommentPolicy.normalizedContentOrNull(veryLong)?.length)
        assertEquals("嗨", TreeHoleCommentPolicy.normalizedContentOrNull("  \n嗨\t\r\n"))
        assertNull(TreeHoleCommentPolicy.normalizedContentOrNull(""))
        assertNull(TreeHoleCommentPolicy.normalizedContentOrNull(" \n\t\r "))
    }

    @Test
    fun `server invalid_length and banned still map to domain results`() {
        assertEquals(SubmitCommentResult.InvalidLength, BasicSuccessResponse(false, "invalid_length").toSubmitCommentResult())
        assertEquals(SubmitCommentResult.Banned, BasicSuccessResponse(false, "banned").toSubmitCommentResult())
        assertEquals(SubmitCommentResult.Submitted, BasicSuccessResponse(true).toSubmitCommentResult())
    }

    @Test
    fun `long comment reaches submit_comment unchanged`() = runBlocking {
        val captured = mutableListOf<Request>()
        val content = "長".repeat(300)
        testApi(captured).submitComment(SubmitCommentRequest("p", "d", "n", "科技業", content))
        assertEquals(content, bodyMap(captured.single())["p_content"])
    }

    // ── 3. 貼文周目 job_index（db/032） ──

    @Test
    fun `post model decodes job_index from v2 and stays null for v1 payloads`() {
        val adapter = JsonProvider.adapter<TreeHolePost>()
        val v2 = adapter.fromJson("""{"id":"a","content":"x","round_number":3,"stamp_count":4,"job_index":2}""")!!
        val v1 = adapter.fromJson("""{"id":"b","content":"x","round_number":3,"stamp_count":4}""")!!
        val firstJob = adapter.fromJson("""{"id":"c","content":"x","job_index":null}""")!!
        val question = adapter.fromJson("""{"id":"d","content":"x","job_index":3,"post_type":"question"}""")!!
        assertEquals(2, v2.jobIndex)
        assertEquals(2, v2.displayRunNumber)
        assertNull(v1.jobIndex)
        assertNull(v1.displayRunNumber)
        assertNull(firstJob.displayRunNumber)
        assertNull("questions have no card progress", question.displayRunNumber)
        assertNull(adapter.fromJson("""{"id":"e","job_index":1}""")!!.displayRunNumber)
    }

    @Test
    fun `run prefix follows the iOS label rules`() {
        val zh = java.util.Locale.TRADITIONAL_CHINESE
        assertNull(TreeHoleProgressLabel.runPrefix(null, zh))
        assertNull(TreeHoleProgressLabel.runPrefix(1, zh))
        assertEquals("二周目", TreeHoleProgressLabel.runPrefix(2, zh))
        assertEquals("十一周目", TreeHoleProgressLabel.runPrefix(11, java.util.Locale.SIMPLIFIED_CHINESE))
        assertEquals("2周目", TreeHoleProgressLabel.runPrefix(2, java.util.Locale.JAPANESE))
        assertEquals("Run 2", TreeHoleProgressLabel.runPrefix(2, java.util.Locale.ENGLISH))
        assertEquals("一百零一", TreeHoleProgressLabel.chineseNumeral(101))
        assertEquals("二十", TreeHoleProgressLabel.chineseNumeral(20))
        assertEquals(
            "二周目 · 第 3 張卡 · 第 4 個印章",
            TreeHoleProgressLabel.applyingRunPrefix("第 3 張卡 · 第 4 個印章", 2, zh),
        )
        assertEquals(
            "· 二周目 · 第 3 張卡 · 第 4 個印章",
            TreeHoleProgressLabel.applyingRunPrefix("· 第 3 張卡 · 第 4 個印章", 2, zh),
        )
        assertEquals("第 3 張卡", TreeHoleProgressLabel.applyingRunPrefix("第 3 張卡", 1, zh))
    }

    @Test
    fun `first job insert body is byte-identical to the legacy body`() {
        val base = SubmitPostRequest(
            content = "今天又加班",
            deviceHash = "device",
            nickname = "N",
            industry = "科技業",
            role = "工程師",
            roundNumber = 2,
            stampCount = 3,
        )
        val legacyKeys = listOf(
            "content", "device_hash", "nickname", "industry", "role",
            "round_number", "stamp_count", "company_name", "post_type",
        )
        assertEquals(legacyKeys, buildSubmitPostBody(base).keys.toList())
        assertEquals(legacyKeys, buildSubmitPostBody(base.copy(jobIndex = 1)).keys.toList())
        assertFalse(buildSubmitPostBody(base.copy(jobIndex = 0)).containsKey("job_index"))

        val second = buildSubmitPostBody(base.copy(jobIndex = 2))
        assertEquals(2, second["job_index"])
        assertFalse(buildSubmitPostBody(base.copy(jobIndex = 2), includeJobIndex = false).containsKey("job_index"))
        assertNull(postJobIndexValue(null))
        assertEquals(11, postJobIndexValue(11))
    }

    @Test
    fun `insert keeps return minimal and puts job_index on the wire only for run two plus`() = runBlocking {
        val captured = mutableListOf<Request>()
        val api = testApi(captured)
        val request = SubmitPostRequest("內容內容", "device", "N", "科技業", null, 1, 1, null, jobIndex = 3)
        api.submitPost(buildSubmitPostBody(request))
        api.submitPost(buildSubmitPostBody(request.copy(jobIndex = null)))

        assertEquals("return=minimal", captured[0].header("Prefer"))
        assertEquals("/rest/v1/tree_hole_posts", captured[0].url.encodedPath)
        assertEquals(3.0, bodyMap(captured[0])["job_index"])
        assertFalse(bodyMap(captured[1]).containsKey("job_index"))
        // 既有行為：role / company_name 為明確 null。
        assertTrue(bodyMap(captured[1]).containsKey("role"))
    }

    @Test
    fun `feed v2 RPCs use the same parameters on their own paths`() = runBlocking {
        val captured = mutableListOf<Request>()
        val api = testApi(captured)
        val feed = FeedRequest(limit = 300, offset = 0, postType = POST_TYPE_STAMP)
        val posts = api.getFeedPostsV2(feed)
        api.getPostsByAuthorV2(PostsByAuthorRequest("0123456789abcdef0123456789abcdef", 200))

        assertEquals("/rest/v1/rpc/get_feed_posts_v2", captured[0].url.encodedPath)
        assertEquals("/rest/v1/rpc/get_posts_by_author_v2", captured[1].url.encodedPath)
        assertEquals(setOf("p_limit", "p_offset", "p_industry", "p_post_type"), bodyMap(captured[0]).keys)
        assertEquals(2, posts.single().jobIndex)
        assertEquals(listOf("Bearer anon-key"), captured[0].headers("Authorization"))
    }

    // ── 4. 金幣 delivery + ACK RPC ──

    @Test
    fun `reward delivery RPCs serialize the db 030 parameter names`() = runBlocking {
        val captured = mutableListOf<Request>()
        val api = testApi(captured)
        val capabilities = api.rewardDeliveryCapabilities("Bearer user")
        val events = api.claimRewardEvents(null, ClaimRewardEventsRequest("device", "delivery-1"))
        val ack = api.ackRewardEvents(null, AckRewardEventsRequest("device", "delivery-1", listOf("e1", "e2")))

        assertEquals(1, capabilities.version)
        assertEquals(emptyMap<String, Any>(), bodyMap(captured[0]))
        assertEquals(listOf("Bearer user"), captured[0].headers("Authorization"))
        assertEquals(mapOf("p_device_hash" to "device", "p_delivery_id" to "delivery-1"), bodyMap(captured[1]))
        assertEquals(listOf("Bearer anon-key"), captured[1].headers("Authorization"))
        assertEquals(listOf("e1", "e2"), bodyMap(captured[2])["p_event_ids"])
        assertEquals(2, events.size)
        assertEquals(listOf("6f1c4a52-1111-4f5b-9c1d-000000000001"), ack.acknowledgedIds)
        assertTrue(ack.acknowledged)
    }

    @Test
    fun `legacy reward responses keep their distinct keys and missing key is null`() {
        assertEquals(2, JsonProvider.adapter<NewApprovalsResponse>().fromJson("""{"new_approvals":2}""")?.newApprovals)
        assertEquals(1, JsonProvider.adapter<NewFeedbackRewardsResponse>().fromJson("""{"new_rewards":1}""")?.newRewards)
        assertNull(JsonProvider.adapter<NewFeedbackRewardsResponse>().fromJson("""{"new_approvals":1}""")?.newRewards)
    }

    // ── 7. 推播 token 暫停（§5.17） ──

    @Test
    fun `push token registration is disabled and never uploads or queues retries`() {
        assertFalse(PUSH_TOKEN_REGISTRATION_ENABLED)
        assertEquals(
            PushTokenRegistrationPolicy.Action.DISCARD,
            PushTokenRegistrationPolicy.decide(PUSH_TOKEN_REGISTRATION_ENABLED, termsAccepted = true),
        )
        assertEquals(
            PushTokenRegistrationPolicy.Action.DISCARD,
            PushTokenRegistrationPolicy.decide(PUSH_TOKEN_REGISTRATION_ENABLED, termsAccepted = false),
        )
        // 旗標打開時才恢復原本行為（保留程式）。
        assertEquals(PushTokenRegistrationPolicy.Action.UPLOAD, PushTokenRegistrationPolicy.decide(true, true))
        assertEquals(PushTokenRegistrationPolicy.Action.DEFER_UNTIL_TERMS, PushTokenRegistrationPolicy.decide(true, false))
    }

    // ── 8. 直連 GET 明確列欄位、不含 device_hash（§21） ──

    @Test
    fun `direct table GET pins its column list, keeps post_type and never device_hash`() = runBlocking {
        val fields = TreeHoleRepository.PUBLIC_AUTHOR_POST_FIELDS
        assertEquals(
            listOf(
                "id", "content", "industry", "nickname", "company_name", "role", "round_number", "stamp_count",
                "likes_count", "cheer_count", "cry_count", "status", "created_at", "post_type",
            ),
            fields.split(","),
        )
        val iosColumns = "id,content,industry,nickname,company_name,role,round_number,stamp_count," +
            "likes_count,cheer_count,cry_count,created_at"
        assertTrue("every iOS column is selected", fields.split(",").containsAll(iosColumns.split(",")))
        assertFalse(fields.contains("device_hash"))
        assertFalse(fields.contains("*"))
        // 提問貼文透過舊 fallback 取回時仍是提問。
        val question = JsonProvider.adapter<TreeHolePost>()
            .fromJson("""{"id":"q","content":"該忍嗎？","status":"approved","post_type":"question"}""")!!
        assertTrue(question.isQuestion)

        val captured = mutableListOf<Request>()
        testApi(captured).getPostsByNickname(fields = fields, nicknameFilter = "eq.N")
        val url = captured.single().url
        assertEquals("GET", captured.single().method)
        assertEquals(fields, url.queryParameter("select"))
        assertEquals("eq.approved", url.queryParameter("status"))
        assertEquals("created_at.desc", url.queryParameter("order"))
        assertFalse(url.toString().contains("device_hash"))
        assertEquals(setOf("select", "nickname", "status", "order", "limit"), url.queryParameterNames)
    }

    private fun bodyMap(request: Request): Map<*, *> {
        val buffer = okio.Buffer()
        request.body?.writeTo(buffer)
        return JsonProvider.moshi.adapter(Map::class.java).fromJson(buffer.readUtf8()) ?: emptyMap<String, Any>()
    }

    private fun testApi(captured: MutableList<Request>): TreeHoleApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain -> chain.proceed(chain.request().withTreeHoleHeaders("anon-key")) }
            .addInterceptor { chain ->
                val request = chain.request()
                captured += request
                val path = request.url.encodedPath
                val json = when {
                    path.endsWith("get_feed_posts_v2") -> """[{"id":"p1","content":"c","job_index":2}]"""
                    path.endsWith("get_posts_by_author_v2") -> "[]"
                    path.endsWith("reward_delivery_capabilities") -> """{"version":1}"""
                    path.endsWith("claim_reward_events") ->
                        """[{"id":"6f1c4a52-1111-4f5b-9c1d-000000000001","kind":"treeHolePostApproved","amount":5},""" +
                            """{"id":"a93e0000-2222-4f5b-9c1d-000000000002","kind":"feedbackReward","amount":10}]"""
                    path.endsWith("ack_reward_events") ->
                        """{"acknowledged":true,"acknowledged_ids":["6f1c4a52-1111-4f5b-9c1d-000000000001"]}"""
                    path.endsWith("submit_comment") -> """{"success":true}"""
                    path.endsWith("tree_hole_posts") && request.method == "GET" -> "[]"
                    else -> ""
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(if (json.isEmpty()) 201 else 200)
                    .message("OK")
                    .body(json.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://tree-hole.test/")
            .client(client)
            .addConverterFactory(treeHoleMoshiConverterFactory())
            .build()
            .create(TreeHoleApi::class.java)
    }
}
