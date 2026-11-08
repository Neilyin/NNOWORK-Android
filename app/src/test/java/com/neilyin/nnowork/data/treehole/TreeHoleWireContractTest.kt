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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class TreeHoleWireContractTest {
    @Test
    fun `profile upsert serializes an explicit null company through Retrofit`() = runBlocking {
        val captured = mutableListOf<Request>()
        val api = testApi(captured)

        api.upsertProfile(
            UpsertProfileRequest(
                deviceHash = "device",
                nickname = "nickname",
                industry = "科技業",
                companyName = null,
                showCompany = false,
                role = "engineer",
            ),
        )

        val request = captured.single()
        val buffer = okio.Buffer()
        request.body?.writeTo(buffer)
        val decoded = JsonProvider.moshi.adapter(Map::class.java).fromJson(buffer.readUtf8())

        assertTrue(decoded?.containsKey("p_company_name") == true)
        assertNull(decoded?.get("p_company_name"))
        assertEquals(listOf("Bearer anon-key"), request.headers("Authorization"))
    }

    @Test
    fun `signed in industry RPCs send exactly one user bearer token`() = runBlocking {
        val captured = mutableListOf<Request>()
        val api = testApi(captured)
        val authorization = treeHoleAuthorizationHeader("  user-jwt  ")

        callIndustryRpcs(api, authorization)

        assertEquals(6, captured.size)
        captured.forEach { request ->
            assertEquals(listOf("Bearer user-jwt"), request.headers("Authorization"))
            assertEquals("anon-key", request.header("apikey"))
        }
    }

    @Test
    fun `anonymous industry RPCs receive exactly one anon bearer token`() = runBlocking {
        val captured = mutableListOf<Request>()
        val api = testApi(captured)

        callIndustryRpcs(api, treeHoleAuthorizationHeader("  "))

        assertEquals(6, captured.size)
        captured.forEach { request ->
            assertEquals(listOf("Bearer anon-key"), request.headers("Authorization"))
            assertEquals("anon-key", request.header("apikey"))
        }
    }

    @Test
    fun `author lookup sends only the public author key`() = runBlocking {
        val captured = mutableListOf<Request>()
        val api = testApi(captured)

        api.getPostsByAuthor(
            PostsByAuthorRequest(
                authorKey = "0123456789abcdef0123456789abcdef",
                limit = 200,
            ),
        )

        val request = captured.single()
        val buffer = okio.Buffer()
        request.body?.writeTo(buffer)
        val body = buffer.readUtf8()
        assertTrue(body.contains("p_author_key"))
        assertTrue(!body.contains("device_hash"))
        assertEquals(listOf("Bearer anon-key"), request.headers("Authorization"))
    }

    @Test
    fun `report rpc uses canonical path and omits an empty note`() = runBlocking {
        val captured = mutableListOf<Request>()
        val api = testApi(captured)

        api.submitReport(
            buildSubmitReportBody(
                targetType = TreeHoleReportTargetType.POST,
                targetId = "post-id",
                deviceHash = "d".repeat(64),
                reason = TreeHoleReportReason.SPAM,
                note = "",
            ),
        )

        val request = captured.single()
        val buffer = okio.Buffer()
        request.body?.writeTo(buffer)
        val decoded = JsonProvider.moshi.adapter(Map::class.java).fromJson(buffer.readUtf8())
        assertEquals("/rest/v1/rpc/submit_report", request.url.encodedPath)
        assertEquals("post", decoded?.get("p_target_type"))
        assertEquals("spam", decoded?.get("p_reason"))
        assertTrue(decoded?.containsKey("p_note") == false)
    }

    private suspend fun callIndustryRpcs(api: TreeHoleApi, authorization: String?) {
        val device = DeviceHashRequest("device")
        api.getIndustrySyncStatus(authorization, device)
        api.updateMyPostsIndustry(
            authorization,
            UpdatePostsIndustryRequest(deviceHash = "device", includeMismatched = false),
        )
        api.getMyAnswerStats(authorization, device)
        api.getMyQuestionActivity(authorization, device)
        api.markCommentHelpful(
            authorization,
            MarkCommentHelpfulRequest(
                commentId = "comment",
                deviceHash = "device",
                helpful = true,
            ),
        )
        api.syncMyNickname(authorization, device)
    }

    private fun testApi(captured: MutableList<Request>): TreeHoleApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().withTreeHoleHeaders("anon-key"))
            }
            .addInterceptor { chain ->
                val request = chain.request()
                captured += request
                val json = when {
                    request.url.encodedPath.endsWith("my_industry_sync_status") ->
                        """{"eligible":false,"breakdown":[]}"""
                    request.url.encodedPath.endsWith("update_my_posts_industry") ->
                        """{"success":true,"updated":0}"""
                    request.url.encodedPath.endsWith("my_answer_count") ->
                        """{"answer_count":0,"helpful_count":0,"exp":0}"""
                    request.url.encodedPath.endsWith("my_question_activity") -> "[]"
                    request.url.encodedPath.endsWith("mark_comment_helpful") ->
                        """{"success":true,"is_helpful":true}"""
                    request.url.encodedPath.endsWith("sync_my_nickname") ->
                        """{"success":true,"nickname":"N","posts_updated":1,"comments_updated":2}"""
                    request.url.encodedPath.endsWith("get_posts_by_author") -> "[]"
                    request.url.encodedPath.endsWith("submit_report") ->
                        """{"success":true}"""
                    else -> "{}"
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
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
