package com.neilyin.nnowork.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OAuthCallbackParserTest {
    @Test
    fun `parses tokens regardless of fragment order`() {
        assertEquals(
            OAuthTokens("access-token", "refresh token"),
            parseOAuthFragment("expires_in=3600&refresh_token=refresh%20token&access_token=access-token"),
        )
    }

    @Test
    fun `rejects incomplete callback`() {
        assertNull(parseOAuthFragment("access_token=only-one-token"))
        assertNull(parseOAuthFragment("error_description=cancelled"))
    }
}
