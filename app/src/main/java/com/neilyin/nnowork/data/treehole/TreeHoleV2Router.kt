package com.neilyin.nnowork.data.treehole

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap

/** v1/v2 RPC 分流的執行面；判定規則在 [TreeHoleV2FallbackPolicy]。 */
internal class TreeHoleV2Router(private val tokenCaller: UserTokenCaller) {
    /** 公開 feed v2 已確認不存在（404 + PGRST202）的 RPC，本輪 App 生命週期不再試。 */
    private val unavailablePublicRpcs: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun isPublicV2Unavailable(name: String): Boolean = name in unavailablePublicRpcs

    /**
     * 「我的資料」／舊獎勵協定（§11）：未登入直接 v1（anon）；已登入先打 v2（401 refresh 一次），
     * 只有 404 + PGRST202 才改打 v1（仍帶 user token）。其他錯誤（400/401/403/5xx/網路/解碼）照實拋出。
     */
    suspend fun <T> userScoped(
        mode: V2FallbackMode,
        v2: suspend (authorization: String) -> T,
        v1: suspend (authorization: String?) -> T,
    ): T {
        if (!tokenCaller.isSignedIn) return v1(null)
        return try {
            tokenCaller.call { authorization -> if (authorization == null) v1(null) else v2(authorization) }
        } catch (error: HttpException) {
            val code = error.postgrestError()?.code
            if (!TreeHoleV2FallbackPolicy.shouldFallback(mode, error.code(), code)) throw error
            tokenCaller.call { authorization -> v1(authorization) }
        }
    }

    /**
     * 公開 feed（§13.9 例外、§20.2）：唯讀無副作用，v2 任何失敗都改打 v1（最壞只是不顯示周目），
     * 但只有 404 + PGRST202 才記住本輪不再試 v2。
     */
    suspend fun <T> publicFeed(name: String, v2: suspend () -> T, v1: suspend () -> T): T {
        if (name !in unavailablePublicRpcs) {
            try {
                return v2()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val http = error as? HttpException
                val code = http?.postgrestError()?.code
                if (TreeHoleV2FallbackPolicy.shouldRememberV2Unavailable(http?.code(), code)) {
                    unavailablePublicRpcs += name
                }
            }
        }
        return v1()
    }
}
