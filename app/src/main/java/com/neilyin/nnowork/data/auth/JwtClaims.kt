package com.neilyin.nnowork.data.auth

import com.neilyin.nnowork.data.JsonProvider
import java.util.Base64
import java.util.Locale

/** 只讀取 JWT payload 的 `sub`（不驗簽；token 來自自己的 session）。 */
internal object JwtClaims {
    private val adapter = JsonProvider.moshi.adapter(Map::class.java)

    fun subject(token: String): String? {
        val parts = token.trim().split('.')
        if (parts.size < 2) return null
        return runCatching {
            val json = String(Base64.getUrlDecoder().decode(parts[1].trimEnd('=')), Charsets.UTF_8)
            (adapter.fromJson(json)?.get("sub") as? String)
                ?.trim()
                ?.lowercase(Locale.ROOT)
                ?.takeIf(String::isNotEmpty)
        }.getOrNull()
    }
}
