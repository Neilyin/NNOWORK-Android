package com.neilyin.nnowork.data.treehole

import retrofit2.HttpException

/**
 * 帶 user token 的 RPC 呼叫（android開發.md §11）：已登入 → `Authorization: Bearer <user token>`；
 * HTTP 401 → refresh token 一次後重打；未登入 → 交給 interceptor 補 anon key。
 */
internal class UserTokenCaller(
    private val accessToken: () -> String?,
    /** 參數是剛被拒的 token；回傳 true 代表已有可用的新 token。 */
    private val refreshAccessToken: suspend (staleToken: String) -> Boolean,
) {
    fun currentToken(): String? = accessToken()?.trim()?.takeIf(String::isNotEmpty)

    val isSignedIn: Boolean get() = currentToken() != null

    /**
     * 固定在「此刻」的 token 與身分。之後若 live session 換成另一個帳號（登出／切帳號），
     * 這個 caller 仍用原本的 token；401 refresh 後若新 token 已屬於別的帳號，視為 refresh 失敗，
     * 讓這次操作照實報錯，而不是用別人的身分繼續。未登入時固定為 anon。
     */
    fun pinned(subjectOf: (String) -> String?): UserTokenCaller {
        val initial = currentToken()
        val initialSubject = initial?.let(subjectOf)
        val token = java.util.concurrent.atomic.AtomicReference(initial)
        return UserTokenCaller(accessToken = { token.get() }) { stale ->
            if (!refreshAccessToken(stale)) return@UserTokenCaller false
            val live = currentToken() ?: return@UserTokenCaller false
            if (subjectOf(live) != initialSubject) return@UserTokenCaller false
            token.set(live)
            true
        }
    }

    /** 已登入帶 user token、未登入帶 null（anon）。 */
    suspend fun <T> call(block: suspend (authorization: String?) -> T): T {
        val token = currentToken() ?: return block(null)
        return callWithToken(token, block)
    }

    /** 只能帶 user token 的 RPC（archive_my_job / get_my_job_archives）；未登入回 null。 */
    suspend fun <T> callSignedIn(block: suspend (authorization: String) -> T): T? {
        val token = currentToken() ?: return null
        return callWithToken(token) { authorization -> block(requireNotNull(authorization)) }
    }

    private suspend fun <T> callWithToken(token: String, block: suspend (authorization: String?) -> T): T =
        try {
            block("Bearer $token")
        } catch (error: HttpException) {
            if (error.code() != 401 || !refreshAccessToken(token)) throw error
            val refreshed = currentToken() ?: throw error
            block("Bearer $refreshed")
        }
}
