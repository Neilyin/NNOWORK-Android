package com.neilyin.nnowork.data.auth

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.neilyin.nnowork.analytics.Analytics
import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.datastore.appDataStore
import com.neilyin.nnowork.data.identity.DeviceIdentityRepository
import com.neilyin.nnowork.data.remote.SUPABASE_ANON_KEY
import com.neilyin.nnowork.data.remote.SUPABASE_URL
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder

data class AuthUiState(
    val isSignedIn: Boolean = false,
    val email: String? = null,
    val linkedProviders: List<String> = emptyList(),
    val lastProvider: String? = null,
    val isProcessing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class AuthRepository(
    private val context: Context,
    private val deviceIdentityRepository: DeviceIdentityRepository,
) {
    private val store = AuthSessionStore(context)
    private val client = OkHttpClient()
    private val operationMutex = Mutex()
    private val sessionAdapter = JsonProvider.adapter<AuthSessionResponse>()
    private val userAdapter = JsonProvider.adapter<AuthUserResponse>()
    private val errorAdapter = JsonProvider.adapter<AuthErrorResponse>()
    @Volatile
    private var session: StoredAuthSession? = store.read()
    private val _state = MutableStateFlow(
        AuthUiState(isSignedIn = session != null, email = session?.email),
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    val currentAccessToken: String? get() = session?.accessToken

    /** Supabase auth user id（JWT `sub`，小寫）；未登入為 null。獎勵 journal 與過往工作以此分帳號。 */
    val currentUserId: String? get() = session?.accessToken?.let(JwtClaims::subject)

    private val refreshMutex = Mutex()

    /**
     * RPC 回 401 時 refresh 一次（android開發.md §11）。並發的 401 只會 refresh 一次：
     * 若 token 已被別人換掉，直接回 true 讓呼叫端用新 token 重打。refresh 失敗不登出。
     */
    suspend fun refreshAccessToken(staleToken: String): Boolean = refreshMutex.withLock {
        val current = session?.accessToken ?: return@withLock false
        if (current != staleToken) return@withLock true
        refreshSession()
    }

    val googleOAuthUri: Uri
        get() = Uri.parse("${SUPABASE_URL}auth/v1/authorize")
            .buildUpon()
            .appendQueryParameter("provider", "google")
            .appendQueryParameter("redirect_to", REDIRECT_URI)
            .build()

    suspend fun initialize() {
        val lastProvider = context.appDataStore.data.first()[LAST_PROVIDER_KEY]
        _state.value = _state.value.copy(lastProvider = lastProvider)
        if (session != null) {
            if (refreshSession()) refreshUser() else clearSession()
        }
    }

    suspend fun signUp(email: String, password: String) = operationMutex.withLock {
        process {
            validate(email, password)
            val response = postAuth("auth/v1/signup", mapOf("email" to email.trim(), "password" to password))
            val auth = decodeSession(response.body)
            Analytics.log("account_signup_succeeded")
            if (auth.accessToken != null && auth.refreshToken != null) {
                finishSignIn(auth, email.trim(), "email")
                "註冊完成，已登入並認領這台裝置的資料。"
            } else {
                "註冊成功，請到信箱完成驗證後再登入。"
            }
        }
    }

    suspend fun signIn(email: String, password: String) = operationMutex.withLock {
        process {
            validate(email, password)
            val response = postAuth(
                "auth/v1/token?grant_type=password",
                mapOf("email" to email.trim(), "password" to password),
            )
            finishSignIn(decodeSession(response.body), email.trim(), "email")
            Analytics.log("account_signin_succeeded", "provider" to "email")
            "登入成功，這台裝置的樹洞資料已完成認領。"
        }
    }

    suspend fun handleOAuthCallback(uri: Uri?): Boolean {
        if (uri?.scheme != REDIRECT_SCHEME || uri.host != REDIRECT_HOST) return false
        return operationMutex.withLock {
            process {
                val tokens = parseOAuthFragment(uri.fragment.orEmpty())
                    ?: error("Google 登入沒有回傳完整 session。")
                finishSignIn(AuthSessionResponse(tokens.accessToken, tokens.refreshToken, null), null, "google")
                Analytics.log("account_signin_succeeded", "provider" to "google")
                "Google 登入成功，這台裝置的資料已完成認領。"
            }
            true
        }
    }

    suspend fun signOut() = operationMutex.withLock {
        val token = session?.accessToken
        if (token != null) runCatching { request("auth/v1/logout", "POST", "{}", token) }
        clearSession("已登出，App 會繼續使用匿名裝置身份。")
    }

    suspend fun deleteAccount() = operationMutex.withLock {
        process {
            callAuthedRpc("delete_my_auth_account", emptyMap())
            clearSession()
            Analytics.log("account_deleted")
            "帳號已刪除，本機集點資料仍保留。"
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, isError = false)
    }

    /** Re-links all device-scoped Tree Hole data after a signed-in profile upsert. */
    suspend fun claimDeviceData(): Boolean {
        if (currentAccessToken.isNullOrBlank()) return false
        return try {
            callAuthedRpc(
                "claim_device_data",
                mapOf("p_device_hash" to deviceIdentityRepository.deviceHash()),
            )
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun finishSignIn(
        response: AuthSessionResponse,
        fallbackEmail: String?,
        provider: String,
    ) {
        val access = response.accessToken ?: error("登入回應缺少 access token。")
        val refresh = response.refreshToken ?: error("登入回應缺少 refresh token。")
        session = StoredAuthSession(access, refresh, response.user?.email ?: fallbackEmail)
        store.write(checkNotNull(session))
        context.appDataStore.edit { it[LAST_PROVIDER_KEY] = provider }
        _state.value = _state.value.copy(
            isSignedIn = true,
            email = session?.email,
            lastProvider = provider,
            isError = false,
        )
        // 登入完成時先認領一次；之後 profile upsert 也會透過公開方法重試。
        claimDeviceData()
        refreshUser()
    }

    private suspend fun refreshSession(): Boolean = withContext(Dispatchers.IO) {
        val current = session ?: return@withContext false
        runCatching {
            val response = postAuth(
                "auth/v1/token?grant_type=refresh_token",
                mapOf("refresh_token" to current.refreshToken),
            )
            val refreshed = decodeSession(response.body)
            val access = requireNotNull(refreshed.accessToken)
            val refresh = requireNotNull(refreshed.refreshToken)
            session = StoredAuthSession(access, refresh, refreshed.user?.email ?: current.email)
            store.write(checkNotNull(session))
            _state.value = _state.value.copy(isSignedIn = true, email = session?.email)
        }.isSuccess
    }

    private suspend fun refreshUser() {
        val token = session?.accessToken ?: return
        runCatching {
            val response = request("auth/v1/user", "GET", null, token)
            val user = userAdapter.fromJson(response.body) ?: return@runCatching
            session = session?.copy(email = user.email ?: session?.email)
            session?.let(store::write)
            _state.value = _state.value.copy(
                isSignedIn = true,
                email = session?.email,
                linkedProviders = user.identities.orEmpty().mapNotNull { it.provider }.distinct(),
            )
        }
    }

    private suspend fun callAuthedRpc(name: String, body: Map<String, Any?>): HttpResult {
        var token = session?.accessToken ?: error("尚未登入。")
        var result = request("rest/v1/rpc/$name", "POST", JsonProvider.moshi.adapter(Map::class.java).toJson(body), token)
        if (result.code == 401 && refreshSession()) {
            token = session?.accessToken ?: error("登入已失效。")
            result = request("rest/v1/rpc/$name", "POST", JsonProvider.moshi.adapter(Map::class.java).toJson(body), token)
        }
        if (result.code !in 200..299) error(humanizedError(result))
        return result
    }

    private suspend fun postAuth(path: String, fields: Map<String, String>): HttpResult {
        val json = JsonProvider.moshi.adapter(Map::class.java).toJson(fields)
        val result = request(path, "POST", json, null)
        if (result.code !in 200..299) error(humanizedError(result))
        return result
    }

    private suspend fun request(path: String, method: String, json: String?, userToken: String?): HttpResult =
        withContext(Dispatchers.IO) {
            val body = json?.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$SUPABASE_URL$path")
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${userToken ?: SUPABASE_ANON_KEY}")
                .apply { if (method == "GET") get() else post(body ?: "{}".toRequestBody(JSON_MEDIA_TYPE)) }
                .build()
            client.newCall(request).execute().use { HttpResult(it.code, it.body?.string().orEmpty()) }
        }

    private fun decodeSession(body: String): AuthSessionResponse =
        sessionAdapter.fromJson(body) ?: error("無法解析登入回應。")

    private suspend fun process(block: suspend () -> String) {
        _state.value = _state.value.copy(isProcessing = true, message = null, isError = false)
        runCatching { block() }
            .onSuccess { _state.value = _state.value.copy(isProcessing = false, message = it, isError = false) }
            .onFailure { _state.value = _state.value.copy(isProcessing = false, message = it.message ?: "網路連線失敗，請稍後再試。", isError = true) }
    }

    private fun validate(email: String, password: String) {
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { "Email 格式不正確。" }
        require(password.length >= 6) { "密碼至少需要 6 個字元。" }
    }

    private fun humanizedError(result: HttpResult): String {
        val error = runCatching { errorAdapter.fromJson(result.body) }.getOrNull()
        val raw = listOfNotNull(error?.message, error?.description, error?.error).joinToString(" ").lowercase()
        return when {
            "already" in raw || "user_repeated_signup" in raw -> "這個 Email 已經註冊過了，請直接登入。"
            "invalid login" in raw || "invalid_grant" in raw -> "Email 或密碼不正確。"
            "password" in raw -> "密碼至少需要 6 個字元。"
            "email" in raw && "invalid" in raw -> "Email 格式不正確。"
            result.code == 401 -> "登入已失效，請重新登入。"
            else -> "連線失敗（${result.code}），請稍後再試。"
        }
    }

    private fun clearSession(message: String? = null) {
        session = null
        store.clear()
        _state.value = AuthUiState(lastProvider = _state.value.lastProvider, message = message)
    }

    private data class HttpResult(val code: Int, val body: String)

    companion object {
        const val REDIRECT_SCHEME = "tw.neil.nnowork"
        const val REDIRECT_HOST = "auth-callback"
        const val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST"
        private val LAST_PROVIDER_KEY = stringPreferencesKey("last_auth_provider")
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

@JsonClass(generateAdapter = false)
private data class AuthSessionResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    val user: AuthUserResponse? = null,
)

@JsonClass(generateAdapter = false)
private data class AuthUserResponse(
    val email: String? = null,
    val identities: List<AuthIdentity>? = null,
)

@JsonClass(generateAdapter = false)
private data class AuthIdentity(val provider: String? = null)

@JsonClass(generateAdapter = false)
private data class AuthErrorResponse(
    val error: String? = null,
    @Json(name = "error_description") val description: String? = null,
    @Json(name = "msg") val message: String? = null,
)

internal data class OAuthTokens(val accessToken: String, val refreshToken: String)

internal fun parseOAuthFragment(fragment: String): OAuthTokens? {
    val values = fragment.split('&')
        .mapNotNull { item ->
            val separator = item.indexOf('=')
            if (separator <= 0) null
            else URLDecoder.decode(item.substring(0, separator), Charsets.UTF_8.name()) to
                URLDecoder.decode(item.substring(separator + 1), Charsets.UTF_8.name())
        }
        .toMap()
    val access = values["access_token"]?.takeIf(String::isNotBlank) ?: return null
    val refresh = values["refresh_token"]?.takeIf(String::isNotBlank) ?: return null
    return OAuthTokens(access, refresh)
}
