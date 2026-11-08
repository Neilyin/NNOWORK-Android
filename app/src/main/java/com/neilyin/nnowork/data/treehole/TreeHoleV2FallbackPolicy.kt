package com.neilyin.nnowork.data.treehole

internal enum class V2FallbackMode {
    /** 「我的資料」類 v2（get_my_posts_v2 / get_my_comments_v2），帶 user token。 */
    USER_SCOPED_READ,

    /** 會消耗伺服器狀態的獎勵 RPC（check_new_approvals_v2 等舊協定）。 */
    REWARD_MUTATION,

    /** 公開、唯讀、無副作用的 feed v2（get_feed_posts_v2 / get_posts_by_author_v2）。 */
    PUBLIC_FEED_READ,
}

/**
 * 決定失敗的 v2 請求能不能改打 v1（android開發.md §11、§13.9）。
 *
 * - 身分相關（USER_SCOPED_READ／REWARD_MUTATION）：**只有** HTTP 404 且 body code 為
 *   PGRST202（函式不存在）才退 v1。400/401/403/5xx/網路/解碼失敗一律照實報錯：
 *   已登入者被默默縮回單機資料看起來像資料不見；獎勵 RPC 可能已在伺服器提交。
 * - 公開 feed：唯讀無副作用，任何失敗都改打 v1（最壞只是不顯示周目），
 *   但只有 404 + PGRST202 才記住「這輪 App 生命週期不再試 v2」。
 */
internal object TreeHoleV2FallbackPolicy {
    fun shouldFallback(mode: V2FallbackMode, httpStatus: Int?, errorCode: String?): Boolean = when (mode) {
        V2FallbackMode.USER_SCOPED_READ,
        V2FallbackMode.REWARD_MUTATION -> isMissingFunction(httpStatus, errorCode)
        V2FallbackMode.PUBLIC_FEED_READ -> true
    }

    /** 公開 feed：是否在本次 App 生命週期記住 v2 不存在。 */
    fun shouldRememberV2Unavailable(httpStatus: Int?, errorCode: String?): Boolean =
        isMissingFunction(httpStatus, errorCode)

    fun isMissingFunction(httpStatus: Int?, errorCode: String?): Boolean =
        httpStatus == 404 && errorCode == PostgrestError.MISSING_FUNCTION
}
