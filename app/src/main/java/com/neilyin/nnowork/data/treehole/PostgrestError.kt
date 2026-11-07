package com.neilyin.nnowork.data.treehole

import com.neilyin.nnowork.data.JsonProvider
import com.squareup.moshi.JsonClass
import retrofit2.HttpException

/** PostgREST 錯誤 body：`{"code":"PGRST202","message":…,"details":…,"hint":…}`。 */
@JsonClass(generateAdapter = false)
data class PostgrestError(
    val code: String? = null,
    val message: String? = null,
    val details: String? = null,
    val hint: String? = null,
) {
    companion object {
        /** PostgREST：schema cache 找不到函式（RPC 未部署）。 */
        const val MISSING_FUNCTION = "PGRST202"
        /** PostgREST：schema cache 找不到欄位。 */
        const val MISSING_COLUMN = "PGRST204"
        /** Postgres undefined_column。 */
        const val UNDEFINED_COLUMN = "42703"

        private val adapter by lazy { JsonProvider.adapter<PostgrestError>() }

        fun parse(body: String?): PostgrestError? {
            if (body.isNullOrBlank()) return null
            return runCatching { adapter.fromJson(body) }.getOrNull()
        }
    }
}

/**
 * §13.9：「新 RPC 不存在」唯一可靠的判斷是 HTTP 404 且 body.code == PGRST202。
 * 逾時、5xx、401/403、解碼失敗都不算。
 */
internal fun isMissingRpc(httpStatus: Int?, error: PostgrestError?): Boolean =
    httpStatus == 404 && error?.code == PostgrestError.MISSING_FUNCTION

/**
 * §20.3：送文帶 job_index 被拒是否因為後端還沒有這個欄位。只認 4xx + PGRST204/42703，
 * 且 message／details／hint 明確提到 job_index；停權 403、constraint、網路一律不算。
 */
internal fun isMissingJobIndexColumn(httpStatus: Int, error: PostgrestError?): Boolean {
    if (httpStatus !in 400..499 || error == null) return false
    if (error.code != PostgrestError.MISSING_COLUMN && error.code != PostgrestError.UNDEFINED_COLUMN) return false
    return listOf(error.message, error.details, error.hint).any { it?.contains("job_index") == true }
}

/** 讀出 Retrofit HttpException 的 PostgREST 錯誤 body（只能讀一次，呼叫端自行保存）。 */
internal fun HttpException.postgrestError(): PostgrestError? =
    PostgrestError.parse(runCatching { response()?.errorBody()?.string() }.getOrNull())

/** 後端明確回報 RPC 不存在（404 + PGRST202）。 */
class MissingRpcException(val rpcName: String) : IllegalStateException("RPC $rpcName is not deployed")
