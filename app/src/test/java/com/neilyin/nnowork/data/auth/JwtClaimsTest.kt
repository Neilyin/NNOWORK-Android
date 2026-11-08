package com.neilyin.nnowork.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtClaimsTest {
    private fun jwt(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return listOf("""{"alg":"HS256"}""", payload, "sig")
            .joinToString(".") { encoder.encodeToString(it.toByteArray()) }
    }

    @Test
    fun `subject is read from an unpadded url-safe payload and lowercased`() {
        assertEquals(
            "9a1e0c4d-1111-4222-8333-444455556666",
            JwtClaims.subject(jwt("""{"sub":"9A1E0C4D-1111-4222-8333-444455556666","role":"authenticated"}""")),
        )
    }

    @Test
    fun `malformed tokens yield null`() {
        assertNull(JwtClaims.subject("not-a-jwt"))
        assertNull(JwtClaims.subject(jwt("""{"role":"anon"}""")))
        assertNull(JwtClaims.subject("a.%%%.c"))
    }
}
