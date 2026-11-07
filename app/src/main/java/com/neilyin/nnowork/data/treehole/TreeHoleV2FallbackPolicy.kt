package com.neilyin.nnowork.data.treehole

internal enum class V2FallbackMode {
    READ_ONLY,
    REWARD_MUTATION,
}

/**
 * Decides whether a failed authenticated v2 request may be retried against v1.
 *
 * Reward checks consume server-side state. A network failure or a successful response that
 * cannot be decoded is ambiguous: the mutation may already have happened, so retrying v1 can
 * consume it twice. Only statuses that reject the request before the RPC can run are eligible.
 */
internal object TreeHoleV2FallbackPolicy {
    private val preExecutionRejections = setOf(400, 401, 403, 404)

    fun shouldFallback(mode: V2FallbackMode, httpStatus: Int?): Boolean = when (mode) {
        V2FallbackMode.READ_ONLY -> true
        V2FallbackMode.REWARD_MUTATION ->
            httpStatus != null && httpStatus in preExecutionRejections
    }
}
