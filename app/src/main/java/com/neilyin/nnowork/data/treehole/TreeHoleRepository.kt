package com.neilyin.nnowork.data.treehole

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.coin.CoinLedgerRepository
import com.neilyin.nnowork.data.coin.CoinTransactionType
import com.neilyin.nnowork.data.datastore.appDataStore
import com.neilyin.nnowork.data.identity.DeviceIdentityRepository
import com.neilyin.nnowork.data.local.StampDao
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

const val ANDROID_FEEDBACK_SOURCE = "來自於android裝置"

class TreeHoleRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val stampDao: StampDao,
    private val deviceIdentityRepository: DeviceIdentityRepository,
    private val coinLedgerRepository: CoinLedgerRepository,
    private val accessTokenProvider: () -> String? = { null },
    private val claimDeviceData: suspend () -> Boolean = { false },
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

    private val api: TreeHoleApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().withTreeHoleHeaders(SUPABASE_ANON_KEY))
            }
            .build()

        Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .client(client)
            .addConverterFactory(treeHoleMoshiConverterFactory())
            .build()
            .create(TreeHoleApi::class.java)
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
            signedIn = treeHoleAuthorizationHeader(accessTokenProvider()) != null,
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
        api.getFeedPosts(
            FeedRequest(
                limit = limit,
                offset = offset,
                industry = industry?.trim()?.takeIf(String::isNotEmpty),
                postType = postType?.trim()?.takeIf(String::isNotEmpty),
            ),
        )
    }

    suspend fun fetchMyPosts(): List<TreeHolePost> = withTermsAccess {
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        withV2Fallback(
            v2 = { authorization -> api.getMyPostsV2(authorization, request) },
            v1 = { api.getMyPosts(request) },
        )
    }

    suspend fun fetchMyComments(): List<MyTreeHoleComment> = withTermsAccess {
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        withV2Fallback(
            v2 = { authorization -> api.getMyCommentsV2(authorization, request) },
            v1 = { api.getMyComments(request) },
        )
    }

    suspend fun fetchPostsByAuthor(authorKey: String, limit: Int = 200): List<TreeHolePost> =
        withTermsAccess {
            val normalizedKey = authorKey.trim().lowercase()
            require(AUTHOR_KEY_PATTERN.matches(normalizedKey)) { "Author key format is invalid" }
            require(limit in 1..200) { "Author post limit must be between 1 and 200" }
            TreeHoleBlockPolicy.filterPosts(
                posts = api.getPostsByAuthor(PostsByAuthorRequest(normalizedKey, limit)),
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
            api.syncMyNickname(
                authorization = treeHoleAuthorizationHeader(accessTokenProvider()),
                request = DeviceHashRequest(deviceIdentityRepository.deviceHash()),
            )
        }
        return response?.takeIf(NicknameSyncResponse::success)
    }

    @Deprecated(
        message = "Pass the stamp-time round and count snapshot to avoid stale metadata.",
        replaceWith = ReplaceWith("submitPost(content, roundNumber, stampCount)"),
    )
    suspend fun submitPost(content: String) {
        val settings = settingsRepository.current()
        submitPost(
            content = content,
            roundNumber = settings.currentRound,
            stampCount = stampDao.countForRound(settings.currentRound),
        )
    }

    suspend fun submitPost(content: String, roundNumber: Int, stampCount: Int) {
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

            val response = api.submitPost(
                SubmitPostRequest(
                    content = normalizedContent,
                    deviceHash = deviceIdentityRepository.deviceHash(),
                    nickname = profile.nickname,
                    industry = profile.industry,
                    role = profile.role.takeIf { it.isNotBlank() },
                    roundNumber = roundNumber,
                    stampCount = stampCount,
                    companyName = companyName,
                ),
            )
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
            api.getIndustrySyncStatus(
                authorization = treeHoleAuthorizationHeader(accessTokenProvider()),
                request = request,
            )
        }
    }

    suspend fun updateMyPostsIndustry(includeMismatched: Boolean): UpdatePostsIndustryResponse {
        ensureCurrentTermsAccepted()
        return mutateAfterProfileSync(
            localProfile = profile.first(),
            syncProfile = { syncProfile(it) },
            mutation = {
                api.updateMyPostsIndustry(
                    authorization = treeHoleAuthorizationHeader(accessTokenProvider()),
                    request = UpdatePostsIndustryRequest(
                        deviceHash = deviceIdentityRepository.deviceHash(),
                        includeMismatched = includeMismatched,
                    ),
                )
            },
        )
    }

    suspend fun fetchMyAnswerStats(): TreeHoleAnswerStats = withTermsAccess {
        val response = api.getMyAnswerStats(
            authorization = treeHoleAuthorizationHeader(accessTokenProvider()),
            request = DeviceHashRequest(deviceIdentityRepository.deviceHash()),
        )
        TreeHoleAnswerStats(
            answerCount = response.answerCount.coerceAtLeast(0),
            helpfulCount = response.helpfulCount.coerceAtLeast(0),
            exp = response.exp.coerceAtLeast(0),
        )
    }

    suspend fun fetchMyQuestionActivity(): List<QuestionActivity> = withTermsAccess {
        val rows = api.getMyQuestionActivity(
            authorization = treeHoleAuthorizationHeader(accessTokenProvider()),
            request = DeviceHashRequest(deviceIdentityRepository.deviceHash()),
        )
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
        api.markCommentHelpful(
            authorization = treeHoleAuthorizationHeader(accessTokenProvider()),
            request = MarkCommentHelpfulRequest(
                commentId = normalizedId,
                deviceHash = deviceIdentityRepository.deviceHash(),
                helpful = helpful,
            ),
        ).toDomainResult(helpful)
    }

    suspend fun submitComment(postId: String, content: String): SubmitCommentResult {
        ensureCurrentTermsAccepted()
        val normalizedContent = content.trim()
        if (normalizedContent.length !in 1..50) return SubmitCommentResult.InvalidLength
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

    suspend fun checkNewApprovals(): Int {
        ensureCurrentTermsAccepted()
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        val approvals = withV2Fallback(
            v2 = { authorization -> api.checkNewApprovalsV2(authorization, request) },
            v1 = { api.checkNewApprovals(request) },
            mode = V2FallbackMode.REWARD_MUTATION,
        ).newApprovals
        if (approvals > 0) {
            val reward = Math.multiplyExact(approvals, COINS_PER_APPROVED_POST)
            coinLedgerRepository.credit(
                amount = reward,
                type = CoinTransactionType.TREE_HOLE_POST_APPROVED,
                note = "樹洞貼文審核通過 ×$approvals",
            )
        }
        return approvals
    }

    suspend fun checkNewFeedbackRewards(): Int {
        ensureCurrentTermsAccepted()
        val request = DeviceHashRequest(deviceIdentityRepository.deviceHash())
        val rewards = withV2Fallback(
            v2 = { authorization -> api.checkNewFeedbackRewardsV2(authorization, request) },
            v1 = { api.checkNewFeedbackRewards(request) },
            mode = V2FallbackMode.REWARD_MUTATION,
        ).newRewards
        if (rewards > 0) {
            val reward = Math.multiplyExact(rewards, COINS_PER_FEEDBACK_REWARD)
            coinLedgerRepository.credit(
                amount = reward,
                type = CoinTransactionType.FEEDBACK_REWARD,
                note = "意見回饋獲獎 ×$rewards",
            )
        }
        return rewards
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

    /** 上傳 FCM token，綁定 device_hash（對應 iOS device_push_tokens）。 */
    suspend fun registerPushToken(token: String) {
        if (!hasAcceptedCurrentTerms()) {
            context.appDataStore.edit { prefs -> prefs[pendingPushTokenKey] = token }
            return
        }
        if (!uploadPushToken(token)) {
            context.appDataStore.edit { prefs -> prefs[pendingPushTokenKey] = token }
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

    private suspend fun <T> withV2Fallback(
        v2: suspend (authorization: String) -> T,
        v1: suspend () -> T,
        mode: V2FallbackMode = V2FallbackMode.READ_ONLY,
    ): T {
        val token = accessTokenProvider()?.trim()?.takeIf(String::isNotEmpty) ?: return v1()
        return try {
            v2("Bearer $token")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val httpStatus = (error as? HttpException)?.code()
            if (TreeHoleV2FallbackPolicy.shouldFallback(mode, httpStatus)) v1() else throw error
        }
    }

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

    private companion object {
        val AUTHOR_KEY_PATTERN = Regex("^[0-9a-f]{32}$")
        const val PUBLIC_AUTHOR_POST_FIELDS =
            "id,content,industry,nickname,company_name,role,round_number,stamp_count," +
                "likes_count,cheer_count,cry_count,status,created_at,post_type"
        const val COINS_PER_APPROVED_POST = 5
        const val COINS_PER_FEEDBACK_REWARD = 10
    }
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
