package com.neilyin.nnowork.data.treehole

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test

class TreeHoleRequestHeadersTest {
    @Test
    fun `anonymous request receives anon authorization`() {
        val request = Request.Builder()
            .url("https://example.test/")
            .build()
            .withTreeHoleHeaders("anon")

        assertEquals(listOf("Bearer anon"), request.headers("Authorization"))
        assertEquals("anon", request.header("apikey"))
    }

    @Test
    fun `v2 request keeps exactly one user authorization`() {
        val request = Request.Builder()
            .url("https://example.test/")
            .header("Authorization", "Bearer user-token")
            .build()
            .withTreeHoleHeaders("anon")

        assertEquals(listOf("Bearer user-token"), request.headers("Authorization"))
        assertEquals("anon", request.header("apikey"))
    }
}
