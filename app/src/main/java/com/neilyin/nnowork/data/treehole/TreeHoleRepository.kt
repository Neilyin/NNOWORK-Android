package com.neilyin.nnowork.data.treehole

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.auth.JwtClaims
import com.neilyin.nnowork.data.coin.CoinLedgerRepository
import com.neilyin.nnowork.data.datastore.appDataStore
import com.neilyin.nnowork.data.identity.DeviceIdentityRepository
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.reward.FileRewardJournalStore
import com.neilyin.nnowork.data.reward.RewardDelivery
import com.neilyin.nnowork.data.reward.RewardDeliveryJournal
import com.neilyin.nnowork.data.reward.RewardDeliveryRemote
import com.neilyin.nnowork.data.reward.RewardEvent
import com.neilyin.nnowork.data.reward.RewardEventIds
import com.neilyin.nnowork.data.reward.RewardIdentity
import com.neilyin.nnowork.data.reward.RewardJournalStore
import com.neilyin.nnowork.data.reward.RewardKind
import com.neilyin.nnowork.data.reward.RewardProtocolException
import com.neilyin.nnowork.data.reward.RewardRpcMissingException
import com.neilyin.nnowork.data.reward.RewardSyncSummary
import com.neilyin.nnowork.data.remote.SUPABASE_ANON_KEY
import com.neilyin.nnowork.data.remote.SUPABASE_URL
import com.neilyin.nnowork.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Retrofit
import com.squareup.moshi.Types
import java.io.File

const val ANDROID_FEEDBACK_SOURCE = "來自於android裝置"

class TreeHoleRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val stampDao: StampDao,
    private val deviceIdentityRepository: DeviceIdentityRepository,
    private val coinLedgerRepository: CoinLedgerRepository,
    private val accessTokenProvider: () -> String? = { null },
    private val claimDeviceData: suspend () -> Boolean = { false },
    /** 401 時 refresh 一次（§11）；參數是被拒的 token。 */
    private val refreshAccessToken: suspend (staleToken: String) -> Boolean = { false },
    /** Supabase auth user id（小寫）；獎勵 journal 依身分分開。 */
    private val currentUserId: () -> String? = { null },
    /** 送文 job_index 用的周目（currentRunNumber，不是 currentJobIndex）。 */
    private val runNumberProvider: suspend () -> Int = { 1 },
    apiFactory: () -> TreeHoleApi = { createTreeHoleApi() },
    rewardJournalStore: (() -> RewardJournalStore)? = null,
) {
    private val profileKey = stringPreferencesKey("treehole_profile_json")
    private val seenPostsKey = stringSetPreferencesKey("treehole_seen_post_ids")
    private val voiceOpenDateKey = stringPreferencesKey("treehole_voice_last_open")
    private val acceptedTermsVersionKey = intPreferencesKey("treeHoleTermsAcceptedVersion")
    private val pendingPushTokenKey = stringPreferencesKey("treehole_pending_fcm_token")
    private val filterIndustriesKey = stringSetPreferencesKey("treehole_filter_industries")
    private val seenAnswerCountsKey = stringPreferencesKey("treehole_seen_answer_counts")
    private val declinedIndustrySyncForKey = stringPreferencesKey("treehole_industry_sync_declined_for")
    private val likedPostIdsKey = stringSetPreferencesKey("treehole_liked_ids")
    private val cheeredPostIdsKey = stringSetPreferencesKey("treehole_cheered_ids")
    private val criedPostIdsKey = stringSetPreferencesKey("treehole_cried_ids")
    private val blockedAuthorsKey = stringPreferencesKey("treehole_blocked_authors")
    private val profileAdapter = JsonProvider.adapter<TreeHoleProfile>()
    private val blockedAuthorsAdapter = JsonProvider.moshi.adapter<List<BlockedTreeHoleAuthor>>(
        Types.newParameterizedType(List::class.java, BlockedTreeHoleAuthor::class.java),
    )
    private val submitPostMutex = Mutex()

    private val api: TreeHoleApi by lazy(apiFactory)
    private val tokenCaller = UserTokenCaller(accessTokenProvider, refreshAccessToken)
    private val v2Router = TreeHoleV2Router(tokenCaller)

    /** §18：Application Support 對應 filesDir（不放 cache）。損毀時讀取拋錯，停止領取。 */
    private val rewardDelivery: RewardDelivery by lazy {
        val store = rewardJournalStore?.invoke()
            ?: FileRewardJournalStore(File(context.filesDir, "reward_delivery/pending-v1.json"))
        RewardDelivery(RewardDeliveryJournal(store))
    }

    val profile: Flow<TreeHoleProfile?> = context.appDataStore.data.map { prefs ->
        prefs[profileKey]?.let { runCatching { profileAdapter.fromJson(it) }.getOrNull() }
    }

    /** 與 iOS 同 key 語意；缺少時為 0，不允許進入樹洞或發出社群請求。 */
    val acceptedTermsVersion: Flow<Int> = context.appDataStore.data.map { prefs ->
        prefs[acceptedTermsVersionKey] ?: 0
    }

    suspend fun acceptCurrentTerms() {
        context.appDataStore.edit { prefs ->
            prefs[acceptedTermsVersionKey] = CURRENT_TREE_HOLE_TERMS_VERSION
        }
        // FCM token 可能在使用者同意前就取得；先留在本機，同意後才上傳。
        // §5.17 暫停中：不上傳，並清掉 pending，避免每次同意條款都打一個必失敗的請求。
        if (!PUSH_TOKEN_REGISTRATION_ENABLED) {
            context.appDataStore.edit { prefs -> prefs.remove(pendingPushTokenKey) }
            return
        }
        context.appDataStore.data.first()[pendingPushTokenKey]?.let { token ->
            if (uploadPushToken(token)) {
                context.appDataStore.edit { prefs ->
                    if (prefs[pendingPushTokenKey] == token) prefs.remove(pendingPushTokenKey)
                }
            }
        }
    }

    suspend fun hasAcceptedCurrentTerms(): Boolean =
        TreeHoleTermsPolicy.canAccess(acceptedTermsVersion.first())

    suspend fun saveProfile(profile: TreeHoleProfile): NicknameSyncResponse? {
        require(profile.isComplete) { "請填寫暱稱、產業與職稱。" }
        val previousNickname = this.profile.first()?.nickname?.trim()
        val normalized = profile.copy(
            nickname = profile.nickname.trim(),
            industry = profile.industry.trim(),
            role = profile.role.trim(),
            companyName = profile.companyName.trim(),
            showCompany = profile.showCompany && profile.companyName.isNotBlank(),
        )
        context.appDataStore.edit { prefs ->
            prefs[profileKey] = profileAdapter.toJson(normalized)
        }
        val profileSynced = syncProfile(normalized)
        return if (
            profileSynced && previousNickname != null && previousNickname != normalized.nickname
        ) {
            syncMyNickname()
        } else {
            null
        }
    }

    /**
     * iOS 同樣採 fire-and-forget：本機 profile 是 UI 真相，後端同步失敗時不撤銷本機設定，
     * 下次開啟樹洞會再嘗試補齊主檔。
     */
    suspend fun syncProfile(profile: TreeHoleProfile): Boolean {
        if (!profile.isComplete || !hasAcceptedCurrentTerms()) return false
        val upsertSucceeded = networkOrNull {
            api.upsertProfile(
                UpsertProfileRequest(
                    deviceHash = deviceIdentityRepository.deviceHash(),
                    nickname = profile.nickname.trim(),
                    industry = profile.industry.trim(),
                    companyName = profile.companyName.trim().takeIf(String::isNotBlank),
                    showCompany = profile.showCompany && profile.companyName.isNotBlank(),
                    role = profile.role.trim(),
                ),
            ).isSuccessful
        } ?: false
        return completeProfileSync(
            upsertSucceeded = upsertSucceeded,
            signedIn = tokenCaller.isSignedIn,
            // upsert_profile is device-scoped. A signed-in account must claim it again so the
            // freshly changed profile is visible through user-scoped v2 RPCs. Returning false
            // on claim failure lets the next screen refresh retry while preserving local data.
            claimDeviceData = { networkOrNull { claimDeviceData() } == true },
        )
    }

    /** 今日心聲翻卡：本機記錄已讀貼文 id（對應 iOS treehole_seen_post_ids）。 */
    val seenPostIds: Flow<Set<String>> = context.appDataStore.data.map { prefs ->
        prefs[seenPostsKey] ?: emptySet()
    }

    /** 行業篩選偏好；空集合代表全部。貼文類型刻意不持久化。 */
    val filterIndustries: Flow<Set<String>> = storedStringSet(filterIndustriesKey)
    val likedPostIds: Flow<Set<String>> = storedStringSet(likedPostIdsKey)
    val cheeredPostIds: Flow<Set<String>> = storedStringSet(cheeredPostIdsKey)
    val criedPostIds: Flow<Set<String>> = storedStringSet(criedPostIdsKey)
    val declinedIndustrySyncFor: Flow<String?> = context.appDataStore.data.map { prefs ->
        prefs[declinedIndustrySyncForKey]?.takeIf(String::isNotBlank)
    }
    val seenAnswerCounts: Flow<Map<String, Int>> = context.appDataStore.data.map { prefs ->
        TreeHoleSeenAnswerCountsCodec.decode(prefs[seenAnswerCountsKey])
    }
    val blockedAuthors: Flow<List<BlockedTreeHoleAuthor>> = context.appDataStore.data.map { prefs ->
        decodeBlockedAuthors(prefs[blockedAuthorsKey])
    }

    suspend fun saveFilterIndustries(industries: Set<String>) {
        val normalized = industries.map(String::trim).filter(String::isNotEmpty).toSet()
        context.appDataStore.edit { prefs ->
            if (normalized.isEmpty()) prefs.remove(filterIndustriesKey)
            else prefs[filterIndustriesKey] = normalized
        }
    }

    /** 只記住使用者對「當前新行業」選過先不要；改行業後自然會再提示。 */
    suspend fun setDeclinedIndustrySyncFor(industry: String?) {
        val normalized = industry?.trim()?.takeIf(String::isNotEmpty)
        context.appDataStore.edit { prefs ->
            if (normalized == null) prefs.remove(declinedIndustrySyncForKey)
            else prefs[declinedIndustrySyncForKey] = normalized
        }
    }

    suspend fun markPostSeen(id: String) {
        if (id.isBlank()) return
        context.appDataStore.edit { prefs ->
            prefs[seenPostsKey] = (prefs[seenPostsKey] ?: emptySet()) + id
        }
    }

    /** 今日心聲每天首次進入自動開啟，記錄上次開啟日期（yyyy-MM-dd）。 */
    val voiceLastOpenDate: Flow<String> = context.appDataStore.data.map { prefs ->
        prefs[voiceOpenDateKey] ?: ""
    }

    suspend fun markVoiceOpened(today: String) {
        context.appDataStore.edit { prefs -> prefs[voiceOpenDateKey] = today }
    }

    suspend fun blockAuthor(authorKey: String, nickname: String): Boolean {
        val normalizedKey = TreeHoleBlockPolicy.normalizeAuthorKey(authorKey) ?: return false
        val normalizedNickname = nickname.trim().ifEmpty { "職場倖存者" }.take(80)
        context.appDataStore.edit { prefs ->
            val current = decodeBlockedAuthors(prefs[blockedAuthorsKey])
            val updated = (
                current.filterNot { it.authorKey == normalizedKey } +
                    BlockedTreeHoleAuthor(normalizedKey, normalizedNickname)
                ).sortedBy { it.nickname.lowercase() }
            prefs[blockedAuthorsKey] = blockedAuthorsAdapter.toJson(updated)
        }
        return true
    }

    suspend fun unblockAuthor(authorKey: String): Boolean {
        val normalizedKey = TreeHoleBlockPolicy.normalizeAuthorKey(authorKey) ?: return false
        var removed = false
        context.appDataStore.edit { prefs ->
            val current = decodeBlockedAuthors(prefs[blockedAuthorsKey])
            val updated = current.filterNot { it.authorKey == normalizedKey }
            removed = updated.size != current.size
            if (updated.isEmpty()) prefs.remove(blockedAuthorsKey)
            else prefs[blockedAuthorsKey] = blockedAuthorsAdapter.toJson(updated)
        }
        return removed
    }

    suspend fun fetchFeed(
        offset: Int = 0,
        postType: String? = null,
        industry: String? = null,
        limit: Int = DEFAULT_FEED_PAGE_SIZE,
    ): List<TreeHolePost> = withTermsAccess {
        require(offset >= 0) { "Feed offset must not be negative" }
        require(limit > 0) { "Feed limit must be positive" }
        val request = FeedRequest(
            limit = limit,
            offset = offset,
            industry = industry?.trim()?.takeIf(String::isNotEmpty),
            postType = postType?.trim()?.takeIf(String::isNotEmpty),
        )
        preferPublicFeedV2(
            v2Name = "get_feed_posts_v2",
            v2 = { api.getFeedPostsV2(request) },
            v1 = { api.getFeedPosts(request) },
        )
    }

    suspend fun fetchMyPosts(): List<TreeHolePost> = withTermsAccess {
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        withV2Fallback(
            v2 = { authorization -> api.getMyPostsV2(authorization, request) },
            v1 = { authorization -> api.getMyPosts(authorization, request) },
            mode = V2FallbackMode.USER_SCOPED_READ,
        )
    }

    suspend fun fetchMyComments(): List<MyTreeHoleComment> = withTermsAccess {
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        withV2Fallback(
            v2 = { authorization -> api.getMyCommentsV2(authorization, request) },
            v1 = { authorization -> api.getMyComments(authorization, request) },
            mode = V2FallbackMode.USER_SCOPED_READ,
        )
    }

    suspend fun fetchPostsByAuthor(authorKey: String, limit: Int = 200): List<TreeHolePost> =
        withTermsAccess {
            val normalizedKey = authorKey.trim().lowercase()
            require(AUTHOR_KEY_PATTERN.matches(normalizedKey)) { "Author key format is invalid" }
            require(limit in 1..200) { "Author post limit must be between 1 and 200" }
            val request = PostsByAuthorRequest(normalizedKey, limit)
            TreeHoleBlockPolicy.filterPosts(
                posts = preferPublicFeedV2(
                    v2Name = "get_posts_by_author_v2",
                    v2 = { api.getPostsByAuthorV2(request) },
                    v1 = { api.getPostsByAuthor(request) },
                ),
                blockedAuthorKeys = blockedAuthorKeysSnapshot(),
            )
        }

    /** 舊資料缺 author_key 才使用；暱稱會撞名，不可當作新資料的身分鍵。 */
    suspend fun fetchPostsByNicknameFallback(
        nickname: String,
        limit: Int = 200,
    ): List<TreeHolePost> = withTermsAccess {
        val normalizedNickname = nickname.trim()
        require(normalizedNickname.isNotEmpty()) { "Nickname must not be blank" }
        require(limit in 1..200) { "Author post limit must be between 1 and 200" }
        TreeHoleBlockPolicy.filterPosts(
            posts = api.getPostsByNickname(
                fields = PUBLIC_AUTHOR_POST_FIELDS,
                nicknameFilter = "eq.$normalizedNickname",
                limit = limit,
            ),
            blockedAuthorKeys = blockedAuthorKeysSnapshot(),
        )
    }

    suspend fun syncMyNickname(): NicknameSyncResponse? {
        ensureCurrentTermsAccepted()
        val response = networkOrNull {
            val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
            tokenCaller.call { authorization -> api.syncMyNickname(authorization, request) }
        }
        return response?.takeIf(NicknameSyncResponse::success)
    }

    /**
     * @param runNumber 蓋章當下的周目（與 roundNumber/stampCount 同時捕捉）。null 時讀取目前周目；
     *   ≥ 2 才會在 body 帶 job_index（§20.3）。
     */
    suspend fun submitPost(content: String, roundNumber: Int, stampCount: Int, runNumber: Int? = null) {
        ensureCurrentTermsAccepted()
        require(roundNumber > 0) { "集點卡回合必須大於 0。" }
        require(stampCount > 0) { "必須在蓋章後才能分享心聲。" }
        val normalizedContent = content.trim()
        require(normalizedContent.length in 5..200) { "心聲請填寫 5～200 字。" }
        check(submitPostMutex.tryLock()) { "貼文正在送出，請稍候。" }

        try {
            val profile = profile.first() ?: error("請先建立樹洞身份")
            require(profile.isComplete) { "請先完成樹洞身份。" }
            val companyName = profile.companyName.takeIf { profile.showCompany && it.isNotBlank() }

            val effectiveRunNumber = runNumber ?: try {
                runNumberProvider()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                1
            }
            val request = SubmitPostRequest(
                content = normalizedContent,
                deviceHash = deviceIdentityRepository.deviceHash(),
                nickname = profile.nickname,
                industry = profile.industry,
                role = profile.role.takeIf { it.isNotBlank() },
                roundNumber = roundNumber,
                stampCount = stampCount,
                companyName = companyName,
                jobIndex = postJobIndexValue(effectiveRunNumber),
            )
            var response = api.submitPost(buildSubmitPostBody(request))
            // 後端還沒有 job_index 欄位（未執行 db/032）：PostgREST 在寫入前就擋下，拿掉重送一次不會變兩則。
            if (!response.isSuccessful && request.jobIndex != null) {
                val error = PostgrestError.parse(runCatching { response.errorBody()?.string() }.getOrNull())
                if (isMissingJobIndexColumn(response.code(), error)) {
                    response = api.submitPost(buildSubmitPostBody(request, includeJobIndex = false))
                }
            }
            if (!response.isSuccessful) {
                if (response.code() == 403) throw TreeHoleAuthorBannedException()
                error("送出失敗：HTTP ${response.code()}")
            }
        } finally {
            submitPostMutex.unlock()
        }
    }

    suspend fun submitQuestion(content: String): SubmitQuestionResult {
        ensureCurrentTermsAccepted()
        val normalizedContent = content.trim()
        if (normalizedContent.length !in 5..200) return SubmitQuestionResult.InvalidLength
        val profile = profile.first() ?: return SubmitQuestionResult.NotJoined
        if (!profile.isComplete) return SubmitQuestionResult.NotJoined
        check(submitPostMutex.tryLock()) { "貼文正在送出，請稍候。" }

        return try {
            api.submitQuestion(
                SubmitQuestionRequest(
                    content = normalizedContent,
                    deviceHash = deviceIdentityRepository.deviceHash(),
                    nickname = profile.nickname,
                    industry = profile.industry,
                    role = profile.role.takeIf(String::isNotBlank),
                    companyName = profile.companyName.takeIf {
                        profile.showCompany && it.isNotBlank()
                    },
                ),
            ).toDomainResult()
        } finally {
            submitPostMutex.unlock()
        }
    }

    /** 額度只是顯示提示；失敗時回傳 null，由 submit_question 做最終把關。 */
    suspend fun fetchQuestionQuota(): QuestionQuotaResponse? {
        ensureCurrentTermsAccepted()
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        return networkOrNull { api.getQuestionQuota(request) }
    }

    /** 查詢失敗時不打擾使用者，交由下次進入個人頁再試。 */
    suspend fun fetchIndustrySyncStatus(): IndustrySyncStatusResponse? {
        ensureCurrentTermsAccepted()
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        return networkOrNull {
            tokenCaller.call { authorization -> api.getIndustrySyncStatus(authorization, request) }
        }
    }

    suspend fun updateMyPostsIndustry(includeMismatched: Boolean): UpdatePostsIndustryResponse {
        ensureCurrentTermsAccepted()
        return mutateAfterProfileSync(
            localProfile = profile.first(),
            syncProfile = { syncProfile(it) },
            mutation = {
                val request = UpdatePostsIndustryRequest(
                    deviceHash = deviceIdentityRepository.deviceHash(),
                    includeMismatched = includeMismatched,
                )
                tokenCaller.call { authorization -> api.updateMyPostsIndustry(authorization, request) }
            },
        )
    }

    suspend fun fetchMyAnswerStats(): TreeHoleAnswerStats = withTermsAccess {
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        val response = tokenCaller.call { authorization -> api.getMyAnswerStats(authorization, request) }
        TreeHoleAnswerStats(
            answerCount = response.answerCount.coerceAtLeast(0),
            helpfulCount = response.helpfulCount.coerceAtLeast(0),
            exp = response.exp.coerceAtLeast(0),
        )
    }

    suspend fun fetchMyQuestionActivity(): List<QuestionActivity> = withTermsAccess {
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        val rows = tokenCaller.call { authorization -> api.getMyQuestionActivity(authorization, request) }
        reconcileSeenAnswerCounts(rows)
        rows
    }

    suspend fun markAnswersSeen(postId: String, commentCount: Int) {
        val normalizedId = postId.trim()
        if (normalizedId.isEmpty()) return
        val normalizedCount = commentCount.coerceAtLeast(0)
        context.appDataStore.edit { prefs ->
            val current = TreeHoleSeenAnswerCountsCodec.decode(prefs[seenAnswerCountsKey]).toMutableMap()
            current[normalizedId] = normalizedCount
            prefs[seenAnswerCountsKey] = TreeHoleSeenAnswerCountsCodec.encode(current)
        }
    }

    private suspend fun reconcileSeenAnswerCounts(activity: List<QuestionActivity>) {
        context.appDataStore.edit { prefs ->
            val current = TreeHoleSeenAnswerCountsCodec.decode(prefs[seenAnswerCountsKey])
            val reconciled = TreeHoleQuestionActivityPolicy.reconcileSeenCounts(activity, current)
            val encoded = TreeHoleSeenAnswerCountsCodec.encode(reconciled)
            if (encoded.isEmpty()) prefs.remove(seenAnswerCountsKey)
            else prefs[seenAnswerCountsKey] = encoded
        }
    }

    suspend fun deleteMyPost(postId: String): Boolean = withTermsAccess {
        api.deleteMyPost(
            DeletePostRequest(postId = postId, deviceHash = deviceIdentityRepository.deviceHash()),
        ).success
    }

    suspend fun deleteMyAccount(): Boolean {
        ensureCurrentTermsAccepted()
        val success = api.deleteMyAccount(
            DeviceHashRequest(deviceIdentityRepository.deviceHash()),
        ).success
        if (success) {
            context.appDataStore.edit { prefs ->
                prefs.remove(profileKey)
                prefs.remove(seenAnswerCountsKey)
            }
        }
        return success
    }

    suspend fun deleteMyComment(commentId: String): Boolean = withTermsAccess {
        api.deleteMyComment(
            DeleteCommentRequest(commentId = commentId, deviceHash = deviceIdentityRepository.deviceHash()),
        )
    }

    suspend fun fetchComments(postId: String): List<TreeHoleComment> = withTermsAccess {
        TreeHoleBlockPolicy.filterComments(
            comments = api.getComments(CommentsRequest(postId = postId)),
            blockedAuthorKeys = blockedAuthorKeysSnapshot(),
        )
    }

    suspend fun markCommentHelpful(
        commentId: String,
        helpful: Boolean,
    ): MarkCommentHelpfulResult = withTermsAccess {
        val normalizedId = commentId.trim()
        require(normalizedId.isNotEmpty()) { "Comment id must not be blank" }
        val request = MarkCommentHelpfulRequest(
            commentId = normalizedId,
            deviceHash = deviceIdentityRepository.deviceHash(),
            helpful = helpful,
        )
        tokenCaller.call { authorization -> api.markCommentHelpful(authorization, request) }
            .toDomainResult(helpful)
    }

    suspend fun submitComment(postId: String, content: String): SubmitCommentResult {
        ensureCurrentTermsAccepted()
        // db/033 起不限字數：只擋去頭尾空白／換行後是空的內容（§5.9）。
        val normalizedContent = TreeHoleCommentPolicy.normalizedContentOrNull(content)
            ?: return SubmitCommentResult.InvalidLength
        val profile = profile.first() ?: error("請先建立樹洞身份")
        return api.submitComment(
            SubmitCommentRequest(
                postId = postId,
                deviceHash = deviceIdentityRepository.deviceHash(),
                nickname = profile.nickname,
                industry = profile.industry,
                content = normalizedContent,
            ),
        ).toSubmitCommentResult()
    }

    suspend fun submitReport(
        targetType: TreeHoleReportTargetType,
        targetId: String,
        reason: TreeHoleReportReason,
        note: String = "",
    ): SubmitReportResult = withTermsAccess {
        val response = api.submitReport(
            buildSubmitReportBody(
                targetType = targetType,
                targetId = targetId,
                deviceHash = deviceIdentityRepository.deviceHash(),
                reason = reason,
                note = note,
            ),
        )
        when {
            response.success -> SubmitReportResult.Submitted
            response.reason == "not_found" -> SubmitReportResult.TargetNotFound
            response.reason == "rate_limited" || response.reason == "rate_limit" ->
                SubmitReportResult.RateLimited
            else -> SubmitReportResult.Rejected(response.reason)
        }
    }

    suspend fun reactPost(postId: String, reactionType: String): String? = withTermsAccess {
        require(postId.isNotBlank()) { "Post id must not be blank" }
        val idsKey = reactionIdsKey(reactionType)
        val result = api.reactPost(
            ReactPostRequest(
                postId = postId,
                deviceHash = deviceIdentityRepository.deviceHash(),
                reactionType = reactionType,
            ),
        )
        check(result.success) { "反應送出失敗，請稍後再試。" }
        when (result.action) {
            "added" -> updateStoredId(idsKey, postId, shouldContain = true)
            "removed" -> updateStoredId(idsKey, postId, shouldContain = false)
        }
        result.action
    }

    suspend fun syncCoinBalance(): Int = coinLedgerRepository.currentBalance()

    /**
     * 金幣獎勵同步（§18 delivery + ACK）：重播 journal → 能力偵測 → claim → 先落地再以事件 UUID
     * 冪等入帳 → ACK。後端沒有新協定（404 + PGRST202）才走舊 RPC；確認新協定後永不退回。
     * iOS 觸發點：進樹洞時、以及每次重新載入「我的貼文」之後。
     */
    suspend fun synchronizeRewards(): RewardSyncSummary {
        ensureCurrentTermsAccepted()
        val deviceHash = deviceIdentityRepository.deviceHash()
        // 一次同步只用一份身分＋token 快照：中途切帳號也不會把事件記到另一個身分、或用另一個 token 去 ACK。
        val caller = tokenCaller.pinned(JwtClaims::subject)
        val identity = rewardIdentityFor(caller.currentToken(), deviceHash, currentUserId)
        val credited = rewardDelivery.synchronize(
            remote = TreeHoleRewardRemote(deviceHash, caller),
            identity = identity,
            credit = { event -> coinLedgerRepository.creditRewardEventIfAbsent(event) },
        )
        return RewardSyncSummary.of(credited)
    }

    private inner class TreeHoleRewardRemote(
        private val deviceHash: String,
        private val caller: UserTokenCaller,
    ) : RewardDeliveryRemote {
        private val router = TreeHoleV2Router(caller)

        private suspend fun <T> withV2Fallback(
            v2: suspend (authorization: String) -> T,
            v1: suspend (authorization: String?) -> T,
            mode: V2FallbackMode,
        ): T = router.userScoped(mode, v2, v1)

        /** 只有 404 + PGRST202 會轉成 [RewardRpcMissingException]；其他錯誤原樣拋出（這次先不領）。 */
        private suspend fun <T> rewardRpc(name: String, block: suspend (authorization: String?) -> T): T = try {
            caller.call(block)
        } catch (error: HttpException) {
            if (isMissingRpc(error.code(), error.postgrestError())) throw RewardRpcMissingException(name)
            throw error
        }

        override suspend fun supportsRewardEvents(): Boolean = try {
            val response = rewardRpc("reward_delivery_capabilities") { authorization ->
                api.rewardDeliveryCapabilities(authorization)
            }
            if (response.version != 1) {
                throw RewardProtocolException("Unsupported reward delivery version ${response.version}")
            }
            true
        } catch (_: RewardRpcMissingException) {
            false
        }

        override suspend fun claimRewardEvents(deliveryId: String): List<RewardEvent> {
            val request = ClaimRewardEventsRequest(deviceHash = deviceHash, deliveryId = deliveryId)
            return rewardRpc("claim_reward_events") { authorization -> api.claimRewardEvents(authorization, request) }
                .map { it.toRewardEvent() }
        }

        override suspend fun acknowledgeRewardEvents(ids: List<String>, deliveryId: String): List<String> {
            val request = AckRewardEventsRequest(deviceHash = deviceHash, deliveryId = deliveryId, eventIds = ids)
            return rewardRpc("ack_reward_events") { authorization -> api.ackRewardEvents(authorization, request) }
                .acknowledgedIds
        }

        override suspend fun claimLegacyRewardCount(kind: RewardKind): Int {
            val request = DeviceHashRequest(deviceHash)
            val count = when (kind) {
                RewardKind.TREE_HOLE_POST_APPROVED -> withV2Fallback(
                    v2 = { authorization -> api.checkNewApprovalsV2(authorization, request) },
                    v1 = { authorization -> api.checkNewApprovals(authorization, request) },
                    mode = V2FallbackMode.REWARD_MUTATION,
                ).newApprovals
                RewardKind.FEEDBACK_REWARD -> withV2Fallback(
                    v2 = { authorization -> api.checkNewFeedbackRewardsV2(authorization, request) },
                    v1 = { authorization -> api.checkNewFeedbackRewards(authorization, request) },
                    mode = V2FallbackMode.REWARD_MUTATION,
                ).newRewards
            }
            return count ?: throw RewardProtocolException("Legacy reward response is missing its count")
        }
    }


    /** 分享使用心得（樹洞回饋）。審核通過後後端會 +金幣。 */
    suspend fun submitFeedback(content: String) {
        val result = api.submitFeedback(
            buildSubmitFeedbackRequest(
                deviceHash = deviceIdentityRepository.deviceHash(),
                content = content,
            ),
        )
        if (!result.success) {
            if (result.reason == "rate_limit" || result.reason == "rate_limited") {
                error("今天已經回饋過了，明天再來吧。")
            }
            error("回饋送出失敗，稍後再試。")
        }
    }

    /**
     * 上傳 FCM token，綁定 device_hash（對應 iOS device_push_tokens）。
     * ⛔ §5.17 暫停：[PUSH_TOKEN_REGISTRATION_ENABLED] 為 false 時不送出、不留 pending 重試。
     */
    suspend fun registerPushToken(token: String) {
        when (PushTokenRegistrationPolicy.decide(PUSH_TOKEN_REGISTRATION_ENABLED, hasAcceptedCurrentTerms())) {
            PushTokenRegistrationPolicy.Action.DISCARD -> {
                context.appDataStore.edit { prefs -> prefs.remove(pendingPushTokenKey) }
            }
            PushTokenRegistrationPolicy.Action.DEFER_UNTIL_TERMS -> {
                context.appDataStore.edit { prefs -> prefs[pendingPushTokenKey] = token }
            }
            PushTokenRegistrationPolicy.Action.UPLOAD -> {
                if (!uploadPushToken(token)) {
                    context.appDataStore.edit { prefs -> prefs[pendingPushTokenKey] = token }
                }
            }
        }
    }

    private suspend fun uploadPushToken(token: String): Boolean =
        networkOrNull {
            api.registerDeviceToken(
                request = DevicePushTokenRequest(
                    deviceHash = deviceIdentityRepository.deviceHash(),
                    fcmToken = token,
                ),
            ).isSuccessful
        } ?: false

    private fun storedStringSet(key: Preferences.Key<Set<String>>): Flow<Set<String>> =
        context.appDataStore.data.map { prefs -> prefs[key]?.toSet() ?: emptySet() }

    private suspend fun blockedAuthorKeysSnapshot(): Set<String> =
        blockedAuthors.first().map(BlockedTreeHoleAuthor::authorKey).toSet()

    private fun decodeBlockedAuthors(json: String?): List<BlockedTreeHoleAuthor> = json
        ?.let { runCatching { blockedAuthorsAdapter.fromJson(it) }.getOrNull() }
        .orEmpty()
        .mapNotNull { author ->
            TreeHoleBlockPolicy.normalizeAuthorKey(author.authorKey)?.let { normalizedKey ->
                author.copy(
                    authorKey = normalizedKey,
                    nickname = author.nickname.trim().ifEmpty { "職場倖存者" }.take(80),
                )
            }
        }
        .distinctBy(BlockedTreeHoleAuthor::authorKey)

    private fun reactionIdsKey(reactionType: String): Preferences.Key<Set<String>> = when (reactionType) {
        "like" -> likedPostIdsKey
        "cheer" -> cheeredPostIdsKey
        "cry" -> criedPostIdsKey
        else -> error("不支援的反應類型：$reactionType")
    }

    private suspend fun updateStoredId(
        key: Preferences.Key<Set<String>>,
        id: String,
        shouldContain: Boolean,
    ) {
        context.appDataStore.edit { prefs ->
            val current = prefs[key]?.toSet() ?: emptySet()
            val updated = if (shouldContain) current + id else current - id
            if (updated.isEmpty()) prefs.remove(key) else prefs[key] = updated
        }
    }

    /**
     * 「我的資料」／舊獎勵協定的 v1/v2 分流（§11）：未登入直接 v1（anon）；已登入先打 v2（401 refresh 一次），
     * 只有 404 + PGRST202 才改打 v1（仍帶 user token）。其他錯誤照實拋出。
     */
    private suspend fun <T> withV2Fallback(
        v2: suspend (authorization: String) -> T,
        v1: suspend (authorization: String?) -> T,
        mode: V2FallbackMode,
    ): T = v2Router.userScoped(mode, v2, v1)

    /** 公開 feed（§13.9 例外、§20.2）：v2 任何失敗都改打 v1，只有 404 + PGRST202 才記住。 */
    private suspend fun <T> preferPublicFeedV2(
        v2Name: String,
        v2: suspend () -> T,
        v1: suspend () -> T,
    ): T = v2Router.publicFeed(v2Name, v2, v1)

    private suspend fun <T> networkOrNull(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private suspend fun ensureCurrentTermsAccepted() {
        check(hasAcceptedCurrentTerms()) { "請先閱讀並同意最新樹洞條款。" }
    }

    private suspend fun <T> withTermsAccess(block: suspend () -> T): T {
        ensureCurrentTermsAccepted()
        return block()
    }

    internal companion object {
        val AUTHOR_KEY_PATTERN = Regex("^[0-9a-f]{32}$")

        /**
         * §21.2：直接查 tree_hole_posts 一律明確列欄位，不含 device_hash、不可 `*`。
         * 包含 iOS 固定 12 欄全部，另加 status 與 post_type：db/034 的欄位級 GRANT 是「除 device_hash 外全部」，
         * 兩欄都讀得到；少了 post_type，舊的提問貼文會被解成蓋章貼文。
         */
        const val PUBLIC_AUTHOR_POST_FIELDS =
            "id,content,industry,nickname,company_name,role,round_number,stamp_count," +
                "likes_count,cheer_count,cry_count,status,created_at,post_type"
    }
}

/** ⛔ §5.17：db/034 刪除 device_push_tokens policy，直接 POST 會被拒。推播大修改 RPC 前保持 false。 */
const val PUSH_TOKEN_REGISTRATION_ENABLED = false

internal object PushTokenRegistrationPolicy {
    enum class Action { UPLOAD, DEFER_UNTIL_TERMS, DISCARD }

    fun decide(enabled: Boolean, termsAccepted: Boolean): Action = when {
        !enabled -> Action.DISCARD
        !termsAccepted -> Action.DEFER_UNTIL_TERMS
        else -> Action.UPLOAD
    }
}

/**
 * 獎勵身分取自**同一份** token 快照：有 token → `account:<JWT sub>`（解不出 sub 才退回目前 user id）；
 * 沒 token → `device:<device_hash>`。不另外讀 live session，避免 token 與身分來自不同帳號。
 */
internal fun rewardIdentityFor(token: String?, deviceHash: String, fallbackUserId: () -> String?): String {
    val accountId = token?.let { JwtClaims.subject(it) ?: fallbackUserId() }
    return RewardIdentity.of(accountId, deviceHash)
}

internal fun RewardEventResponse.toRewardEvent(): RewardEvent = RewardEvent(
    id = RewardEventIds.normalize(id),
    kind = RewardKind.fromRaw(kind) ?: throw RewardProtocolException("Unknown reward kind $kind"),
    amount = amount.takeIf { it > 0 } ?: throw RewardProtocolException("Reward amount must be positive"),
)

internal fun createTreeHoleApi(
    baseUrl: String = SUPABASE_URL,
    anonKey: String = SUPABASE_ANON_KEY,
): TreeHoleApi {
    val client = OkHttpClient.Builder()
        .addInterceptor { chain -> chain.proceed(chain.request().withTreeHoleHeaders(anonKey)) }
        .build()
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(treeHoleMoshiConverterFactory())
        .build()
        .create(TreeHoleApi::class.java)
}

internal fun String.withAndroidDeviceSource(): String {
    val userContent = trim()
    return if (userContent.endsWith(ANDROID_FEEDBACK_SOURCE)) {
        userContent
    } else {
        "$userContent\n\n$ANDROID_FEEDBACK_SOURCE"
    }
}

/** 唯一的回饋 request 建置點，確保 Retrofit 最後送出的 p_content 已附上平台標記。 */
internal fun buildSubmitFeedbackRequest(deviceHash: String, content: String) =
    SubmitFeedbackRequest(
        deviceHash = deviceHash,
        content = content.withAndroidDeviceSource(),
    )

internal fun Request.withTreeHoleHeaders(anonKey: String): Request {
    val builder = newBuilder()
        .header("Content-Type", "application/json")
        .header("apikey", anonKey)
    if (header("Authorization") == null) {
        builder.header("Authorization", "Bearer $anonKey")
    }
    return builder.build()
}

/** A null value deliberately leaves Authorization absent for the anon interceptor to add. */
internal fun treeHoleAuthorizationHeader(accessToken: String?): String? =
    accessToken?.trim()?.takeIf(String::isNotEmpty)?.let { "Bearer $it" }
