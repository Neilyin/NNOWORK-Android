package com.neilyin.nnowork.data.treehole

import com.neilyin.nnowork.data.JsonProvider
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class FeedbackDeviceSourceTest {
    @Test
    fun `Android source is appended after trimmed user content`() {
        assertEquals(
            "希望可以增加深色模式\n\n來自於android裝置",
            "  希望可以增加深色模式  ".withAndroidDeviceSource(),
        )
    }

    @Test
    fun `existing Android source is not duplicated`() {
        val alreadyTagged = "測試回饋\n\n來自於android裝置"

        assertEquals(alreadyTagged, alreadyTagged.withAndroidDeviceSource())
    }

    @Test
    fun `Retrofit sends Android source in final p_content JSON field`() = runBlocking {
        var sentBody: String? = null
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val buffer = okio.Buffer()
                request.body?.writeTo(buffer)
                sentBody = buffer.readUtf8()

                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """{"success":true}"""
                            .toResponseBody("application/json".toMediaType()),
                    )
                    .build()
            }
            .build()
        val api = Retrofit.Builder()
            .baseUrl("https://feedback.test/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(JsonProvider.moshi))
            .build()
            .create(TreeHoleApi::class.java)

        api.submitFeedback(
            buildSubmitFeedbackRequest(
                deviceHash = "android-device",
                content = "希望可以增加深色模式",
            ),
        )

        val body = assertNotNull(sentBody).let { sentBody!! }
        val decoded = JsonProvider.moshi.adapter(Map::class.java).fromJson(body)
        assertEquals("android-device", decoded?.get("p_device_hash"))
        assertEquals(
            "希望可以增加深色模式\n\n來自於android裝置",
            decoded?.get("p_content"),
        )
    }
}
