package com.neilyin.nnowork.data.treehole

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.datastore.appDataStore
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.MessageDigest
import java.util.UUID

class TreeHoleRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val stampDao: StampDao,
) {
    private val profileKey = stringPreferencesKey("treehole_profile_json")
    private val profileAdapter = JsonProvider.adapter<TreeHoleProfile>()

    private val api: TreeHoleApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(JsonProvider.moshi))
            .build()
            .create(TreeHoleApi::class.java)
    }

    val profile: Flow<TreeHoleProfile?> = context.appDataStore.data.map { prefs ->
        prefs[profileKey]?.let { runCatching { profileAdapter.fromJson(it) }.getOrNull() }
    }

    suspend fun saveProfile(profile: TreeHoleProfile) {
        context.appDataStore.edit { prefs ->
            prefs[profileKey] = profileAdapter.toJson(profile)
        }
    }

    suspend fun fetchFeed(industry: String? = null): List<TreeHolePost> =
        api.getFeedPosts(FeedRequest(industry = industry?.takeIf { it.isNotBlank() }))

    suspend fun fetchMyPosts(): List<TreeHolePost> =
        api.getMyPosts(DeviceHashRequest(deviceHash()))

    suspend fun fetchMyComments(): List<MyTreeHoleComment> =
        api.getMyComments(DeviceHashRequest(deviceHash()))

    suspend fun submitPost(content: String) {
        val profile = profile.first() ?: error("請先建立樹洞身份")
        val settings = settingsRepository.current()
        val stampCount = stampDao.countForRound(settings.currentRound)
        val companyName = profile.companyName.takeIf { profile.showCompany && it.isNotBlank() }

        val response = api.submitPost(
            SubmitPostRequest(
                content = content.trim(),
                deviceHash = deviceHash(),
                nickname = profile.nickname,
                industry = profile.industry,
                roundNumber = settings.currentRound,
                stampCount = stampCount,
                companyName = companyName,
            ),
        )
        if (!response.isSuccessful) {
            error("送出失敗：HTTP ${response.code()}")
        }
    }

    suspend fun deleteMyPost(postId: String): Boolean =
        api.deleteMyPost(DeletePostRequest(postId = postId, deviceHash = deviceHash())).success

    suspend fun deleteMyComment(commentId: String): Boolean =
        api.deleteMyComment(DeleteCommentRequest(commentId = commentId, deviceHash = deviceHash()))

    suspend fun fetchComments(postId: String): List<TreeHoleComment> =
        api.getComments(CommentsRequest(postId = postId))

    suspend fun submitComment(postId: String, content: String): Boolean {
        val profile = profile.first() ?: error("請先建立樹洞身份")
        val result = api.submitComment(
            SubmitCommentRequest(
                postId = postId,
                deviceHash = deviceHash(),
                nickname = profile.nickname,
                industry = profile.industry,
                content = content.trim(),
            ),
        )
        if (!result.success && result.reason == "rate_limit") {
            error("留言太熱情了，今天先讓樹洞喘口氣。")
        }
        return result.success
    }

    suspend fun reactPost(postId: String, reactionType: String): String? =
        api.reactPost(
            ReactPostRequest(
                postId = postId,
                deviceHash = deviceHash(),
                reactionType = reactionType,
            ),
        ).action

    suspend fun syncCoinBalance(): Int {
        val balance = api.getCoinBalance(DeviceHashRequest(deviceHash()))
        settingsRepository.update { it.copy(coinCount = balance) }
        return balance
    }

    suspend fun checkNewApprovals(): Int {
        val approvals = api.checkNewApprovals(DeviceHashRequest(deviceHash())).newApprovals
        if (approvals > 0) {
            syncCoinBalance()
        }
        return approvals
    }

    suspend fun addCoins(amount: Int) {
        api.addCoins(AddCoinsRequest(deviceHash = deviceHash(), amount = amount))
        syncCoinBalance()
    }

    private fun deviceHash(): String {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val SUPABASE_URL = "https://tcgncqgbmlenwrghkzvv.supabase.co/"
        const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRjZ25jcWdibWxlbndyZ2hrenZ2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY5NDcxMzYsImV4cCI6MjA5MjUyMzEzNn0.pGWaDUyZDKVJSZ_BmXp137BVkK4GqG2dQtCODWPZ1_s"
    }
}
