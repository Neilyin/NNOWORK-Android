package com.neilyin.nnowork.data

import android.content.Context
import com.neilyin.nnowork.data.remote.SUPABASE_ANON_KEY
import com.neilyin.nnowork.data.remote.SUPABASE_URL
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Supabase 版本化語錄快取；任何失敗都安靜保留舊快取與內建 fallback。 */
object RemoteQuotesStore {
    private val client = OkHttpClient()
    private val quoteListAdapter = JsonProvider.moshi.adapter<List<CachedQuote>>(
        Types.newParameterizedType(List::class.java, CachedQuote::class.java),
    )
    private val metaListAdapter = JsonProvider.moshi.adapter<List<ContentMeta>>(
        Types.newParameterizedType(List::class.java, ContentMeta::class.java),
    )

    fun chineseQuotes(context: Context): List<String> = readCache(context)
        .filter { it.lang == "zh-Hant" }
        .map { it.content }

    suspend fun refreshIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - preferences.getLong(LAST_CHECK_KEY, 0L) < TimeUnit.DAYS.toMillis(1)) return@withContext

        runCatching {
            val remoteVersion = fetchVersion()
            preferences.edit().putLong(LAST_CHECK_KEY, now).apply()
            if (remoteVersion <= preferences.getInt(VERSION_KEY, 0)) return@runCatching
            val quotes = fetchQuotes()
            if (quotes.isEmpty()) return@runCatching
            val target = File(appContext.filesDir, CACHE_FILE)
            val temporary = File(appContext.filesDir, "$CACHE_FILE.tmp")
            temporary.writeText(quoteListAdapter.toJson(quotes), Charsets.UTF_8)
            check(temporary.renameTo(target) || run { temporary.copyTo(target, overwrite = true); temporary.delete(); true })
            preferences.edit().putInt(VERSION_KEY, remoteVersion).apply()
        }
    }

    private fun readCache(context: Context): List<CachedQuote> = runCatching {
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.isFile) emptyList() else quoteListAdapter.fromJson(file.readText(Charsets.UTF_8)).orEmpty()
    }.getOrDefault(emptyList())

    private fun fetchVersion(): Int {
        val body = get("rest/v1/app_content_meta?select=key,int_value&key=eq.quotes_version")
        return metaListAdapter.fromJson(body)?.firstOrNull()?.intValue ?: error("Missing quotes version")
    }

    private fun fetchQuotes(): List<CachedQuote> {
        val body = get("rest/v1/quotes?select=lang,content&is_active=eq.true&order=sort_order.asc")
        return quoteListAdapter.fromJson(body).orEmpty()
    }

    private fun get(path: String): String {
        val request = Request.Builder()
            .url("$SUPABASE_URL$path")
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Quote refresh HTTP ${response.code}" }
            return response.body?.string().orEmpty()
        }
    }

    @JsonClass(generateAdapter = false)
    data class CachedQuote(val lang: String = "", val content: String = "")

    @JsonClass(generateAdapter = false)
    data class ContentMeta(
        val key: String = "",
        @Json(name = "int_value") val intValue: Int? = null,
    )

    private const val PREFS_NAME = "nnowork_quotes_meta"
    private const val VERSION_KEY = "quotes_cache_version"
    private const val LAST_CHECK_KEY = "quotes_last_check_date"
    private const val CACHE_FILE = "quotes_cache.json"
}
