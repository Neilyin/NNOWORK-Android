package com.neilyin.nnowork.feature.treehole

import android.widget.Toast
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.R
import com.neilyin.nnowork.analytics.Analytics
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.data.treehole.TreeHoleProfile
import com.neilyin.nnowork.data.treehole.TreeHoleRepository
import com.neilyin.nnowork.data.treehole.TreeHoleShareSource
import com.neilyin.nnowork.data.treehole.TreeHoleComment
import com.neilyin.nnowork.data.treehole.BlockedTreeHoleAuthor
import com.neilyin.nnowork.data.treehole.SubmitCommentResult
import com.neilyin.nnowork.data.treehole.SubmitReportResult
import com.neilyin.nnowork.data.treehole.TreeHoleAuthorBannedException
import com.neilyin.nnowork.data.treehole.TreeHoleBlockPolicy
import com.neilyin.nnowork.data.treehole.TreeHoleReportReason
import com.neilyin.nnowork.data.treehole.TreeHoleReportTargetType
import com.neilyin.nnowork.data.treehole.CURRENT_TREE_HOLE_TERMS_VERSION
import com.neilyin.nnowork.data.treehole.TreeHoleTermsPolicy
import com.neilyin.nnowork.data.treehole.TREE_HOLE_DISCLAIMER_V3
import com.neilyin.nnowork.data.treehole.TREE_HOLE_PRIVACY_V3
import com.neilyin.nnowork.data.treehole.MyTreeHoleComment
import com.neilyin.nnowork.data.treehole.DEFAULT_FEED_PAGE_SIZE
import com.neilyin.nnowork.data.treehole.IndustrySyncStatusResponse
import com.neilyin.nnowork.data.treehole.MarkCommentHelpfulResult
import com.neilyin.nnowork.data.treehole.POST_TYPE_QUESTION
import com.neilyin.nnowork.data.treehole.POST_TYPE_STAMP
import com.neilyin.nnowork.data.treehole.QuestionQuotaResponse
import com.neilyin.nnowork.data.treehole.QuestionActivity
import com.neilyin.nnowork.data.treehole.SubmitQuestionResult
import com.neilyin.nnowork.data.treehole.TreeHoleAnswerStats
import com.neilyin.nnowork.data.treehole.TreeHoleFeedPage
import com.neilyin.nnowork.data.treehole.TreeHoleFeedPolicy
import com.neilyin.nnowork.data.treehole.TreeHoleIndustryCatalog
import com.neilyin.nnowork.data.treehole.TreeHoleQuestionActivityPolicy
import com.neilyin.nnowork.data.treehole.toShareCardContent
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

private const val TREE_HOLE_COMMENT_MAX_LENGTH = 50

enum class TreeHoleFeatureOperation {
    ACCEPT_TERMS,
    SAVE_PROFILE,
    REFRESH,
    RESET_FEED,
    LOAD_MORE_FEED,
    SAVE_INDUSTRY_FILTER,
    SUBMIT_STAMP_POST,
    SUBMIT_QUESTION,
    LOAD_COMMENTS,
    SUBMIT_COMMENT,
    MARK_COMMENT_HELPFUL,
    OPEN_NEW_ANSWER,
    LOAD_AUTHOR_POSTS,
    REACTION,
    MARK_SEEN,
    DELETE_POST,
    DELETE_COMMENT,
    DELETE_ACCOUNT,
    UPDATE_INDUSTRY,
    DECLINE_INDUSTRY_UPDATE,
    SYNC_COINS_AND_APPROVALS,
    REPORT_CONTENT,
    BLOCK_AUTHOR,
}

enum class TreeHoleFeatureFailureReason {
    TERMS_REQUIRED,
    PROFILE_INCOMPLETE,
    INVALID_CONTENT_LENGTH,
    INVALID_IDENTIFIER,
    INVALID_STAMP_CONTEXT,
    NO_SELECTED_POST,
    UNSUPPORTED_REACTION,
    NO_PENDING_INDUSTRY_SYNC,
    SERVER_REJECTED,
    PARTIAL_REFRESH,
    AUTHOR_BANNED,
    COMMENT_RATE_LIMITED,
    REPORT_RATE_LIMITED,
    REPORT_TARGET_NOT_FOUND,
    BLOCK_UNAVAILABLE,
    REQUEST_FAILED,
}

enum class TreeHoleRefreshSection {
    PROFILE_SYNC,
    MY_POSTS,
    MY_COMMENTS,
    QUESTION_QUOTA,
    ANSWER_STATS,
    QUESTION_ACTIVITY,
    INDUSTRY_SYNC_STATUS,
}

data class TreeHoleFeatureFailure(
    val operation: TreeHoleFeatureOperation,
    val reason: TreeHoleFeatureFailureReason,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val failedRefreshSections: Set<TreeHoleRefreshSection> = emptySet(),
    /** UI 不應直接顯示；只供記錄與除錯。 */
    val causeType: String? = null,
)

enum class TreeHoleFeatureSuccessKind {
    TERMS_ACCEPTED,
    PROFILE_SAVED,
    STAMP_POST_SUBMITTED,
    QUESTION_SUBMITTED,
    COMMENT_SUBMITTED,
    HELPFUL_ANSWER_UPDATED,
    POST_DELETED,
    COMMENT_DELETED,
    ACCOUNT_DELETED,
    INDUSTRY_UPDATED,
    INDUSTRY_UPDATE_DECLINED,
    COINS_AND_APPROVALS_SYNCED,
    REPORT_SUBMITTED,
    REPORT_SUBMITTED_AND_BLOCKED,
    AUTHOR_BLOCKED,
}

data class TreeHoleFeatureSuccess(
    val kind: TreeHoleFeatureSuccessKind,
    val affectedCount: Int? = null,
)

data class TreeHoleRewardSyncResult(
    val newApprovals: Int,
    val newFeedbackRewards: Int,
    val coinBalance: Int,
)

data class TreeHoleIndustryUpdateResult(
    val includeMismatched: Boolean,
    val updatedCount: Int,
    val industry: String?,
)

enum class TreeHoleReactionType(val apiValue: String) {
    LIKE("like"),
    CHEER("cheer"),
    CRY("cry");

    companion object {
        fun fromApiValue(value: String): TreeHoleReactionType? =
            entries.firstOrNull { it.apiValue == value }
    }
}

data class TreeHoleReactionKey(
    val postId: String,
    val type: TreeHoleReactionType,
)

data class TreeHoleUiState(
    val profile: TreeHoleProfile? = null,
    /** null 代表 DataStore 尚未載入；載入前不可顯示樹洞內容或發網路請求。 */
    val acceptedTermsVersion: Int? = null,
    val feedMode: TreeHoleFeedMode = TreeHoleFeedMode.ALL,
    /** 後端已依 [feedMode] 過濾、但尚未套用本機行業篩選的分頁內容。 */
    val feedPosts: List<TreeHolePost> = emptyList(),
    val feedNextOffset: Int = 0,
    val feedEndReached: Boolean = false,
    val feedLoading: Boolean = false,
    val feedLoadingMore: Boolean = false,
    val selectedIndustries: Set<String> = emptySet(),
    val myPosts: List<TreeHolePost> = emptyList(),
    val myComments: List<MyTreeHoleComment> = emptyList(),
    val selectedPost: TreeHolePost? = null,
    val comments: List<TreeHoleComment> = emptyList(),
    val commentsLoading: Boolean = false,
    val submittingComment: Boolean = false,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val refreshFailures: Set<TreeHoleRefreshSection> = emptySet(),
    val posting: Boolean = false,
    val submittingQuestion: Boolean = false,
    val questionSubmissionResult: SubmitQuestionResult? = null,
    val questionQuota: QuestionQuotaResponse? = null,
    val myAnswerExp: Int = 0,
    val myHelpfulAnswerCount: Int = 0,
    val questionActivity: List<QuestionActivity> = emptyList(),
    val seenAnswerCounts: Map<String, Int> = emptyMap(),
    val pendingHelpfulCommentIds: Set<String> = emptySet(),
    val selectedAuthor: TreeHoleAuthorRef? = null,
    val authorPosts: List<TreeHolePost> = emptyList(),
    val authorPostsLoading: Boolean = false,
    val industrySyncStatus: IndustrySyncStatusResponse? = null,
    val industrySyncing: Boolean = false,
    val industryUpdateResult: TreeHoleIndustryUpdateResult? = null,
    val syncingCoins: Boolean = false,
    val rewardSyncResult: TreeHoleRewardSyncResult? = null,
    val likedPostIds: Set<String> = emptySet(),
    val cheeredPostIds: Set<String> = emptySet(),
    val criedPostIds: Set<String> = emptySet(),
    val blockedAuthors: List<BlockedTreeHoleAuthor> = emptyList(),
    val message: String? = null,
    val seenPostIds: Set<String> = emptySet(),
    val declinedIndustrySyncFor: String? = null,
    val pendingReactions: Set<TreeHoleReactionKey> = emptySet(),
    val showVoice: Boolean = false,
    val acceptingTerms: Boolean = false,
    val savingProfile: Boolean = false,
    val deletingAccount: Boolean = false,
    val reporting: Boolean = false,
    val failure: TreeHoleFeatureFailure? = null,
    val success: TreeHoleFeatureSuccess? = null,
) {
    val hasProfile: Boolean get() = profile?.isComplete == true
    val hasAcceptedCurrentTerms: Boolean get() = TreeHoleTermsPolicy.canAccess(acceptedTermsVersion)
    val canAccessCommunity: Boolean get() = hasProfile && hasAcceptedCurrentTerms
    val hasMorePosts: Boolean get() = !feedEndReached

    /** 行業是只在已載入的 server-side 貼文類型上再做 AND 過濾。 */
    val visibleFeedPosts: List<TreeHolePost>
        get() = TreeHoleBlockPolicy.filterPosts(
            posts = TreeHoleFeedPolicy.filter(
                posts = feedPosts,
                industries = selectedIndustries,
                postType = null,
            ),
            blockedAuthorKeys = blockedAuthors.map(BlockedTreeHoleAuthor::authorKey).toSet(),
        )

    /** 舊呼叫端過渡期別名。 */
    val filteredFeedPosts: List<TreeHolePost> get() = visibleFeedPosts

    val pendingIndustrySyncStatus: IndustrySyncStatusResponse?
        get() {
            val status = industrySyncStatus?.takeIf(IndustrySyncStatusResponse::eligible) ?: return null
            val currentIndustry = status.currentIndustry ?: return null
            val profileIndustry = profile?.industry ?: return null
            // 後端狀態可能是舊 profile 請求的結果；只有兩邊 canonical value
            // 完全相同時才能提示或更新，避免將貼文改到過期的行業。
            if (
                currentIndustry.isBlank() ||
                currentIndustry != profileIndustry ||
                currentIndustry == declinedIndustrySyncFor
            ) {
                return null
            }
            return status
        }

    /** 今日心聲：尚未讀過的已通過貼文（feed 本身只回傳 approved）。 */
    val voicePosts: List<TreeHolePost> get() = visibleFeedPosts.filter { it.id !in seenPostIds }

    val newAnswerCount: Int
        get() = TreeHoleQuestionActivityPolicy.newAnswerCount(questionActivity, seenAnswerCounts)

    val hasUnmarkedHelpfulAnswers: Boolean
        get() = questionActivity.any { activity ->
            activity.hasUnmarkedHelpfulAnswer &&
                activity.commentCount > seenAnswerCounts.getOrDefault(activity.postId, 0)
        }

    fun hasReacted(postId: String, type: TreeHoleReactionType): Boolean = when (type) {
        TreeHoleReactionType.LIKE -> postId in likedPostIds
        TreeHoleReactionType.CHEER -> postId in cheeredPostIds
        TreeHoleReactionType.CRY -> postId in criedPostIds
    }
}

private data class TreeHolePersistentPrimary(
    val profile: TreeHoleProfile?,
    val termsVersion: Int,
    val filters: Set<String>,
    val liked: Set<String>,
    val cheered: Set<String>,
)

private data class TreeHolePersistentSecondary(
    val cried: Set<String>,
    val seen: Set<String>,
    val declinedIndustry: String?,
    val seenAnswerCounts: Map<String, Int>,
    val blockedAuthors: List<BlockedTreeHoleAuthor>,
)

private data class TreeHoleAccessKey(
    val profile: TreeHoleProfile,
    val termsVersion: Int,
)

private data class TreeHoleRefreshSnapshot(
    val profileSync: Result<Boolean>,
    val myPosts: Result<List<TreeHolePost>>,
    val myComments: Result<List<MyTreeHoleComment>>,
    val questionQuota: Result<QuestionQuotaResponse?>,
    val answerStats: Result<TreeHoleAnswerStats>,
    val questionActivity: Result<List<QuestionActivity>>,
    val industrySyncStatus: Result<IndustrySyncStatusResponse?>,
)

class TreeHoleViewModel(
    private val repository: TreeHoleRepository = ServiceLocator.treeHoleRepository,
) : ViewModel() {
    private val localState = MutableStateFlow(TreeHoleUiState())
    val uiState: StateFlow<TreeHoleUiState> = localState

    private var feedJob: Job? = null
    private var refreshJob: Job? = null
    private var commentsJob: Job? = null
    private var authorPostsJob: Job? = null
    private var feedEpoch = 0L
    private var refreshEpoch = 0L
    private var commentsEpoch = 0L
    private var authorPostsEpoch = 0L
    private var activeAccessKey: TreeHoleAccessKey? = null

    /** compareAndSet 在 launch 前執行，阻擋同一事件迴圈內的連點重入。 */
    private val stampSubmissionGate = AtomicBoolean(false)
    private val questionSubmissionGate = AtomicBoolean(false)
    private val commentSubmissionGate = AtomicBoolean(false)
    private val industrySyncGate = AtomicBoolean(false)
    private val newAnswerOpenGate = AtomicBoolean(false)
    private val reportSubmissionGate = AtomicBoolean(false)
    private val blockAuthorGate = AtomicBoolean(false)
    private val reactionRequests = mutableSetOf<TreeHoleReactionKey>()
    private val helpfulRequests = mutableSetOf<String>()

    init {
        observePersistentRepositoryState()
    }

    private fun observePersistentRepositoryState() {
        val primary = combine(
            repository.profile,
            repository.acceptedTermsVersion,
            repository.filterIndustries,
            repository.likedPostIds,
            repository.cheeredPostIds,
        ) { profile, terms, filters, liked, cheered ->
            TreeHolePersistentPrimary(profile, terms, filters, liked, cheered)
        }
        val secondary = combine(
            repository.criedPostIds,
            repository.seenPostIds,
            repository.declinedIndustrySyncFor,
            repository.seenAnswerCounts,
            repository.blockedAuthors,
        ) { cried, seen, declined, seenAnswers, blockedAuthors ->
            TreeHolePersistentSecondary(cried, seen, declined, seenAnswers, blockedAuthors)
        }

        viewModelScope.launch {
            combine(primary, secondary) { first, second -> first to second }
                .collect { (first, second) ->
                    val knownIndustries = TreeHoleIndustryCatalog.values.toSet()
                    val normalizedFilters = first.filters.intersect(knownIndustries)
                    localState.update { current ->
                        val blockedKeys = second.blockedAuthors
                            .map(BlockedTreeHoleAuthor::authorKey)
                            .toSet()
                        val selectedPostBlocked = current.selectedPost?.let {
                            TreeHoleBlockPolicy.normalizeAuthorKey(it.authorKey) in blockedKeys
                        } == true
                        current.copy(
                            profile = first.profile,
                            acceptedTermsVersion = first.termsVersion,
                            selectedIndustries = normalizedFilters,
                            likedPostIds = first.liked,
                            cheeredPostIds = first.cheered,
                            criedPostIds = second.cried,
                            seenPostIds = second.seen,
                            declinedIndustrySyncFor = second.declinedIndustry,
                            seenAnswerCounts = second.seenAnswerCounts,
                            blockedAuthors = second.blockedAuthors,
                            comments = if (selectedPostBlocked) {
                                emptyList()
                            } else {
                                TreeHoleBlockPolicy.filterComments(current.comments, blockedKeys)
                            },
                            authorPosts = TreeHoleBlockPolicy.filterPosts(current.authorPosts, blockedKeys),
                            selectedPost = current.selectedPost?.takeUnless { selectedPostBlocked },
                            selectedAuthor = current.selectedAuthor?.takeUnless {
                                TreeHoleBlockPolicy.normalizeAuthorKey(it.authorKey) in blockedKeys
                            },
                        )
                    }

                    val nextAccessKey = first.profile
                        ?.takeIf(TreeHoleProfile::isComplete)
                        ?.takeIf { TreeHoleTermsPolicy.canAccess(first.termsVersion) }
                        ?.let { TreeHoleAccessKey(it, first.termsVersion) }

                    if (nextAccessKey == null) {
                        if (activeAccessKey != null) cancelAccessScopedWork()
                        activeAccessKey = null
                    } else if (nextAccessKey != activeAccessKey) {
                        activeAccessKey = nextAccessKey
                        refreshAll()
                    }
                }
        }
    }

    private fun cancelAccessScopedWork() {
        feedEpoch += 1
        refreshEpoch += 1
        commentsEpoch += 1
        authorPostsEpoch += 1
        feedJob?.cancel()
        refreshJob?.cancel()
        commentsJob?.cancel()
        authorPostsJob?.cancel()
        localState.update {
            it.copy(
                feedPosts = emptyList(),
                feedNextOffset = 0,
                feedEndReached = false,
                feedLoading = false,
                feedLoadingMore = false,
                myPosts = emptyList(),
                myComments = emptyList(),
                selectedPost = null,
                comments = emptyList(),
                commentsLoading = false,
                loading = false,
                refreshing = false,
                questionQuota = null,
                myAnswerExp = 0,
                myHelpfulAnswerCount = 0,
                questionActivity = emptyList(),
                pendingHelpfulCommentIds = emptySet(),
                selectedAuthor = null,
                authorPosts = emptyList(),
                authorPostsLoading = false,
                industrySyncStatus = null,
            )
        }
    }

    fun acceptCurrentTerms() {
        if (localState.value.acceptingTerms) return
        localState.update { it.copy(acceptingTerms = true, failure = null, success = null, message = null) }
        viewModelScope.launch {
            try {
                repository.acceptCurrentTerms()
                localState.update {
                    it.copy(
                        acceptingTerms = false,
                        success = TreeHoleFeatureSuccess(TreeHoleFeatureSuccessKind.TERMS_ACCEPTED),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                localState.update { it.copy(acceptingTerms = false) }
                publishFailure(TreeHoleFeatureOperation.ACCEPT_TERMS, error = error)
            }
        }
    }

    fun saveProfile(profile: TreeHoleProfile) {
        if (!profile.isComplete) {
            publishFailure(
                operation = TreeHoleFeatureOperation.SAVE_PROFILE,
                reason = TreeHoleFeatureFailureReason.PROFILE_INCOMPLETE,
            )
            return
        }
        if (localState.value.savingProfile) return
        localState.update { it.copy(savingProfile = true, failure = null, success = null, message = null) }
        viewModelScope.launch {
            try {
                val nicknameSync = repository.saveProfile(profile)
                localState.update {
                    it.copy(
                        savingProfile = false,
                        success = TreeHoleFeatureSuccess(
                            TreeHoleFeatureSuccessKind.PROFILE_SAVED,
                            affectedCount = nicknameSync?.totalUpdated?.takeIf { count -> count > 0 },
                        ),
                    )
                }
                if ((nicknameSync?.totalUpdated ?: 0) > 0) reloadFeed()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                localState.update { it.copy(savingProfile = false) }
                publishFailure(TreeHoleFeatureOperation.SAVE_PROFILE, error = error)
            }
        }
    }

    fun refreshAll() {
        val state = localState.value
        if (!state.canAccessCommunity) {
            publishFailure(
                operation = TreeHoleFeatureOperation.REFRESH,
                reason = TreeHoleFeatureFailureReason.TERMS_REQUIRED,
            )
            return
        }

        reloadFeed()
        refreshEpoch += 1
        val requestEpoch = refreshEpoch
        refreshJob?.cancel()
        localState.update {
            it.copy(
                loading = true,
                refreshing = true,
                refreshFailures = emptySet(),
                failure = null,
                message = null,
            )
        }

        refreshJob = viewModelScope.launch {
            try {
                val profile = localState.value.profile
                val profileSync = if (profile?.isComplete == true) {
                    captureResult { repository.syncProfile(profile) }
                } else {
                    Result.success(false)
                }

                if (profileSync.getOrNull() == true) {
                    val nicknameSync = captureResult { repository.syncMyNickname() }.getOrNull()
                    if ((nicknameSync?.totalUpdated ?: 0) > 0) reloadFeed()
                }

                val snapshot = supervisorScope {
                    val myPosts = async { captureResult { repository.fetchMyPosts() } }
                    val myComments = async { captureResult { repository.fetchMyComments() } }
                    val quota = async { captureResult { repository.fetchQuestionQuota() } }
                    val answerStats = async { captureResult { repository.fetchMyAnswerStats() } }
                    val questionActivity = async { captureResult { repository.fetchMyQuestionActivity() } }
                    // profile sync 已 await，後端才會以最新行業計算同步狀態。
                    val industryStatus = async { captureResult { repository.fetchIndustrySyncStatus() } }
                    TreeHoleRefreshSnapshot(
                        profileSync = profileSync,
                        myPosts = myPosts.await(),
                        myComments = myComments.await(),
                        questionQuota = quota.await(),
                        answerStats = answerStats.await(),
                        questionActivity = questionActivity.await(),
                        industrySyncStatus = industryStatus.await(),
                    )
                }

                if (requestEpoch != refreshEpoch) return@launch
                val failures = buildSet {
                    if (snapshot.profileSync.getOrNull() != true) add(TreeHoleRefreshSection.PROFILE_SYNC)
                    if (snapshot.myPosts.isFailure) add(TreeHoleRefreshSection.MY_POSTS)
                    if (snapshot.myComments.isFailure) add(TreeHoleRefreshSection.MY_COMMENTS)
                    if (snapshot.questionQuota.getOrNull() == null) add(TreeHoleRefreshSection.QUESTION_QUOTA)
                    if (snapshot.answerStats.isFailure) add(TreeHoleRefreshSection.ANSWER_STATS)
                    if (snapshot.questionActivity.isFailure) add(TreeHoleRefreshSection.QUESTION_ACTIVITY)
                    if (snapshot.industrySyncStatus.getOrNull() == null) {
                        add(TreeHoleRefreshSection.INDUSTRY_SYNC_STATUS)
                    }
                }

                localState.update { current ->
                    if (requestEpoch != refreshEpoch) return@update current
                    current.copy(
                        myPosts = snapshot.myPosts.getOrNull() ?: current.myPosts,
                        myComments = snapshot.myComments.getOrNull() ?: current.myComments,
                        questionQuota = snapshot.questionQuota.getOrNull() ?: current.questionQuota,
                        myAnswerExp = snapshot.answerStats.getOrNull()?.exp ?: current.myAnswerExp,
                        myHelpfulAnswerCount = snapshot.answerStats.getOrNull()?.helpfulCount
                            ?: current.myHelpfulAnswerCount,
                        questionActivity = snapshot.questionActivity.getOrNull()
                            ?: current.questionActivity,
                        industrySyncStatus = snapshot.industrySyncStatus.getOrNull()
                            ?: current.industrySyncStatus,
                        loading = false,
                        refreshing = false,
                        refreshFailures = failures,
                        failure = if (failures.isEmpty()) {
                            current.failure?.takeUnless {
                                it.operation == TreeHoleFeatureOperation.REFRESH
                            }
                        } else {
                            TreeHoleFeatureFailure(
                                operation = TreeHoleFeatureOperation.REFRESH,
                                reason = TreeHoleFeatureFailureReason.PARTIAL_REFRESH,
                                failedRefreshSections = failures,
                            )
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (requestEpoch == refreshEpoch) {
                    localState.update { it.copy(loading = false, refreshing = false) }
                    publishFailure(TreeHoleFeatureOperation.REFRESH, error = error)
                }
            }
        }
    }

    fun selectFeedMode(mode: TreeHoleFeedMode) {
        if (localState.value.feedMode == mode) return
        localState.update { it.copy(feedMode = mode) }
        reloadFeed()
    }

    fun reloadFeed() = startFeedRequest(reset = true)

    fun loadMoreFeed() = startFeedRequest(reset = false)

    private fun startFeedRequest(reset: Boolean) {
        val state = localState.value
        if (!state.canAccessCommunity) {
            publishFailure(
                operation = if (reset) TreeHoleFeatureOperation.RESET_FEED
                else TreeHoleFeatureOperation.LOAD_MORE_FEED,
                reason = TreeHoleFeatureFailureReason.TERMS_REQUIRED,
            )
            return
        }
        if (!reset && (feedJob?.isActive == true || state.feedEndReached)) return

        if (reset) {
            feedEpoch += 1
            feedJob?.cancel()
        }
        val requestEpoch = feedEpoch
        val requestMode = state.feedMode
        val offset = if (reset) 0 else state.feedNextOffset

        localState.update {
            it.copy(
                feedPosts = if (reset) emptyList() else it.feedPosts,
                feedNextOffset = if (reset) 0 else it.feedNextOffset,
                feedEndReached = if (reset) false else it.feedEndReached,
                feedLoading = reset,
                feedLoadingMore = !reset,
                failure = it.failure?.takeUnless { failure ->
                    failure.operation == TreeHoleFeatureOperation.RESET_FEED ||
                        failure.operation == TreeHoleFeatureOperation.LOAD_MORE_FEED
                },
                message = null,
            )
        }

        feedJob = viewModelScope.launch {
            try {
                val incoming = repository.fetchFeed(
                    offset = offset,
                    postType = requestMode.apiValue,
                    industry = null,
                    limit = DEFAULT_FEED_PAGE_SIZE,
                )
                if (requestEpoch != feedEpoch || localState.value.feedMode != requestMode) return@launch

                localState.update { current ->
                    if (requestEpoch != feedEpoch || current.feedMode != requestMode) return@update current
                    val base = TreeHoleFeedPage(
                        posts = if (reset) emptyList() else current.feedPosts,
                        nextOffset = if (reset) 0 else current.feedNextOffset,
                        endReached = false,
                    )
                    val merged = TreeHoleFeedPolicy.mergePage(
                        current = base,
                        incoming = incoming,
                        pageSize = DEFAULT_FEED_PAGE_SIZE,
                    )
                    current.copy(
                        feedPosts = merged.posts,
                        feedNextOffset = merged.nextOffset,
                        feedEndReached = merged.endReached,
                        feedLoading = false,
                        feedLoadingMore = false,
                    )
                }
                maybeAutoOpenVoice(requestEpoch, requestMode)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (requestEpoch == feedEpoch && localState.value.feedMode == requestMode) {
                    localState.update { it.copy(feedLoading = false, feedLoadingMore = false) }
                    publishFailure(
                        operation = if (reset) TreeHoleFeatureOperation.RESET_FEED
                        else TreeHoleFeatureOperation.LOAD_MORE_FEED,
                        error = error,
                    )
                }
            }
        }
    }

    private suspend fun maybeAutoOpenVoice(
        requestEpoch: Long,
        requestMode: TreeHoleFeedMode,
    ) {
        val state = localState.value
        if (requestEpoch != feedEpoch || state.feedMode != requestMode || state.voicePosts.isEmpty()) return
        val today = todayKey()
        if (repository.voiceLastOpenDate.first() == today) return
        repository.markVoiceOpened(today)
        localState.update { current ->
            if (requestEpoch == feedEpoch && current.feedMode == requestMode) {
                current.copy(showVoice = true)
            } else {
                current
            }
        }
    }

    fun setIndustryFilters(industries: Set<String>) {
        val normalized = TreeHoleIndustryCatalog.values.filter(industries::contains).toSet()
        val previous = localState.value.selectedIndustries
        if (normalized == previous) return
        localState.update { it.copy(selectedIndustries = normalized, failure = null, message = null) }
        viewModelScope.launch {
            try {
                repository.saveFilterIndustries(normalized)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                localState.update { current ->
                    if (current.selectedIndustries == normalized) {
                        current.copy(selectedIndustries = previous)
                    } else {
                        current
                    }
                }
                publishFailure(TreeHoleFeatureOperation.SAVE_INDUSTRY_FILTER, error = error)
            }
        }
    }

    fun clearIndustryFilters() = setIndustryFilters(emptySet())

    fun openVoice() = localState.update { it.copy(showVoice = true) }

    fun closeVoice() = localState.update { it.copy(showVoice = false) }

    /** 翻卡：右滑讚 like、左滑加油 cheer、上滑哭 cry、下滑跳過（reactionType = null）。 */
    fun voiceSwipe(post: TreeHolePost, reactionType: String?) {
        reactionType?.let { react(post.id, it) }
        markVoicePostSeen(post.id)
    }

    fun markVoicePostSeen(postId: String) {
        val normalizedId = postId.trim()
        if (normalizedId.isEmpty()) return
        val wasSeen = normalizedId in localState.value.seenPostIds
        if (!wasSeen) {
            localState.update { it.copy(seenPostIds = it.seenPostIds + normalizedId) }
        }
        viewModelScope.launch {
            try {
                repository.markPostSeen(normalizedId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (!wasSeen) {
                    localState.update { it.copy(seenPostIds = it.seenPostIds - normalizedId) }
                }
                publishFailure(TreeHoleFeatureOperation.MARK_SEEN, error = error)
            }
        }
    }

    /** 現行 UI 相容入口；新呼叫點應傳蓋章當下的 round/stamp snapshot。 */
    fun submit(content: String) {
        val trimmed = content.trim()
        if (!validateLength(trimmed, STAMP_MIN_LENGTH, POST_MAX_LENGTH, TreeHoleFeatureOperation.SUBMIT_STAMP_POST)) {
            return
        }
        launchStampSubmission {
            @Suppress("DEPRECATION")
            repository.submitPost(trimmed)
        }
    }

    fun submitStampPost(
        content: String,
        roundNumber: Int,
        stampCount: Int,
    ) {
        val trimmed = content.trim()
        if (!validateLength(trimmed, STAMP_MIN_LENGTH, POST_MAX_LENGTH, TreeHoleFeatureOperation.SUBMIT_STAMP_POST)) {
            return
        }
        if (roundNumber <= 0 || stampCount <= 0) {
            publishFailure(
                operation = TreeHoleFeatureOperation.SUBMIT_STAMP_POST,
                reason = TreeHoleFeatureFailureReason.INVALID_STAMP_CONTEXT,
            )
            return
        }
        launchStampSubmission {
            repository.submitPost(trimmed, roundNumber, stampCount)
        }
    }

    private fun launchStampSubmission(submitCall: suspend () -> Unit) {
        if (!stampSubmissionGate.compareAndSet(false, true)) return
        localState.update { it.copy(posting = true, failure = null, success = null, message = null) }
        viewModelScope.launch {
            try {
                submitCall()
                val mine = captureResult { repository.fetchMyPosts() }.getOrNull()
                val comments = captureResult { repository.fetchMyComments() }.getOrNull()
                localState.update {
                    it.copy(
                        myPosts = mine ?: it.myPosts,
                        myComments = comments ?: it.myComments,
                        success = TreeHoleFeatureSuccess(TreeHoleFeatureSuccessKind.STAMP_POST_SUBMITTED),
                    )
                }
                com.neilyin.nnowork.analytics.Analytics.log("treehole_post_created")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.SUBMIT_STAMP_POST, error = error)
            } finally {
                stampSubmissionGate.set(false)
                localState.update { it.copy(posting = false) }
            }
        }
    }

    /**
     * 回傳 false 代表已有提問正在送出；旗標與 state 都在 launch 前同步設定。
     */
    fun submitQuestion(content: String): Boolean {
        if (!questionSubmissionGate.compareAndSet(false, true)) return false
        localState.update {
            it.copy(
                submittingQuestion = true,
                questionSubmissionResult = null,
                failure = null,
                success = null,
                message = null,
            )
        }

        val trimmed = content.trim()
        if (trimmed.length !in QUESTION_MIN_LENGTH..POST_MAX_LENGTH) {
            questionSubmissionGate.set(false)
            localState.update {
                it.copy(
                    submittingQuestion = false,
                    questionSubmissionResult = SubmitQuestionResult.InvalidLength,
                )
            }
            return true
        }

        viewModelScope.launch {
            try {
                val result = repository.submitQuestion(trimmed)
                var refreshedMine: List<TreeHolePost>? = null
                var refreshedQuota: QuestionQuotaResponse? = null
                if (result is SubmitQuestionResult.Submitted) {
                    refreshedMine = captureResult { repository.fetchMyPosts() }.getOrNull()
                } else {
                    refreshedQuota = captureResult { repository.fetchQuestionQuota() }.getOrNull()
                }
                localState.update { current ->
                    current.copy(
                        myPosts = refreshedMine ?: current.myPosts,
                        questionQuota = refreshedQuota ?: current.questionQuota.updatedFor(result),
                        questionSubmissionResult = result,
                        success = if (result is SubmitQuestionResult.Submitted) {
                            TreeHoleFeatureSuccess(TreeHoleFeatureSuccessKind.QUESTION_SUBMITTED)
                        } else {
                            current.success
                        },
                    )
                }
                if (result is SubmitQuestionResult.Submitted) {
                    com.neilyin.nnowork.analytics.Analytics.log("treehole_question_created")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.SUBMIT_QUESTION, error = error)
            } finally {
                questionSubmissionGate.set(false)
                localState.update { it.copy(submittingQuestion = false) }
            }
        }
        return true
    }

    fun clearQuestionSubmissionResult() {
        localState.update { it.copy(questionSubmissionResult = null) }
    }

    fun delete(postId: String) = deletePost(postId)

    fun deletePost(postId: String) {
        val normalizedId = postId.trim()
        if (normalizedId.isEmpty()) {
            publishFailure(
                operation = TreeHoleFeatureOperation.DELETE_POST,
                reason = TreeHoleFeatureFailureReason.INVALID_IDENTIFIER,
            )
            return
        }
        val deletedPostWasQuestion = localState.value.let { state ->
            (state.myPosts + state.feedPosts + listOfNotNull(state.selectedPost))
                .firstOrNull { it.id == normalizedId }
                ?.isQuestion == true
        }
        viewModelScope.launch {
            try {
                if (!repository.deleteMyPost(normalizedId)) {
                    publishFailure(
                        operation = TreeHoleFeatureOperation.DELETE_POST,
                        reason = TreeHoleFeatureFailureReason.SERVER_REJECTED,
                    )
                    return@launch
                }
                // 額度只採信後端重新計算，不在本機因刪除（包含 rejected）
                // 自行加回。若刷新失敗則清掉 cache，讓下次送出由 server 把關。
                val refreshedQuestionQuota = if (deletedPostWasQuestion) {
                    captureResult { repository.fetchQuestionQuota() }.getOrNull()
                } else {
                    null
                }
                localState.update {
                    it.copy(
                        feedPosts = it.feedPosts.filterNot { post -> post.id == normalizedId },
                        myPosts = it.myPosts.filterNot { post -> post.id == normalizedId },
                        questionActivity = it.questionActivity.filterNot { activity ->
                            activity.postId == normalizedId
                        },
                        selectedPost = it.selectedPost?.takeUnless { post -> post.id == normalizedId },
                        comments = if (it.selectedPost?.id == normalizedId) emptyList() else it.comments,
                        questionQuota = if (deletedPostWasQuestion) {
                            refreshedQuestionQuota
                        } else {
                            it.questionQuota
                        },
                        success = TreeHoleFeatureSuccess(TreeHoleFeatureSuccessKind.POST_DELETED, 1),
                        failure = null,
                        message = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.DELETE_POST, error = error)
            }
        }
    }

    fun deleteComment(commentId: String) {
        val normalizedId = commentId.trim()
        if (normalizedId.isEmpty()) {
            publishFailure(
                operation = TreeHoleFeatureOperation.DELETE_COMMENT,
                reason = TreeHoleFeatureFailureReason.INVALID_IDENTIFIER,
            )
            return
        }
        viewModelScope.launch {
            try {
                if (!repository.deleteMyComment(normalizedId)) {
                    publishFailure(
                        operation = TreeHoleFeatureOperation.DELETE_COMMENT,
                        reason = TreeHoleFeatureFailureReason.SERVER_REJECTED,
                    )
                    return@launch
                }
                localState.update {
                    it.copy(
                        myComments = it.myComments.filterNot { comment -> comment.id == normalizedId },
                        comments = it.comments.filterNot { comment -> comment.id == normalizedId },
                        success = TreeHoleFeatureSuccess(TreeHoleFeatureSuccessKind.COMMENT_DELETED, 1),
                        failure = null,
                        message = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.DELETE_COMMENT, error = error)
            }
        }
    }

    fun submitReport(
        targetType: TreeHoleReportTargetType,
        targetId: String,
        reason: TreeHoleReportReason,
        note: String,
        alsoBlock: Boolean,
        authorKey: String?,
        nickname: String,
    ) {
        val normalizedTargetId = targetId.trim()
        if (normalizedTargetId.isEmpty()) {
            publishFailure(
                operation = TreeHoleFeatureOperation.REPORT_CONTENT,
                reason = TreeHoleFeatureFailureReason.INVALID_IDENTIFIER,
            )
            return
        }
        if (!reportSubmissionGate.compareAndSet(false, true)) return
        localState.update {
            it.copy(reporting = true, failure = null, success = null, message = null)
        }
        viewModelScope.launch {
            try {
                when (repository.submitReport(targetType, normalizedTargetId, reason, note)) {
                    SubmitReportResult.Submitted -> {
                        val didBlock = if (alsoBlock) {
                            authorKey?.let { repository.blockAuthor(it, nickname) } == true
                        } else {
                            false
                        }
                        localState.update {
                            it.copy(
                                success = TreeHoleFeatureSuccess(
                                    if (didBlock) {
                                        TreeHoleFeatureSuccessKind.REPORT_SUBMITTED_AND_BLOCKED
                                    } else {
                                        TreeHoleFeatureSuccessKind.REPORT_SUBMITTED
                                    },
                                ),
                            )
                        }
                        Analytics.log("treehole_report_submitted", "target_type" to targetType.apiValue)
                        if (didBlock) Analytics.log("treehole_author_blocked", "source" to "report")
                    }
                    SubmitReportResult.TargetNotFound -> publishFailure(
                        operation = TreeHoleFeatureOperation.REPORT_CONTENT,
                        reason = TreeHoleFeatureFailureReason.REPORT_TARGET_NOT_FOUND,
                    )
                    SubmitReportResult.RateLimited -> publishFailure(
                        operation = TreeHoleFeatureOperation.REPORT_CONTENT,
                        reason = TreeHoleFeatureFailureReason.REPORT_RATE_LIMITED,
                    )
                    is SubmitReportResult.Rejected -> publishFailure(
                        operation = TreeHoleFeatureOperation.REPORT_CONTENT,
                        reason = TreeHoleFeatureFailureReason.SERVER_REJECTED,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.REPORT_CONTENT, error = error)
            } finally {
                reportSubmissionGate.set(false)
                localState.update { it.copy(reporting = false) }
            }
        }
    }

    fun blockAuthor(authorKey: String?, nickname: String) {
        val normalizedKey = TreeHoleBlockPolicy.normalizeAuthorKey(authorKey)
        if (normalizedKey == null) {
            publishFailure(
                operation = TreeHoleFeatureOperation.BLOCK_AUTHOR,
                reason = TreeHoleFeatureFailureReason.BLOCK_UNAVAILABLE,
            )
            return
        }
        if (!blockAuthorGate.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                if (!repository.blockAuthor(normalizedKey, nickname)) {
                    publishFailure(
                        operation = TreeHoleFeatureOperation.BLOCK_AUTHOR,
                        reason = TreeHoleFeatureFailureReason.BLOCK_UNAVAILABLE,
                    )
                    return@launch
                }
                localState.update {
                    it.copy(
                        success = TreeHoleFeatureSuccess(TreeHoleFeatureSuccessKind.AUTHOR_BLOCKED),
                        failure = null,
                        message = null,
                    )
                }
                Analytics.log("treehole_author_blocked", "source" to "direct")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.BLOCK_AUTHOR, error = error)
            } finally {
                blockAuthorGate.set(false)
            }
        }
    }

    fun deleteAccount() {
        if (localState.value.deletingAccount) return
        localState.update {
            it.copy(
                deletingAccount = true,
                loading = true,
                failure = null,
                success = null,
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                if (!repository.deleteMyAccount()) {
                    localState.update { it.copy(deletingAccount = false, loading = false) }
                    publishFailure(
                        operation = TreeHoleFeatureOperation.DELETE_ACCOUNT,
                        reason = TreeHoleFeatureFailureReason.SERVER_REJECTED,
                    )
                    return@launch
                }
                cancelAccessScopedWork()
                localState.update {
                    it.copy(
                        deletingAccount = false,
                        loading = false,
                        success = TreeHoleFeatureSuccess(TreeHoleFeatureSuccessKind.ACCOUNT_DELETED),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                localState.update { it.copy(deletingAccount = false, loading = false) }
                publishFailure(TreeHoleFeatureOperation.DELETE_ACCOUNT, error = error)
            }
        }
    }

    fun openPost(post: TreeHolePost) {
        if (localState.value.submittingComment) return
        markQuestionAnswersSeen(post.id)
        com.neilyin.nnowork.analytics.Analytics.log("treehole_post_viewed")
        commentsEpoch += 1
        val requestEpoch = commentsEpoch
        commentsJob?.cancel()
        localState.update {
            it.copy(
                selectedPost = post,
                commentsLoading = true,
                comments = emptyList(),
                failure = null,
                success = null,
                message = null,
            )
        }
        commentsJob = viewModelScope.launch {
            try {
                val comments = repository.fetchComments(post.id)
                localState.update { current ->
                    if (requestEpoch == commentsEpoch && current.selectedPost?.id == post.id) {
                        current.copy(comments = comments, commentsLoading = false)
                    } else {
                        current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (requestEpoch == commentsEpoch && localState.value.selectedPost?.id == post.id) {
                    localState.update { it.copy(commentsLoading = false) }
                    publishFailure(TreeHoleFeatureOperation.LOAD_COMMENTS, error = error)
                }
            }
        }
    }

    fun openNewestQuestionWithAnswers() {
        val snapshot = localState.value
        val activity = TreeHoleQuestionActivityPolicy.newestWithNewAnswers(
            activity = snapshot.questionActivity,
            seenCounts = snapshot.seenAnswerCounts,
        ) ?: return
        val cachedPost = snapshot.myPosts.firstOrNull { it.id == activity.postId }
        if (cachedPost != null) {
            openPost(cachedPost)
            return
        }
        if (!newAnswerOpenGate.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                val refreshedMine = repository.fetchMyPosts()
                val target = refreshedMine.firstOrNull { it.id == activity.postId }
                localState.update { it.copy(myPosts = refreshedMine) }
                if (target == null) {
                    publishFailure(
                        operation = TreeHoleFeatureOperation.OPEN_NEW_ANSWER,
                        reason = TreeHoleFeatureFailureReason.SERVER_REJECTED,
                    )
                } else {
                    openPost(target)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.OPEN_NEW_ANSWER, error = error)
            } finally {
                newAnswerOpenGate.set(false)
            }
        }
    }

    private fun markQuestionAnswersSeen(postId: String) {
        val activity = localState.value.questionActivity.firstOrNull { it.postId == postId } ?: return
        val currentCount = activity.commentCount.coerceAtLeast(0)
        localState.update {
            it.copy(seenAnswerCounts = it.seenAnswerCounts + (postId to currentCount))
        }
        viewModelScope.launch {
            try {
                repository.markAnswersSeen(postId, currentCount)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // 已讀是本機提示狀態；不讓 DataStore 短暫失敗擋住留言內容。
            }
        }
    }

    fun closePost() {
        if (localState.value.submittingComment) return
        commentsEpoch += 1
        commentsJob?.cancel()
        localState.update {
            it.copy(
                selectedPost = null,
                comments = emptyList(),
                commentsLoading = false,
                submittingComment = false,
            )
        }
    }

    fun openAuthor(
        nickname: String,
        industry: String?,
        authorKey: String?,
    ) {
        val normalizedNickname = nickname.trim()
        val normalizedAuthorKey = TreeHoleBlockPolicy.normalizeAuthorKey(authorKey)
        if (normalizedNickname.isEmpty() && normalizedAuthorKey == null) {
            publishFailure(
                operation = TreeHoleFeatureOperation.LOAD_AUTHOR_POSTS,
                reason = TreeHoleFeatureFailureReason.INVALID_IDENTIFIER,
            )
            return
        }
        if (normalizedAuthorKey != null && localState.value.blockedAuthors.any {
                it.authorKey == normalizedAuthorKey
            }
        ) {
            return
        }
        val ref = TreeHoleAuthorRef(
            nickname = normalizedNickname.ifEmpty { "職場倖存者" },
            industry = industry,
            authorKey = normalizedAuthorKey,
        )
        startAuthorPostsRequest(ref)
    }

    fun retryAuthorPosts() {
        localState.value.selectedAuthor?.let(::startAuthorPostsRequest)
    }

    fun closeAuthorPosts() {
        authorPostsEpoch += 1
        authorPostsJob?.cancel()
        localState.update {
            it.copy(
                selectedAuthor = null,
                authorPosts = emptyList(),
                authorPostsLoading = false,
                failure = it.failure?.takeUnless { failure ->
                    failure.operation == TreeHoleFeatureOperation.LOAD_AUTHOR_POSTS
                },
            )
        }
    }

    private fun startAuthorPostsRequest(ref: TreeHoleAuthorRef) {
        authorPostsEpoch += 1
        val requestEpoch = authorPostsEpoch
        authorPostsJob?.cancel()
        localState.update {
            it.copy(
                selectedAuthor = ref,
                authorPosts = emptyList(),
                authorPostsLoading = true,
                failure = null,
                success = null,
                message = null,
            )
        }
        authorPostsJob = viewModelScope.launch {
            try {
                val posts = if (ref.authorKey != null) {
                    repository.fetchPostsByAuthor(ref.authorKey)
                } else {
                    repository.fetchPostsByNicknameFallback(ref.nickname)
                }
                localState.update { current ->
                    if (requestEpoch == authorPostsEpoch && current.selectedAuthor == ref) {
                        current.copy(authorPosts = posts, authorPostsLoading = false)
                    } else {
                        current
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (requestEpoch == authorPostsEpoch && localState.value.selectedAuthor == ref) {
                    localState.update { it.copy(authorPostsLoading = false) }
                    publishFailure(TreeHoleFeatureOperation.LOAD_AUTHOR_POSTS, error = error)
                }
            }
        }
    }

    /**
     * 回傳 true 表示內容與重入 gate 已通過，請求已被 ViewModel 接受。
     * 草稿仍要等 [TreeHoleFeatureSuccessKind.COMMENT_SUBMITTED] 才能清空。
     */
    fun submitComment(content: String): Boolean {
        val post = localState.value.selectedPost
        if (post == null) {
            publishFailure(
                operation = TreeHoleFeatureOperation.SUBMIT_COMMENT,
                reason = TreeHoleFeatureFailureReason.NO_SELECTED_POST,
            )
            return false
        }
        val trimmed = content.trim()
        if (!validateLength(
                content = trimmed,
                minLength = COMMENT_MIN_LENGTH,
                maxLength = TREE_HOLE_COMMENT_MAX_LENGTH,
                operation = TreeHoleFeatureOperation.SUBMIT_COMMENT,
            )
        ) {
            return false
        }
        if (!commentSubmissionGate.compareAndSet(false, true)) return false
        localState.update {
            it.copy(
                submittingComment = true,
                commentsLoading = true,
                failure = null,
                success = null,
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                when (repository.submitComment(post.id, trimmed)) {
                    SubmitCommentResult.Submitted -> Unit
                    SubmitCommentResult.InvalidLength -> {
                        publishFailure(
                            operation = TreeHoleFeatureOperation.SUBMIT_COMMENT,
                            reason = TreeHoleFeatureFailureReason.INVALID_CONTENT_LENGTH,
                            minLength = COMMENT_MIN_LENGTH,
                            maxLength = TREE_HOLE_COMMENT_MAX_LENGTH,
                        )
                        return@launch
                    }
                    SubmitCommentResult.RateLimited -> {
                        publishFailure(
                            operation = TreeHoleFeatureOperation.SUBMIT_COMMENT,
                            reason = TreeHoleFeatureFailureReason.COMMENT_RATE_LIMITED,
                        )
                        return@launch
                    }
                    SubmitCommentResult.Banned -> {
                        publishFailure(
                            operation = TreeHoleFeatureOperation.SUBMIT_COMMENT,
                            reason = TreeHoleFeatureFailureReason.AUTHOR_BANNED,
                        )
                        return@launch
                    }
                    is SubmitCommentResult.Rejected -> {
                        publishFailure(
                            operation = TreeHoleFeatureOperation.SUBMIT_COMMENT,
                            reason = TreeHoleFeatureFailureReason.SERVER_REJECTED,
                        )
                        return@launch
                    }
                }
                val comments = captureResult { repository.fetchComments(post.id) }.getOrNull()
                val mine = captureResult { repository.fetchMyComments() }.getOrNull()
                localState.update { current ->
                    current.copy(
                        comments = if (current.selectedPost?.id == post.id) {
                            comments ?: current.comments
                        } else {
                            current.comments
                        },
                        myComments = mine ?: current.myComments,
                        success = TreeHoleFeatureSuccess(TreeHoleFeatureSuccessKind.COMMENT_SUBMITTED),
                    )
                }
                com.neilyin.nnowork.analytics.Analytics.log("treehole_comment_submitted")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.SUBMIT_COMMENT, error = error)
            } finally {
                commentSubmissionGate.set(false)
                localState.update {
                    it.copy(
                        submittingComment = false,
                        commentsLoading = if (it.selectedPost?.id == post.id) false else it.commentsLoading,
                    )
                }
            }
        }
        return true
    }

    fun toggleCommentHelpful(commentId: String) {
        val normalizedId = commentId.trim()
        val snapshot = localState.value
        val post = snapshot.selectedPost
        val comment = snapshot.comments.firstOrNull { it.id == normalizedId }
        val ownsQuestion = post != null && post.isQuestion && snapshot.myPosts.any { it.id == post.id }
        if (normalizedId.isEmpty() || comment == null || !ownsQuestion) {
            publishFailure(
                operation = TreeHoleFeatureOperation.MARK_COMMENT_HELPFUL,
                reason = if (normalizedId.isEmpty()) {
                    TreeHoleFeatureFailureReason.INVALID_IDENTIFIER
                } else {
                    TreeHoleFeatureFailureReason.SERVER_REJECTED
                },
            )
            return
        }
        synchronized(helpfulRequests) {
            if (!helpfulRequests.add(normalizedId)) return
        }
        val requestedHelpful = !comment.isHelpful
        localState.update {
            it.copy(
                pendingHelpfulCommentIds = it.pendingHelpfulCommentIds + normalizedId,
                failure = null,
                success = null,
                message = null,
            )
        }

        viewModelScope.launch {
            try {
                when (val result = repository.markCommentHelpful(normalizedId, requestedHelpful)) {
                    is MarkCommentHelpfulResult.Updated -> {
                        val optimistic = localState.value.comments.map { row ->
                            if (row.id == normalizedId) row.copy(isHelpful = result.isHelpful) else row
                        }
                        val refreshed = captureResult { repository.fetchComments(post.id) }.getOrNull()
                        val stats = captureResult { repository.fetchMyAnswerStats() }.getOrNull()
                        val activity = captureResult { repository.fetchMyQuestionActivity() }.getOrNull()
                        localState.update { current ->
                            current.copy(
                                comments = if (current.selectedPost?.id == post.id) {
                                    refreshed ?: optimistic
                                } else {
                                    current.comments
                                },
                                myAnswerExp = stats?.exp ?: current.myAnswerExp,
                                myHelpfulAnswerCount = stats?.helpfulCount
                                    ?: current.myHelpfulAnswerCount,
                                questionActivity = activity ?: current.questionActivity,
                                success = TreeHoleFeatureSuccess(
                                    TreeHoleFeatureSuccessKind.HELPFUL_ANSWER_UPDATED,
                                ),
                            )
                        }
                    }
                    is MarkCommentHelpfulResult.Rejected -> publishFailure(
                        operation = TreeHoleFeatureOperation.MARK_COMMENT_HELPFUL,
                        reason = TreeHoleFeatureFailureReason.SERVER_REJECTED,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.MARK_COMMENT_HELPFUL, error = error)
            } finally {
                synchronized(helpfulRequests) { helpfulRequests.remove(normalizedId) }
                localState.update {
                    it.copy(pendingHelpfulCommentIds = it.pendingHelpfulCommentIds - normalizedId)
                }
            }
        }
    }

    fun react(postId: String, reactionType: String) {
        val type = TreeHoleReactionType.fromApiValue(reactionType)
        if (type == null) {
            publishFailure(
                operation = TreeHoleFeatureOperation.REACTION,
                reason = TreeHoleFeatureFailureReason.UNSUPPORTED_REACTION,
            )
            return
        }
        react(postId, type)
    }

    fun react(postId: String, reactionType: TreeHoleReactionType) {
        val normalizedId = postId.trim()
        if (normalizedId.isEmpty()) {
            publishFailure(
                operation = TreeHoleFeatureOperation.REACTION,
                reason = TreeHoleFeatureFailureReason.INVALID_IDENTIFIER,
            )
            return
        }
        val key = TreeHoleReactionKey(normalizedId, reactionType)
        synchronized(reactionRequests) {
            if (!reactionRequests.add(key)) return
        }

        val reactionState = localState.value
        val wasSelected = reactionState.hasReacted(normalizedId, reactionType)
        val originalCount = reactionState.reactionCount(normalizedId, reactionType)
        val optimisticSelected = !wasSelected
        val optimisticDelta = if (optimisticSelected) 1 else -1
        localState.update {
            it.withReaction(
                postId = normalizedId,
                type = reactionType,
                selected = optimisticSelected,
                countDelta = optimisticDelta,
            ).copy(
                pendingReactions = it.pendingReactions + key,
                failure = null,
                message = null,
            )
        }

        viewModelScope.launch {
            try {
                val action = repository.reactPost(normalizedId, reactionType.apiValue)
                val serverSelected = when (action) {
                    "added" -> true
                    "removed" -> false
                    else -> null
                }
                if (serverSelected == null) {
                    localState.update {
                        it.withReactionSnapshot(
                            postId = normalizedId,
                            type = reactionType,
                            selected = wasSelected,
                            count = originalCount,
                        )
                    }
                    publishFailure(
                        operation = TreeHoleFeatureOperation.REACTION,
                        reason = TreeHoleFeatureFailureReason.SERVER_REJECTED,
                    )
                    return@launch
                }
                if (serverSelected != optimisticSelected) {
                    val serverCount = originalCount?.let { count ->
                        (count + if (serverSelected) 1 else -1).coerceAtLeast(0)
                    }
                    localState.update {
                        it.withReactionSnapshot(
                            postId = normalizedId,
                            type = reactionType,
                            selected = serverSelected,
                            count = serverCount,
                        )
                    }
                }
                com.neilyin.nnowork.analytics.Analytics.log(
                    "treehole_reaction_sent",
                    "reaction" to reactionType.apiValue,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                localState.update {
                    it.withReactionSnapshot(
                        postId = normalizedId,
                        type = reactionType,
                        selected = wasSelected,
                        count = originalCount,
                    )
                }
                publishFailure(TreeHoleFeatureOperation.REACTION, error = error)
            } finally {
                synchronized(reactionRequests) { reactionRequests.remove(key) }
                localState.update { it.copy(pendingReactions = it.pendingReactions - key) }
            }
        }
    }

    fun syncCoinsAndApprovals() {
        if (localState.value.syncingCoins) return
        if (!localState.value.canAccessCommunity) {
            publishFailure(
                operation = TreeHoleFeatureOperation.SYNC_COINS_AND_APPROVALS,
                reason = TreeHoleFeatureFailureReason.TERMS_REQUIRED,
            )
            return
        }
        localState.update {
            it.copy(
                syncingCoins = true,
                rewardSyncResult = null,
                failure = null,
                success = null,
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                val approvals = repository.checkNewApprovals()
                val feedbackRewards = repository.checkNewFeedbackRewards()
                val balance = repository.syncCoinBalance()
                if (approvals > 0) {
                    com.neilyin.nnowork.analytics.Analytics.log(
                        "coin_reward_claimed",
                        "amount" to (approvals * 5).toString(),
                        "type" to "treehole_post_approved",
                    )
                }
                if (feedbackRewards > 0) {
                    com.neilyin.nnowork.analytics.Analytics.log(
                        "coin_reward_claimed",
                        "amount" to (feedbackRewards * 10).toString(),
                        "type" to "feedback_reward",
                    )
                }
                localState.update {
                    it.copy(
                        syncingCoins = false,
                        rewardSyncResult = TreeHoleRewardSyncResult(
                            newApprovals = approvals,
                            newFeedbackRewards = feedbackRewards,
                            coinBalance = balance,
                        ),
                        success = TreeHoleFeatureSuccess(
                            TreeHoleFeatureSuccessKind.COINS_AND_APPROVALS_SYNCED,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                localState.update { it.copy(syncingCoins = false) }
                publishFailure(TreeHoleFeatureOperation.SYNC_COINS_AND_APPROVALS, error = error)
            }
        }
    }

    fun updateUnsetIndustryPosts() = updateIndustryPosts(includeMismatched = false)

    fun updateAllMismatchedIndustryPosts() = updateIndustryPosts(includeMismatched = true)

    private fun updateIndustryPosts(includeMismatched: Boolean) {
        val status = localState.value.pendingIndustrySyncStatus
        if (status == null) {
            publishFailure(
                operation = TreeHoleFeatureOperation.UPDATE_INDUSTRY,
                reason = TreeHoleFeatureFailureReason.NO_PENDING_INDUSTRY_SYNC,
            )
            return
        }
        if (!industrySyncGate.compareAndSet(false, true)) return
        localState.update {
            it.copy(
                industrySyncing = true,
                industryUpdateResult = null,
                failure = null,
                success = null,
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                val response = repository.updateMyPostsIndustry(includeMismatched)
                if (!response.success) {
                    publishFailure(
                        operation = TreeHoleFeatureOperation.UPDATE_INDUSTRY,
                        reason = TreeHoleFeatureFailureReason.SERVER_REJECTED,
                    )
                    return@launch
                }
                repository.setDeclinedIndustrySyncFor(null)
                val mine = captureResult { repository.fetchMyPosts() }.getOrNull()
                val refreshedStatus = captureResult { repository.fetchIndustrySyncStatus() }.getOrNull()
                localState.update {
                    it.copy(
                        myPosts = mine ?: it.myPosts,
                        industrySyncStatus = refreshedStatus,
                        declinedIndustrySyncFor = null,
                        industryUpdateResult = TreeHoleIndustryUpdateResult(
                            includeMismatched = includeMismatched,
                            updatedCount = response.updated.coerceAtLeast(0),
                            industry = response.industry ?: status.currentIndustry,
                        ),
                        success = TreeHoleFeatureSuccess(
                            kind = TreeHoleFeatureSuccessKind.INDUSTRY_UPDATED,
                            affectedCount = response.updated.coerceAtLeast(0),
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                publishFailure(TreeHoleFeatureOperation.UPDATE_INDUSTRY, error = error)
            } finally {
                industrySyncGate.set(false)
                localState.update { it.copy(industrySyncing = false) }
            }
        }
    }

    fun declineCurrentIndustrySync() {
        val currentIndustry = localState.value.pendingIndustrySyncStatus
            ?.currentIndustry
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        if (currentIndustry == null) {
            publishFailure(
                operation = TreeHoleFeatureOperation.DECLINE_INDUSTRY_UPDATE,
                reason = TreeHoleFeatureFailureReason.NO_PENDING_INDUSTRY_SYNC,
            )
            return
        }
        val previous = localState.value.declinedIndustrySyncFor
        localState.update {
            it.copy(
                declinedIndustrySyncFor = currentIndustry,
                failure = null,
                success = TreeHoleFeatureSuccess(TreeHoleFeatureSuccessKind.INDUSTRY_UPDATE_DECLINED),
                message = null,
            )
        }
        viewModelScope.launch {
            try {
                repository.setDeclinedIndustrySyncFor(currentIndustry)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                localState.update { current ->
                    if (current.declinedIndustrySyncFor == currentIndustry) {
                        current.copy(declinedIndustrySyncFor = previous)
                    } else {
                        current
                    }
                }
                publishFailure(TreeHoleFeatureOperation.DECLINE_INDUSTRY_UPDATE, error = error)
            }
        }
    }

    fun clearTransientFeedback() {
        localState.update {
            it.copy(
                failure = null,
                success = null,
                message = null,
                industryUpdateResult = null,
                rewardSyncResult = null,
            )
        }
    }

    private fun validateLength(
        content: String,
        minLength: Int,
        maxLength: Int,
        operation: TreeHoleFeatureOperation,
    ): Boolean {
        if (content.length in minLength..maxLength) return true
        publishFailure(
            operation = operation,
            reason = TreeHoleFeatureFailureReason.INVALID_CONTENT_LENGTH,
            minLength = minLength,
            maxLength = maxLength,
        )
        return false
    }

    private fun publishFailure(
        operation: TreeHoleFeatureOperation,
        reason: TreeHoleFeatureFailureReason = TreeHoleFeatureFailureReason.REQUEST_FAILED,
        minLength: Int? = null,
        maxLength: Int? = null,
        error: Throwable? = null,
    ) {
        val resolvedReason = if (
            reason == TreeHoleFeatureFailureReason.REQUEST_FAILED &&
            error is TreeHoleAuthorBannedException
        ) {
            TreeHoleFeatureFailureReason.AUTHOR_BANNED
        } else {
            reason
        }
        localState.update {
            it.copy(
                failure = TreeHoleFeatureFailure(
                    operation = operation,
                    reason = resolvedReason,
                    minLength = minLength,
                    maxLength = maxLength,
                    causeType = error?.javaClass?.simpleName,
                ),
                success = null,
                message = null,
            )
        }
    }

    private suspend fun <T> captureResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())

    private companion object {
        const val STAMP_MIN_LENGTH = 5
        const val QUESTION_MIN_LENGTH = 5
        const val POST_MAX_LENGTH = 200
        const val COMMENT_MIN_LENGTH = 1
    }
}

private fun QuestionQuotaResponse?.updatedFor(result: SubmitQuestionResult): QuestionQuotaResponse? =
    when (result) {
        is SubmitQuestionResult.Submitted -> QuestionQuotaResponse(
            limit = result.limit,
            used = (result.limit - result.remaining).coerceAtLeast(0),
            remaining = result.remaining.coerceAtLeast(0),
            joined = true,
        )
        is SubmitQuestionResult.RateLimited -> QuestionQuotaResponse(
            limit = result.limit,
            used = (result.limit - result.remaining).coerceAtLeast(0),
            remaining = result.remaining.coerceAtLeast(0),
            joined = true,
        )
        SubmitQuestionResult.NotJoined -> this?.copy(joined = false)
            ?: QuestionQuotaResponse(joined = false)
        SubmitQuestionResult.InvalidLength,
        SubmitQuestionResult.Banned,
        is SubmitQuestionResult.Rejected,
        -> this
    }

private fun TreeHoleUiState.withReaction(
    postId: String,
    type: TreeHoleReactionType,
    selected: Boolean,
    countDelta: Int,
): TreeHoleUiState {
    fun Set<String>.withMembership(): Set<String> = if (selected) this + postId else this - postId
    fun List<TreeHolePost>.updated(): List<TreeHolePost> = map { post ->
        if (post.id == postId) post.withReactionDelta(type, countDelta) else post
    }

    return copy(
        feedPosts = feedPosts.updated(),
        myPosts = myPosts.updated(),
        authorPosts = authorPosts.updated(),
        selectedPost = selectedPost?.let { post ->
            if (post.id == postId) post.withReactionDelta(type, countDelta) else post
        },
        likedPostIds = if (type == TreeHoleReactionType.LIKE) likedPostIds.withMembership()
        else likedPostIds,
        cheeredPostIds = if (type == TreeHoleReactionType.CHEER) cheeredPostIds.withMembership()
        else cheeredPostIds,
        criedPostIds = if (type == TreeHoleReactionType.CRY) criedPostIds.withMembership()
        else criedPostIds,
    )
}

private fun TreeHoleUiState.reactionCount(
    postId: String,
    type: TreeHoleReactionType,
): Int? {
    val post = feedPosts.firstOrNull { it.id == postId }
        ?: myPosts.firstOrNull { it.id == postId }
        ?: selectedPost?.takeIf { it.id == postId }
        ?: return null
    return when (type) {
        TreeHoleReactionType.LIKE -> post.likesCount
        TreeHoleReactionType.CHEER -> post.cheerCount
        TreeHoleReactionType.CRY -> post.cryCount
    }
}

private fun TreeHoleUiState.withReactionSnapshot(
    postId: String,
    type: TreeHoleReactionType,
    selected: Boolean,
    count: Int?,
): TreeHoleUiState {
    fun Set<String>.withMembership(): Set<String> = if (selected) this + postId else this - postId
    fun TreeHolePost.restored(): TreeHolePost = if (count == null || id != postId) {
        this
    } else {
        withReactionCount(type, count)
    }

    return copy(
        feedPosts = feedPosts.map { it.restored() },
        myPosts = myPosts.map { it.restored() },
        authorPosts = authorPosts.map { it.restored() },
        selectedPost = selectedPost?.restored(),
        likedPostIds = if (type == TreeHoleReactionType.LIKE) likedPostIds.withMembership()
        else likedPostIds,
        cheeredPostIds = if (type == TreeHoleReactionType.CHEER) cheeredPostIds.withMembership()
        else cheeredPostIds,
        criedPostIds = if (type == TreeHoleReactionType.CRY) criedPostIds.withMembership()
        else criedPostIds,
    )
}

private fun TreeHolePost.withReactionDelta(
    type: TreeHoleReactionType,
    delta: Int,
): TreeHolePost = when (type) {
    TreeHoleReactionType.LIKE -> copy(likesCount = (likesCount + delta).coerceAtLeast(0))
    TreeHoleReactionType.CHEER -> copy(cheerCount = (cheerCount + delta).coerceAtLeast(0))
    TreeHoleReactionType.CRY -> copy(cryCount = (cryCount + delta).coerceAtLeast(0))
}

private fun TreeHolePost.withReactionCount(
    type: TreeHoleReactionType,
    count: Int,
): TreeHolePost = when (type) {
    TreeHoleReactionType.LIKE -> copy(likesCount = count.coerceAtLeast(0))
    TreeHoleReactionType.CHEER -> copy(cheerCount = count.coerceAtLeast(0))
    TreeHoleReactionType.CRY -> copy(cryCount = count.coerceAtLeast(0))
}

@Composable
fun TreeHoleScreen(
    onExit: () -> Unit = {},
    viewModel: TreeHoleViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    TreeHoleContent(
        state = state,
        onAcceptTerms = viewModel::acceptCurrentTerms,
        onDeclineTerms = onExit,
        onSaveProfile = viewModel::saveProfile,
        onRefresh = viewModel::refreshAll,
        onFeedModeSelected = viewModel::selectFeedMode,
        onIndustryFiltersChanged = viewModel::setIndustryFilters,
        onClearIndustryFilters = viewModel::clearIndustryFilters,
        onLoadMore = viewModel::loadMoreFeed,
        onSubmitQuestion = { viewModel.submitQuestion(it) },
        onClearQuestionResult = viewModel::clearQuestionSubmissionResult,
        onDelete = viewModel::delete,
        onDeleteComment = viewModel::deleteComment,
        onDeleteAccount = viewModel::deleteAccount,
        onOpenPost = viewModel::openPost,
        onOpenAuthor = viewModel::openAuthor,
        onCloseAuthor = viewModel::closeAuthorPosts,
        onRetryAuthor = viewModel::retryAuthorPosts,
        onOpenNewAnswers = viewModel::openNewestQuestionWithAnswers,
        onClosePost = viewModel::closePost,
        onSubmitComment = viewModel::submitComment,
        onToggleHelpful = viewModel::toggleCommentHelpful,
        onSubmitReport = viewModel::submitReport,
        onBlockAuthor = viewModel::blockAuthor,
        onReact = viewModel::react,
        onSyncCoins = viewModel::syncCoinsAndApprovals,
        onUpdateUnsetIndustries = viewModel::updateUnsetIndustryPosts,
        onUpdateAllIndustries = viewModel::updateAllMismatchedIndustryPosts,
        onDeclineIndustrySync = viewModel::declineCurrentIndustrySync,
        onClearTransientFeedback = viewModel::clearTransientFeedback,
        onOpenVoice = viewModel::openVoice,
        onCloseVoice = viewModel::closeVoice,
        onVoiceSwipe = viewModel::voiceSwipe,
        onShare = { post, source ->
            shareScope.launch {
                runCatching {
                    TreeHoleShareCardSharer.share(context, post.toShareCardContent(), source)
                }.onFailure {
                    Toast.makeText(context, "分享圖產生失敗，請稍後再試。", Toast.LENGTH_SHORT).show()
                }
            }
        },
    )
}

@Composable
private fun TreeHoleContent(
    state: TreeHoleUiState,
    onAcceptTerms: () -> Unit,
    onDeclineTerms: () -> Unit,
    onSaveProfile: (TreeHoleProfile) -> Unit,
    onRefresh: () -> Unit,
    onFeedModeSelected: (TreeHoleFeedMode) -> Unit,
    onIndustryFiltersChanged: (Set<String>) -> Unit,
    onClearIndustryFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onSubmitQuestion: (String) -> Unit,
    onClearQuestionResult: () -> Unit,
    onDelete: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenPost: (TreeHolePost) -> Unit,
    onOpenAuthor: (String, String?, String?) -> Unit,
    onCloseAuthor: () -> Unit,
    onRetryAuthor: () -> Unit,
    onOpenNewAnswers: () -> Unit,
    onClosePost: () -> Unit,
    onSubmitComment: (String) -> Boolean,
    onToggleHelpful: (String) -> Unit,
    onSubmitReport: (
        TreeHoleReportTargetType,
        String,
        TreeHoleReportReason,
        String,
        Boolean,
        String?,
        String,
    ) -> Unit,
    onBlockAuthor: (String?, String) -> Unit,
    onReact: (String, String) -> Unit,
    onSyncCoins: () -> Unit,
    onUpdateUnsetIndustries: () -> Unit,
    onUpdateAllIndustries: () -> Unit,
    onDeclineIndustrySync: () -> Unit,
    onClearTransientFeedback: () -> Unit,
    onOpenVoice: () -> Unit,
    onCloseVoice: () -> Unit,
    onVoiceSwipe: (TreeHolePost, String?) -> Unit,
    onShare: (TreeHolePost, TreeHoleShareSource) -> Unit,
) {
    if (state.acceptedTermsVersion == null) {
        TreeHoleTermsLoading()
    } else if (!state.hasAcceptedCurrentTerms) {
        TreeHoleTermsGate(
            isSubmitting = state.acceptingTerms,
            onAgree = onAcceptTerms,
            onDecline = onDeclineTerms,
        )
    } else if (!state.hasProfile) {
        TreeHoleProfileSetup(
            initial = state.profile,
            message = localizedTreeHoleFeedback(state)?.first,
            onSave = onSaveProfile,
        )
    } else {
        TreeHoleMain(
            state = state,
            onSaveProfile = onSaveProfile,
            onRefresh = onRefresh,
            onFeedModeSelected = onFeedModeSelected,
            onIndustryFiltersChanged = onIndustryFiltersChanged,
            onClearIndustryFilters = onClearIndustryFilters,
            onLoadMore = onLoadMore,
            onSubmitQuestion = onSubmitQuestion,
            onClearQuestionResult = onClearQuestionResult,
            onDelete = onDelete,
            onDeleteComment = onDeleteComment,
            onDeleteAccount = onDeleteAccount,
            onOpenPost = onOpenPost,
            onOpenAuthor = onOpenAuthor,
            onCloseAuthor = onCloseAuthor,
            onRetryAuthor = onRetryAuthor,
            onOpenNewAnswers = onOpenNewAnswers,
            onClosePost = onClosePost,
            onSubmitComment = onSubmitComment,
            onToggleHelpful = onToggleHelpful,
            onSubmitReport = onSubmitReport,
            onBlockAuthor = onBlockAuthor,
            onReact = onReact,
            onSyncCoins = onSyncCoins,
            onUpdateUnsetIndustries = onUpdateUnsetIndustries,
            onUpdateAllIndustries = onUpdateAllIndustries,
            onDeclineIndustrySync = onDeclineIndustrySync,
            onClearTransientFeedback = onClearTransientFeedback,
            onOpenVoice = onOpenVoice,
            onCloseVoice = onCloseVoice,
            onVoiceSwipe = onVoiceSwipe,
            onShare = onShare,
        )
    }
}

@Composable
private fun TreeHoleTermsLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Text("載入條款中…", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, MaterialTheme.colorScheme.onBackground))
    }
}

@Composable
private fun TreeHoleTermsGate(
    isSubmitting: Boolean,
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(TreeHoleTermsStep.DISCLAIMER.index) }
    var agreedDisclaimer by rememberSaveable { mutableStateOf(false) }
    var agreedPrivacy by rememberSaveable { mutableStateOf(false) }
    val step = TreeHoleTermsStep.fromIndex(stepIndex)
    val legalScrollState = rememberScrollState()
    val isCurrentStepAgreed = when (step) {
        TreeHoleTermsStep.DISCLAIMER -> agreedDisclaimer
        TreeHoleTermsStep.PRIVACY -> agreedPrivacy
    }
    val canContinue = TreeHoleTermsFlowPolicy.canContinue(
        step = step,
        disclaimerAgreed = agreedDisclaimer,
        privacyAgreed = agreedPrivacy,
    )
    val canComplete = TreeHoleTermsFlowPolicy.canComplete(
        step = step,
        disclaimerAgreed = agreedDisclaimer,
        privacyAgreed = agreedPrivacy,
    )

    LaunchedEffect(step) {
        legalScrollState.scrollTo(0)
    }
    BackHandler(enabled = !isSubmitting) {
        if (step == TreeHoleTermsStep.PRIVACY) {
            stepIndex = TreeHoleTermsFlowPolicy.previousStep(step).index
        } else {
            onDecline()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = NnoworkSpacing.md, vertical = NnoworkSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("樹洞條款已更新", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, MaterialTheme.colorScheme.onBackground))
                Text(
                    "新增「不當內容零容忍」條文",
                    style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.highlight),
                )
            }
            PixelButton(
                onClick = onDecline,
                enabled = !isSubmitting,
                backgroundColor = NnoworkTheme.colors.disabledBG,
                contentPadding = PaddingValues(NnoworkSpacing.snug),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "不同意並離開樹洞", tint = NnoworkTheme.colors.primaryText)
            }
        }
        Spacer(Modifier.height(NnoworkSpacing.sm))
        TreeHoleTermsProgress(step = step)
        Spacer(Modifier.height(NnoworkSpacing.sm))
        Text(
            when (step) {
                TreeHoleTermsStep.DISCLAIMER -> "先閱讀並同意免責聲明，下一步會再顯示隱私權聲明。"
                TreeHoleTermsStep.PRIVACY -> "免責聲明已確認。請再閱讀並同意隱私權聲明，才會完成加入。"
            },
            style = pixelTextStyle(NnoworkTheme.type.body, color = MaterialTheme.colorScheme.onBackground),
        )
        Spacer(Modifier.height(NnoworkSpacing.sm))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(legalScrollState),
        ) {
            TreeHoleLegalSection(
                title = when (step) {
                    TreeHoleTermsStep.DISCLAIMER -> "免責聲明"
                    TreeHoleTermsStep.PRIVACY -> "隱私權聲明"
                },
                text = when (step) {
                    TreeHoleTermsStep.DISCLAIMER -> TREE_HOLE_DISCLAIMER_V3
                    TreeHoleTermsStep.PRIVACY -> TREE_HOLE_PRIVACY_V3
                },
                agreed = isCurrentStepAgreed,
                onAgreedChange = { agreed ->
                    when (step) {
                        TreeHoleTermsStep.DISCLAIMER -> agreedDisclaimer = agreed
                        TreeHoleTermsStep.PRIVACY -> agreedPrivacy = agreed
                    }
                },
            )
        }

        Spacer(Modifier.height(NnoworkSpacing.snug))
        Text(
            when (step) {
                TreeHoleTermsStep.DISCLAIMER -> "此步驟不會儲存同意紀錄；完成兩份條款後才會正式加入。"
                TreeHoleTermsStep.PRIVACY -> "按下最後的同意按鈕後，才會儲存本次條款版本。"
            },
            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
        )
        Spacer(Modifier.height(NnoworkSpacing.snug))
        if (step == TreeHoleTermsStep.DISCLAIMER) {
            PixelButton(
                onClick = {
                    stepIndex = TreeHoleTermsFlowPolicy.nextStep(
                        step = step,
                        disclaimerAgreed = agreedDisclaimer,
                        privacyAgreed = agreedPrivacy,
                    ).index
                },
                enabled = canContinue && !isSubmitting,
                backgroundColor = if (canContinue) NnoworkTheme.colors.buttonBG else NnoworkTheme.colors.disabledBG,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (canContinue) "下一步：隱私權聲明" else "請先閱讀並同意免責聲明",
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
                Spacer(Modifier.size(NnoworkSpacing.xs))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = NnoworkTheme.colors.primaryText,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
            ) {
                PixelButton(
                    onClick = {
                        stepIndex = TreeHoleTermsFlowPolicy.previousStep(step).index
                    },
                    enabled = !isSubmitting,
                    backgroundColor = NnoworkTheme.colors.disabledBG,
                    modifier = Modifier.weight(0.35f),
                    contentPadding = PaddingValues(horizontal = NnoworkSpacing.snug, vertical = 13.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = NnoworkTheme.colors.primaryText,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(NnoworkSpacing.tight))
                    Text("上一步", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
                PixelButton(
                    onClick = onAgree,
                    enabled = canComplete && !isSubmitting,
                    backgroundColor = if (canComplete) NnoworkTheme.colors.success else NnoworkTheme.colors.disabledBG,
                    modifier = Modifier.weight(0.65f),
                    contentPadding = PaddingValues(horizontal = NnoworkSpacing.snug, vertical = 13.dp),
                ) {
                    Text(
                        when {
                            isSubmitting -> "正在儲存同意…"
                            canComplete -> "同意兩份條款並繼續"
                            else -> "請同意隱私權聲明"
                        },
                        style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                    )
                }
            }
        }
    }
}

@Composable
private fun TreeHoleTermsProgress(step: TreeHoleTermsStep) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "第 ${step.index + 1} 步，共 ${TreeHoleTermsFlowPolicy.STEP_COUNT} 步",
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
            )
            Text(
                if (step == TreeHoleTermsStep.DISCLAIMER) "免責聲明" else "隱私權聲明",
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.highlight),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
        ) {
            repeat(TreeHoleTermsFlowPolicy.STEP_COUNT) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            color = if (index <= step.index) NnoworkTheme.colors.success else NnoworkTheme.colors.secondaryText,
                            shape = RectangleShape,
                        )
                        .border(NnoworkLine.hairline, MaterialTheme.colorScheme.outline, RectangleShape),
                )
            }
        }
    }
}

@Composable
private fun TreeHoleLegalSection(
    title: String,
    text: String,
    agreed: Boolean,
    onAgreedChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(NnoworkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
    ) {
        Text(title, style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        Text(
            text,
            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.mutedText).copy(lineHeight = NnoworkTheme.type.small * 1.75f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = agreed,
                    role = Role.Checkbox,
                    onValueChange = onAgreedChange,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = NnoworkTheme.colors.success,
                    uncheckedColor = NnoworkTheme.colors.secondaryText,
                    checkmarkColor = NnoworkTheme.colors.primaryText,
                ),
            )
            Text(
                "我已閱讀並同意「$title」",
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
            )
        }
    }
}

@Composable
private fun TreeHoleProfileSetup(
    initial: TreeHoleProfile?,
    message: String?,
    onSave: (TreeHoleProfile) -> Unit,
) {
    val industryOptions = treeHoleIndustryOptions()
    var nickname by remember(initial) { mutableStateOf(initial?.nickname.orEmpty()) }
    var industry by remember(initial) { mutableStateOf(initial?.industry ?: TreeHoleIndustryCatalog.values.first()) }
    var role by remember(initial) { mutableStateOf(initial?.role.orEmpty()) }
    var company by remember(initial) { mutableStateOf(initial?.companyName.orEmpty()) }
    var showCompany by remember(initial) { mutableStateOf(initial?.showCompany ?: false) }
    var choosingIndustry by remember { mutableStateOf(false) }

    if (choosingIndustry) {
        TreeHoleIndustryPickerSheet(
            industries = industryOptions,
            selectedIndustry = industry,
            onApply = { industry = it },
            onDismiss = { choosingIndustry = false },
            strings = localizedIndustryPickerStrings(),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(NnoworkSpacing.page),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.roomy),
    ) {
        Text(stringResource(R.string.treehole_title), style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, MaterialTheme.colorScheme.onBackground))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pixelCard()
                .padding(NnoworkSpacing.roomy),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.cozy),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Spa, null, tint = NnoworkTheme.colors.success, modifier = Modifier.size(34.dp))
                Spacer(Modifier.size(NnoworkSpacing.snug))
                Column {
                    Text(stringResource(R.string.treehole_profile_setup_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    Text(stringResource(R.string.treehole_profile_help), style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.mutedText))
                }
            }
            PixelTextField(
                stringResource(R.string.treehole_profile_nickname_label),
                nickname,
                stringResource(R.string.treehole_profile_nickname_placeholder),
            ) { nickname = it }
            PixelTextField(
                stringResource(R.string.treehole_profile_role_label),
                role,
                stringResource(R.string.treehole_profile_role_placeholder),
            ) { role = it }
            Text(stringResource(R.string.treehole_profile_industry_label), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.mutedText))
            PixelButton(
                onClick = { choosingIndustry = true },
                backgroundColor = NnoworkTheme.colors.success,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.sm),
            ) {
                Icon(treeHoleIndustryIcon(industry), contentDescription = null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(NnoworkSpacing.xs))
                Text(localizedIndustryLabel(industry), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
            PixelTextField(
                stringResource(R.string.treehole_profile_company_label),
                company,
                stringResource(R.string.treehole_profile_company_placeholder),
            ) { company = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showCompany,
                    onCheckedChange = { showCompany = it },
                    colors = CheckboxDefaults.colors(checkedColor = NnoworkTheme.colors.success),
                )
                Text(stringResource(R.string.treehole_profile_show_company), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
            message?.let { Text(it, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.danger)) }
            PixelButton(
                onClick = {
                    onSave(
                        TreeHoleProfile(
                            nickname = nickname.trim(),
                            industry = industry,
                            role = role.trim(),
                            companyName = company.trim(),
                            showCompany = showCompany,
                        ),
                    )
                },
                enabled = nickname.isNotBlank() && industry.isNotBlank() && role.isNotBlank(),
                backgroundColor = if (nickname.isNotBlank() && role.isNotBlank()) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.treehole_profile_join), style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        }
    }
}

@Composable
private fun ProfileEditDialog(
    initial: TreeHoleProfile?,
    onDismiss: () -> Unit,
    onSave: (TreeHoleProfile) -> Unit,
) {
    val industryOptions = treeHoleIndustryOptions()
    var nickname by remember(initial) { mutableStateOf(initial?.nickname.orEmpty()) }
    var industry by remember(initial) { mutableStateOf(initial?.industry ?: TreeHoleIndustryCatalog.values.first()) }
    var role by remember(initial) { mutableStateOf(initial?.role.orEmpty()) }
    var company by remember(initial) { mutableStateOf(initial?.companyName.orEmpty()) }
    var showCompany by remember(initial) { mutableStateOf(initial?.showCompany ?: false) }
    var choosingIndustry by remember { mutableStateOf(false) }

    if (choosingIndustry) {
        TreeHoleIndustryPickerSheet(
            industries = industryOptions,
            selectedIndustry = industry,
            onApply = { industry = it },
            onDismiss = { choosingIndustry = false },
            strings = localizedIndustryPickerStrings(),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.treehole_profile_edit_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
                PixelTextField(
                    stringResource(R.string.treehole_profile_nickname_label),
                    nickname,
                    stringResource(R.string.treehole_profile_nickname_placeholder),
                ) { nickname = it }
                PixelTextField(
                    stringResource(R.string.treehole_profile_role_label),
                    role,
                    stringResource(R.string.treehole_profile_role_placeholder),
                ) { role = it }
                Text(stringResource(R.string.treehole_profile_industry_label), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
                PixelButton(
                    onClick = { choosingIndustry = true },
                    backgroundColor = NnoworkTheme.colors.success,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.sm),
                ) {
                    Icon(treeHoleIndustryIcon(industry), contentDescription = null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(NnoworkSpacing.xs))
                    Text(localizedIndustryLabel(industry), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
                PixelTextField(
                    stringResource(R.string.treehole_profile_company_label),
                    company,
                    stringResource(R.string.treehole_profile_company_placeholder),
                ) { company = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showCompany,
                        onCheckedChange = { showCompany = it },
                        colors = CheckboxDefaults.colors(checkedColor = NnoworkTheme.colors.success),
                    )
                    Text(stringResource(R.string.treehole_profile_show_company), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
                }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = {
                    onSave(
                        TreeHoleProfile(
                            nickname = nickname.trim(),
                            industry = industry,
                            role = role.trim(),
                            companyName = company.trim(),
                            showCompany = showCompany,
                        ),
                    )
                },
                enabled = nickname.isNotBlank() && industry.isNotBlank() && role.isNotBlank(),
                backgroundColor = if (nickname.isNotBlank() && role.isNotBlank()) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG,
            ) {
                Text(stringResource(R.string.treehole_profile_save), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, backgroundColor = NnoworkTheme.colors.disabledBG) {
                Text(stringResource(R.string.treehole_action_cancel), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
    )
}

@Composable
private fun TreeHoleMain(
    state: TreeHoleUiState,
    onSaveProfile: (TreeHoleProfile) -> Unit,
    onRefresh: () -> Unit,
    onFeedModeSelected: (TreeHoleFeedMode) -> Unit,
    onIndustryFiltersChanged: (Set<String>) -> Unit,
    onClearIndustryFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onSubmitQuestion: (String) -> Unit,
    onClearQuestionResult: () -> Unit,
    onDelete: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenPost: (TreeHolePost) -> Unit,
    onOpenAuthor: (String, String?, String?) -> Unit,
    onCloseAuthor: () -> Unit,
    onRetryAuthor: () -> Unit,
    onOpenNewAnswers: () -> Unit,
    onClosePost: () -> Unit,
    onSubmitComment: (String) -> Boolean,
    onToggleHelpful: (String) -> Unit,
    onSubmitReport: (
        TreeHoleReportTargetType,
        String,
        TreeHoleReportReason,
        String,
        Boolean,
        String?,
        String,
    ) -> Unit,
    onBlockAuthor: (String?, String) -> Unit,
    onReact: (String, String) -> Unit,
    onSyncCoins: () -> Unit,
    onUpdateUnsetIndustries: () -> Unit,
    onUpdateAllIndustries: () -> Unit,
    onDeclineIndustrySync: () -> Unit,
    onClearTransientFeedback: () -> Unit,
    onOpenVoice: () -> Unit,
    onCloseVoice: () -> Unit,
    onVoiceSwipe: (TreeHolePost, String?) -> Unit,
    onShare: (TreeHolePost, TreeHoleShareSource) -> Unit,
) {
    val industryOptions = treeHoleIndustryOptions()
    val feedbackMessage = localizedTreeHoleFeedback(state)
    var editingProfile by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var showIndustryFilter by remember { mutableStateOf(false) }
    var showQuestionSheet by remember { mutableStateOf(false) }
    var showMyTreeHole by remember { mutableStateOf(false) }
    var safetyMenuTarget by remember { mutableStateOf<TreeHoleSafetyTarget?>(null) }
    var reportTarget by remember { mutableStateOf<TreeHoleSafetyTarget?>(null) }
    var blockTarget by remember { mutableStateOf<TreeHoleSafetyTarget?>(null) }

    LaunchedEffect(state.success?.kind) {
        when (state.success?.kind) {
            TreeHoleFeatureSuccessKind.REPORT_SUBMITTED,
            TreeHoleFeatureSuccessKind.REPORT_SUBMITTED_AND_BLOCKED,
            -> {
                reportTarget = null
                safetyMenuTarget = null
            }
            TreeHoleFeatureSuccessKind.AUTHOR_BLOCKED -> {
                blockTarget = null
                safetyMenuTarget = null
            }
            else -> Unit
        }
    }

    safetyMenuTarget?.let { target ->
        TreeHoleContentActionsDialog(
            target = target,
            onReport = {
                reportTarget = target
                safetyMenuTarget = null
            },
            onBlock = {
                blockTarget = target
                safetyMenuTarget = null
            },
            onDismiss = { safetyMenuTarget = null },
        )
    }

    reportTarget?.let { target ->
        TreeHoleReportDialog(
            target = target,
            submitting = state.reporting,
            errorMessage = feedbackMessage?.first?.takeIf {
                state.failure?.operation == TreeHoleFeatureOperation.REPORT_CONTENT
            },
            onSubmit = { reason, note, alsoBlock ->
                onSubmitReport(
                    target.targetType,
                    target.targetId,
                    reason,
                    note,
                    alsoBlock,
                    target.authorKey,
                    target.nickname,
                )
            },
            onDismiss = { if (!state.reporting) reportTarget = null },
        )
    }

    blockTarget?.let { target ->
        TreeHoleBlockConfirmationDialog(
            target = target,
            onConfirm = { onBlockAuthor(target.authorKey, target.nickname) },
            onDismiss = { blockTarget = null },
        )
    }

    LaunchedEffect(state.questionSubmissionResult) {
        if (state.questionSubmissionResult is SubmitQuestionResult.Submitted) {
            showQuestionSheet = false
            onClearQuestionResult()
        }
    }

    if (showIndustryFilter) {
        TreeHoleIndustryFilterSheet(
            industries = industryOptions,
            selectedIndustries = state.selectedIndustries,
            myIndustry = state.profile?.industry,
            onApply = onIndustryFiltersChanged,
            onDismiss = { showIndustryFilter = false },
            strings = localizedIndustryFilterStrings(),
        )
    }

    if (showQuestionSheet) {
        state.profile?.let { profile ->
            TreeHoleQuestionSheet(
                identity = TreeHoleQuestionIdentityUi(
                    nickname = profile.nickname,
                    industry = localizedIndustryLabel(profile.industry),
                    role = profile.role,
                    companyName = profile.companyName,
                    showCompany = profile.showCompany,
                ),
                quota = state.questionQuota?.let {
                    TreeHoleQuestionQuotaUi(
                        limit = it.limit,
                        remaining = it.remaining,
                        joined = it.joined,
                    )
                },
                submitting = state.submittingQuestion,
                errorMessage = localizedQuestionError(state),
                onSubmit = onSubmitQuestion,
                onDismiss = {
                    showQuestionSheet = false
                    onClearQuestionResult()
                },
                strings = localizedQuestionStrings(),
            )
        }
    }

    if (showMyTreeHole) {
        state.profile?.let { profile ->
            val syncStatus = state.pendingIndustrySyncStatus
            val syncUnset = (syncStatus?.unsetCount ?: 0) > 0
            val syncUi = syncStatus?.let {
                TreeHoleIndustrySyncUi(
                    title = stringResource(
                        if (syncUnset) R.string.treehole_industry_sync_unset_title
                        else R.string.treehole_industry_sync_mismatch_title,
                    ),
                    message = stringResource(
                        if (syncUnset) R.string.treehole_industry_sync_unset_body
                        else R.string.treehole_industry_sync_mismatch_body,
                        if (syncUnset) it.unsetCount else it.mismatchedCount,
                        localizedIndustryLabel(it.currentIndustry),
                    ),
                    actionLabel = stringResource(
                        if (syncUnset) R.string.treehole_industry_sync_action_update_unset
                        else R.string.treehole_industry_sync_action_update_all,
                    ),
                    loading = state.industrySyncing,
                )
            }
            TreeHoleMyContentSheet(
                profile = profile.copy(industry = localizedIndustryLabel(profile.industry)),
                industryIcon = treeHoleIndustryIcon(profile.industry),
                // 只用於 My sheet 顯示；callback 只傳 id，翻譯值不會寫回 API。
                posts = state.myPosts.map { post ->
                    post.copy(industry = localizedIndustryLabel(post.industry))
                },
                comments = state.myComments,
                answerExp = state.myAnswerExp,
                helpfulAnswerCount = state.myHelpfulAnswerCount,
                loadingPosts = state.refreshing,
                loadingComments = state.refreshing,
                onEditProfile = {
                    showMyTreeHole = false
                    editingProfile = true
                },
                onDeletePost = { onDelete(it.id) },
                onDeleteComment = { onDeleteComment(it.id) },
                onDismiss = { showMyTreeHole = false },
                industrySync = syncUi,
                onIndustrySync = syncStatus?.let {
                    if (syncUnset) onUpdateUnsetIndustries else onUpdateAllIndustries
                },
                strings = localizedMyTreeHoleStrings(),
            )
        }
    }

    state.selectedAuthor?.let { author ->
        TreeHoleAuthorPostsSheet(
            author = author,
            posts = state.authorPosts,
            loading = state.authorPostsLoading,
            errorMessage = if (
                state.failure?.operation == TreeHoleFeatureOperation.LOAD_AUTHOR_POSTS
            ) {
                stringResource(R.string.treehole_author_load_failed)
            } else {
                null
            },
            industryLabel = localizedIndustryLabel(author.industry),
            industryIcon = treeHoleIndustryIcon(author.industry),
            onDismiss = onCloseAuthor,
            onRetry = onRetryAuthor,
            onOpenPost = { post ->
                onCloseAuthor()
                onOpenPost(post)
            },
            onReact = { postId, type -> onReact(postId, type.apiValue) },
            strings = localizedAuthorPostsStrings(),
            badgeStrings = localizedAnswerBadgeStrings(),
        )
    }

    state.selectedPost?.let { post ->
        val commentFeedback = feedbackMessage?.takeIf {
            state.failure?.operation == TreeHoleFeatureOperation.LOAD_COMMENTS ||
                state.failure?.operation == TreeHoleFeatureOperation.SUBMIT_COMMENT ||
                state.failure?.operation == TreeHoleFeatureOperation.MARK_COMMENT_HELPFUL ||
                state.failure?.operation == TreeHoleFeatureOperation.REPORT_CONTENT ||
                state.success?.kind == TreeHoleFeatureSuccessKind.COMMENT_SUBMITTED ||
                state.success?.kind == TreeHoleFeatureSuccessKind.HELPFUL_ANSWER_UPDATED ||
                state.success?.kind == TreeHoleFeatureSuccessKind.REPORT_SUBMITTED
        }
        CommentsDialog(
            post = post,
            comments = state.comments,
            loading = state.commentsLoading,
            submitting = state.submittingComment,
            feedback = commentFeedback,
            submissionSucceeded = state.success?.kind == TreeHoleFeatureSuccessKind.COMMENT_SUBMITTED,
            canMarkHelpful = post.isQuestion && state.myPosts.any { it.id == post.id },
            ownNickname = state.profile?.nickname,
            pendingHelpfulCommentIds = state.pendingHelpfulCommentIds,
            ownCommentIds = state.myComments.mapTo(mutableSetOf()) { it.id },
            onDismiss = onClosePost,
            onSubmit = onSubmitComment,
            onToggleHelpful = onToggleHelpful,
            onOpenAuthor = { comment ->
                onClosePost()
                onOpenAuthor(comment.nickname, comment.industry, comment.authorKey)
            },
            onSafetyActionsPost = if (state.myPosts.none { it.id == post.id }) {
                {
                    val target = TreeHoleSafetyTarget(
                        targetType = TreeHoleReportTargetType.POST,
                        targetId = post.id,
                        authorKey = post.authorKey,
                        nickname = post.nickname?.ifBlank { "職場倖存者" } ?: "職場倖存者",
                        preview = post.content,
                    )
                    onClosePost()
                    safetyMenuTarget = target
                }
            } else {
                null
            },
            onSafetyActionsComment = { comment ->
                if (state.myComments.none { it.id == comment.id }) {
                    val target = TreeHoleSafetyTarget(
                        targetType = TreeHoleReportTargetType.COMMENT,
                        targetId = comment.id,
                        authorKey = comment.authorKey,
                        nickname = comment.nickname.ifBlank { "職場倖存者" },
                        preview = comment.content,
                    )
                    onClosePost()
                    safetyMenuTarget = target
                }
            },
            onShare = if (post.status == null || post.status == "approved") {
                { onShare(post, TreeHoleShareSource.PostDetail) }
            } else {
                null
            },
        )
    }

    if (editingProfile) {
        ProfileEditDialog(
            initial = state.profile,
            onDismiss = { editingProfile = false },
            onSave = {
                editingProfile = false
                onSaveProfile(it)
            },
        )
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAccount = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("刪除樹洞帳號？", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark)) },
            text = {
                Text(
                    "這會刪除你的樹洞身份、貼文與留言；本機集點紀錄與金幣不會被刪除。",
                    style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.secondaryText),
                )
            },
            confirmButton = {
                PixelButton(
                    onClick = {
                        confirmDeleteAccount = false
                        onDeleteAccount()
                    },
                    backgroundColor = NnoworkTheme.colors.danger,
                ) {
                    Text("確定刪除", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
                }
            },
            dismissButton = {
                PixelButton(onClick = { confirmDeleteAccount = false }, backgroundColor = NnoworkTheme.colors.disabledBG) {
                    Text("取消", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            },
        )
    }

    state.pendingIndustrySyncStatus
        ?.takeIf {
            !showMyTreeHole && !editingProfile && !showQuestionSheet && !showIndustryFilter &&
                !confirmDeleteAccount
        }
        ?.let { status ->
            val updateUnsetOnly = status.unsetCount > 0
            IndustrySyncPromptDialog(
                status = status,
                updateUnsetOnly = updateUnsetOnly,
                loading = state.industrySyncing,
                onUpdate = if (updateUnsetOnly) onUpdateUnsetIndustries else onUpdateAllIndustries,
                onDecline = onDeclineIndustrySync,
            )
        }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(horizontal = NnoworkSpacing.page),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.cozy),
        ) {
            item {
                Row(
                    modifier = Modifier.padding(top = NnoworkSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.treehole_title),
                        style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.weight(1f),
                    )
                    PixelButton(
                        onClick = onSyncCoins,
                        enabled = !state.syncingCoins,
                        backgroundColor = NnoworkTheme.colors.highlight,
                        modifier = Modifier.heightIn(min = NnoworkSize.minTouchTarget),
                        contentPadding = PaddingValues(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.snug),
                    ) {
                        if (state.syncingCoins) {
                            Text("…", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                        } else {
                            Icon(
                                Icons.Filled.MonetizationOn,
                                contentDescription = stringResource(R.string.treehole_coin_sync),
                                tint = NnoworkTheme.colors.primaryText,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.size(NnoworkSpacing.xs))
                    PixelButton(
                        onClick = { showMyTreeHole = true },
                        backgroundColor = NnoworkTheme.colors.success,
                        modifier = Modifier.heightIn(min = NnoworkSize.minTouchTarget),
                        contentPadding = PaddingValues(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.snug),
                    ) {
                        Text(stringResource(R.string.treehole_mine_title), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                    Spacer(Modifier.size(NnoworkSpacing.xs))
                    PixelButton(
                        onClick = { confirmDeleteAccount = true },
                        backgroundColor = NnoworkTheme.colors.danger,
                        modifier = Modifier.size(NnoworkSize.minTouchTarget),
                        contentPadding = PaddingValues(NnoworkSpacing.snug),
                    ) {
                        Icon(Icons.Filled.Delete, "刪除樹洞帳號", tint = NnoworkTheme.colors.textOnDark, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.size(NnoworkSpacing.xs))
                    PixelButton(
                        onClick = onRefresh,
                        enabled = !state.refreshing,
                        backgroundColor = NnoworkTheme.colors.buttonBG,
                        modifier = Modifier.size(NnoworkSize.minTouchTarget),
                        contentPadding = PaddingValues(NnoworkSpacing.snug),
                    ) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.treehole_action_refresh), tint = NnoworkTheme.colors.textOnDark)
                    }
                }
            }
            item {
                TodaysVoiceEntry(unseenCount = state.voicePosts.size, onOpen = onOpenVoice)
            }
            if (state.newAnswerCount > 0) {
                item {
                    NewAnswersBanner(
                        count = state.newAnswerCount,
                        hasUnmarkedHelpfulAnswers = state.hasUnmarkedHelpfulAnswers,
                        onOpen = onOpenNewAnswers,
                    )
                }
            }
            if (state.feedMode != TreeHoleFeedMode.QUESTION) {
                item { PostHintCard() }
            }
            item {
                TreeHoleFeedFilterBar(
                    selectedMode = state.feedMode,
                    industryLabel = if (state.selectedIndustries.isEmpty()) {
                        stringResource(R.string.treehole_industry_filter_all)
                    } else {
                        stringResource(R.string.treehole_industry_filter_count, state.selectedIndustries.size)
                    },
                    industryFilterActive = state.selectedIndustries.isNotEmpty(),
                    onModeSelected = onFeedModeSelected,
                    onOpenIndustryFilter = { showIndustryFilter = true },
                    onClearIndustryFilter = onClearIndustryFilters,
                    modifier = Modifier.padding(horizontal = 0.dp),
                    strings = localizedFeedFilterStrings(),
                )
            }

            feedbackMessage?.let { feedback ->
                item {
                    Text(
                        feedback.first,
                        style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, feedback.second),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NnoworkTheme.colors.primaryText.copy(alpha = 0.25f))
                            .clickable { onClearTransientFeedback() }
                            .padding(NnoworkSpacing.snug),
                    )
                }
            }

            if (state.feedLoading && state.feedPosts.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.treehole_feed_loading),
                        style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier.padding(vertical = 30.dp),
                    )
                }
            } else if (state.visibleFeedPosts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(vertical = 30.dp),
                        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
                    ) {
                        Text(
                            when {
                                state.feedMode == TreeHoleFeedMode.QUESTION && state.selectedIndustries.isNotEmpty() ->
                                    stringResource(R.string.treehole_question_empty_industry)
                                state.feedMode == TreeHoleFeedMode.QUESTION ->
                                    stringResource(R.string.treehole_question_empty_all)
                                else -> "目前還沒有符合篩選條件的貼文。"
                            },
                            style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.secondaryText),
                        )
                        if (state.feedMode == TreeHoleFeedMode.QUESTION) {
                            PixelButton(onClick = { showQuestionSheet = true }, backgroundColor = NnoworkTheme.colors.treeHole.questionAccent) {
                                Text(stringResource(R.string.treehole_question_ask), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                            }
                        }
                    }
                }
            } else {
                items(state.visibleFeedPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        mine = false,
                        onOpen = { onOpenPost(post) },
                        onOpenAuthor = {
                            onOpenAuthor(
                                post.nickname.orEmpty(),
                                post.industry,
                                post.authorKey,
                            )
                        },
                        onReact = { reaction -> onReact(post.id, reaction) },
                        onDelete = {},
                        onShare = { onShare(post, TreeHoleShareSource.FeedList) },
                        onSafetyActions = if (state.myPosts.none { it.id == post.id }) {
                            {
                                safetyMenuTarget = TreeHoleSafetyTarget(
                                    targetType = TreeHoleReportTargetType.POST,
                                    targetId = post.id,
                                    authorKey = post.authorKey,
                                    nickname = post.nickname?.ifBlank { "職場倖存者" }
                                        ?: "職場倖存者",
                                    preview = post.content,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }

            // 當第一頁沒有命中本機行業篩選時，只要 server feed
            // 尚未到底，仍要讓使用者繼續載入後續頁。
            if (!state.feedLoading && (state.visibleFeedPosts.isNotEmpty() || state.hasMorePosts)) {
                item {
                    if (state.hasMorePosts) {
                        PixelButton(
                            onClick = onLoadMore,
                            enabled = !state.feedLoadingMore,
                            backgroundColor = NnoworkTheme.colors.buttonBG,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (state.feedLoadingMore) stringResource(R.string.treehole_common_loading)
                                else stringResource(R.string.treehole_feed_load_more),
                                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.treehole_feed_end),
                            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(82.dp)) }
        }

        PixelButton(
            onClick = { showQuestionSheet = true },
            backgroundColor = NnoworkTheme.colors.treeHole.questionAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(NnoworkSpacing.page)
                .heightIn(min = 52.dp),
            contentPadding = PaddingValues(horizontal = NnoworkSpacing.md, vertical = NnoworkSpacing.sm),
        ) {
            Text("?", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            Spacer(Modifier.size(7.dp))
            Text(stringResource(R.string.treehole_question_ask), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }

        if (state.showVoice) {
            TodaysVoiceOverlay(
                posts = state.voicePosts,
                onSwipe = onVoiceSwipe,
                onShare = { post -> onShare(post, TreeHoleShareSource.CardStack) },
                onOpenPost = { post ->
                    onCloseVoice()
                    onOpenPost(post)
                },
                onSafetyActions = { post ->
                    if (state.myPosts.none { it.id == post.id }) {
                        onCloseVoice()
                        safetyMenuTarget = TreeHoleSafetyTarget(
                            targetType = TreeHoleReportTargetType.POST,
                            targetId = post.id,
                            authorKey = post.authorKey,
                            nickname = post.nickname?.ifBlank { "職場倖存者" }
                                ?: "職場倖存者",
                            preview = post.content,
                        )
                    }
                },
                onClose = onCloseVoice,
            )
        }
    }
}

@Composable
private fun TodaysVoiceEntry(unseenCount: Int, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = NnoworkTheme.colors.highlight)
            .clickable(enabled = unseenCount > 0, onClick = onOpen)
            .padding(NnoworkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.ChatBubble,
            contentDescription = null,
            tint = NnoworkTheme.colors.primaryText,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.size(NnoworkSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text("今日心聲", style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            Text(
                if (unseenCount > 0) "還有 $unseenCount 則沒看過，滑一滑給點反應吧" else "今天的心聲都看完了，明天再來",
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.mutedText),
            )
        }
        if (unseenCount > 0) {
            Icon(Icons.Filled.Style, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun NewAnswersBanner(
    count: Int,
    hasUnmarkedHelpfulAnswers: Boolean,
    onOpen: () -> Unit,
) {
    val openLabel = stringResource(R.string.treehole_new_answers_open)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(NnoworkTheme.colors.treeHole.questionSoft)
            .border(NnoworkLine.emphasis, NnoworkTheme.colors.treeHole.questionAccentDark, RectangleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = openLabel,
                onClick = onOpen,
            )
            .padding(horizontal = NnoworkSpacing.cozy, vertical = NnoworkSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (hasUnmarkedHelpfulAnswers) Icons.Filled.Star else Icons.Filled.ChatBubble,
            contentDescription = null,
            tint = NnoworkTheme.colors.treeHole.questionAccentDark,
            modifier = Modifier.size(26.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = stringResource(R.string.treehole_new_answers_title, count.coerceAtLeast(0)),
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.treeHole.questionTitle),
            )
            Text(
                text = stringResource(R.string.treehole_new_answers_hint),
                style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Medium, NnoworkTheme.colors.treeHole.questionBody),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = openLabel,
            tint = NnoworkTheme.colors.treeHole.questionAccentDark,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun TodaysVoiceOverlay(
    posts: List<TreeHolePost>,
    onSwipe: (TreeHolePost, String?) -> Unit,
    onShare: (TreeHolePost) -> Unit,
    onOpenPost: (TreeHolePost) -> Unit,
    onSafetyActions: (TreeHolePost) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NnoworkTheme.colors.primaryText.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(NnoworkSpacing.page),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("今日心聲", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.textOnDark), modifier = Modifier.weight(1f))
                PixelButton(
                    onClick = onClose,
                    backgroundColor = NnoworkTheme.colors.disabledBG,
                    modifier = Modifier.size(NnoworkSize.minTouchTarget),
                    contentPadding = PaddingValues(NnoworkSpacing.snug),
                ) {
                    Icon(Icons.Filled.Close, "關閉今日心聲", tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(NnoworkSpacing.snug))
            Text(
                "右滑按讚 · 左滑加油 · 上滑難過 · 下滑跳過",
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.highlight),
            )
            Spacer(Modifier.weight(1f))
            val current = posts.firstOrNull()
            if (current == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", style = pixelTextStyle(NnoworkTheme.type.hero))
                    Spacer(Modifier.height(NnoworkSpacing.sm))
                    Text("今天的心聲都看完了！", style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
                    Spacer(Modifier.height(NnoworkSpacing.md))
                    PixelButton(onClick = onClose, backgroundColor = NnoworkTheme.colors.highlight) {
                        Text("關閉", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // 後面疊一張下一則，做出卡堆景深
                    posts.getOrNull(1)?.let { back ->
                        VoiceCard(
                            post = back,
                            modifier = Modifier
                                .scale(0.94f)
                                .offset(y = 16.dp)
                                .alpha(0.6f),
                        )
                    }
                    SwipeableVoiceCard(
                        post = current,
                        // key 確保換卡時重置位移狀態
                        key = current.id,
                        onSwipe = { reaction -> onSwipe(current, reaction) },
                        onSafetyActions = { onSafetyActions(current) },
                    )
                }
                Spacer(Modifier.height(NnoworkSpacing.roomy))
                var reactionSent by remember(current.id) { mutableStateOf(false) }
                VoiceActionRow(
                    reactionsEnabled = !reactionSent,
                    onReaction = { reaction ->
                        if (!reactionSent) {
                            reactionSent = true
                            onSwipe(current, reaction)
                        }
                    },
                    onShare = { onShare(current) },
                    onComment = { onOpenPost(current) },
                    onSkip = {
                        if (!reactionSent) {
                            reactionSent = true
                            onSwipe(current, null)
                        }
                    },
                )
            }
            Spacer(Modifier.weight(1f))
            Text("剩下 ${posts.size} 則", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
        }
    }
}

@Composable
private fun VoiceActionRow(
    reactionsEnabled: Boolean,
    onReaction: (String) -> Unit,
    onShare: () -> Unit,
    onComment: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
        ) {
            VoiceActionButton(Icons.Filled.ThumbUp, "按讚", NnoworkTheme.colors.success, reactionsEnabled) { onReaction("like") }
            VoiceActionButton(Icons.Filled.Campaign, "加油", NnoworkTheme.colors.info, reactionsEnabled) { onReaction("cheer") }
            VoiceActionButton(Icons.Filled.SentimentDissatisfied, "難過", NnoworkTheme.colors.warning, reactionsEnabled) { onReaction("cry") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
        ) {
            VoiceActionButton(Icons.Filled.Share, "分享", NnoworkTheme.colors.highlight, true, onShare)
            VoiceActionButton(Icons.Filled.ChatBubble, "留言", NnoworkTheme.colors.secondaryText, true, onComment)
            VoiceActionButton(Icons.Filled.ArrowDownward, "跳過", NnoworkTheme.colors.textOnDark, true, onSkip)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.VoiceActionButton(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PixelButton(
        onClick = onClick,
        enabled = enabled,
        backgroundColor = color,
        modifier = Modifier
            .weight(1f)
            .height(54.dp),
        contentPadding = PaddingValues(horizontal = NnoworkSpacing.xs, vertical = 5.dp),
    ) {
        Icon(icon, contentDescription = label, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(NnoworkSpacing.tight))
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText), maxLines = 1)
    }
}

@Composable
private fun SwipeableVoiceCard(
    post: TreeHolePost,
    key: String,
    onSwipe: (String?) -> Unit,
    onSafetyActions: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(key) { Animatable(0f) }
    val offsetY = remember(key) { Animatable(0f) }
    var dismissing by remember(key) { mutableStateOf(false) }
    val threshold = 200f
    val fly = 1800f

    fun fling(reaction: String?, tx: Float, ty: Float) {
        if (dismissing) return
        dismissing = true
        scope.launch {
            launch { offsetX.animateTo(tx, tween(260)) }
            offsetY.animateTo(ty, tween(260))
            onSwipe(reaction) // 動畫飛出後才換卡
        }
    }

    val x = offsetX.value
    val y = offsetY.value
    val hint: Pair<String, androidx.compose.ui.graphics.Color>? = when {
        x > 40 && x >= kotlin.math.abs(y) -> "按讚" to NnoworkTheme.colors.success
        x < -40 && -x >= kotlin.math.abs(y) -> "加油" to NnoworkTheme.colors.info
        y < -40 -> "難過" to NnoworkTheme.colors.warning
        y > 40 -> "跳過" to NnoworkTheme.colors.secondaryText
        else -> null
    }
    val hintAlpha = (maxOf(kotlin.math.abs(x), kotlin.math.abs(y)) / threshold).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
            .offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
            .rotate(offsetX.value / 45f)
            .pointerInput(key) {
                detectDragGestures(
                    onDrag = { change, drag ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + drag.x) }
                        scope.launch { offsetY.snapTo(offsetY.value + drag.y) }
                    },
                    onDragEnd = {
                        val cx = offsetX.value
                        val cy = offsetY.value
                        when {
                            cx > threshold -> fling("like", fly, cy)
                            cx < -threshold -> fling("cheer", -fly, cy)
                            cy < -threshold -> fling("cry", cx, -fly)
                            cy > threshold -> fling(null, cx, fly)
                            else -> resetCard(scope, offsetX, offsetY)
                        }
                    },
                )
            },
    ) {
        VoiceCardBody(post, onSafetyActions)
        hint?.let { (label, color) ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = NnoworkSpacing.page)
                    .alpha(hintAlpha)
                    .background(color)
                    .border(NnoworkLine.emphasis, NnoworkTheme.colors.border, RectangleShape)
                    .padding(horizontal = NnoworkSpacing.md, vertical = NnoworkSpacing.tight),
            ) {
                Text(label, style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
            }
        }
    }
}

/** 卡堆中後面那張靜態卡片。 */
@Composable
private fun VoiceCard(post: TreeHolePost, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(380.dp)) {
        VoiceCardBody(post, onSafetyActions = null)
    }
}

/** 心聲卡片內容（前景與背景共用）。 */
@Composable
private fun VoiceCardBody(
    post: TreeHolePost,
    onSafetyActions: (() -> Unit)?,
) {
    val authorName = post.nickname?.ifBlank { "職場倖存者" } ?: "職場倖存者"
    val badgeStrings = localizedAnswerBadgeStrings()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pixelCard(backgroundColor = MaterialTheme.colorScheme.surfaceVariant)
            .padding(NnoworkSpacing.page),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TreeHoleInitialsAvatar(nickname = authorName, size = 40.dp)
                Spacer(Modifier.size(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(authorName, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                        if (post.isQuestion) QuestionTag()
                        TreeHoleAnswerBadgeMark(post.authorExp, strings = badgeStrings)
                    }
                    Text(
                        buildList {
                            post.industry?.let { add(localizedIndustryLabel(it)) }
                            post.role?.takeIf(String::isNotBlank)?.let(::add)
                            post.companyName?.takeIf(String::isNotBlank)?.let(::add)
                        }.joinToString(" · ").ifBlank { "職場倖存者" },
                        style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.mutedText),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                onSafetyActions?.let {
                    TreeHoleSafetyActionButton(onClick = it)
                }
            }
            Spacer(Modifier.height(NnoworkSpacing.cozy))
            Text(
                post.content,
                style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Medium, NnoworkTheme.colors.primaryText),
                modifier = Modifier.weight(1f),
            )
            if (post.isQuestion) {
                Text(
                    if (post.commentCount > 0) {
                        stringResource(R.string.treehole_question_reply_count, post.commentCount)
                    } else {
                        stringResource(R.string.treehole_question_waiting_for_answer)
                    },
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.treeHole.questionAccentDark),
                )
                Spacer(Modifier.height(NnoworkSpacing.tight))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.cozy)) {
                ReactionStat(Icons.Filled.ThumbUp, "按讚", post.likesCount)
                ReactionStat(Icons.Filled.Campaign, "加油", post.cheerCount)
                ReactionStat(Icons.Filled.SentimentDissatisfied, "難過", post.cryCount)
            }
        }
    }
}

private fun resetCard(
    scope: kotlinx.coroutines.CoroutineScope,
    offsetX: Animatable<Float, *>,
    offsetY: Animatable<Float, *>,
) {
    scope.launch { offsetX.animateTo(0f) }
    scope.launch { offsetY.animateTo(0f) }
}

@Composable
private fun PostHintCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(NnoworkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = NnoworkTheme.colors.warning, modifier = Modifier.size(26.dp))
        Spacer(Modifier.size(NnoworkSpacing.snug))
        Column {
            Text("想發文？去「集點卡」蓋章", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            Text(
                "蓋章後會問你要不要把原因分享到樹洞，審核通過才會公開。",
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
            )
        }
    }
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    PixelButton(
        onClick = onClick,
        backgroundColor = if (selected) NnoworkTheme.colors.success else NnoworkTheme.colors.disabledBG,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = NnoworkSpacing.sm),
    ) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
    }
}

@Composable
private fun PostCard(
    post: TreeHolePost,
    mine: Boolean,
    onOpen: () -> Unit,
    onOpenAuthor: () -> Unit,
    onReact: (String) -> Unit,
    onDelete: () -> Unit,
    onShare: (() -> Unit)?,
    onSafetyActions: (() -> Unit)?,
) {
    val authorName = post.nickname?.ifBlank { "匿名社畜" } ?: "匿名社畜"
    val badgeStrings = localizedAnswerBadgeStrings()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .clickable(
                role = Role.Button,
                onClickLabel = "查看留言",
                onClick = onOpen,
            )
            .padding(NnoworkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(NnoworkSize.minTouchTarget)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.treehole_author_view_posts),
                        onClick = onOpenAuthor,
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                TreeHoleInitialsAvatar(nickname = authorName, size = 40.dp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
                    Text(
                        authorName,
                        style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (post.isQuestion) QuestionTag()
                    TreeHoleAnswerBadgeMark(
                        exp = post.authorExp,
                        strings = badgeStrings,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(treeHoleIndustryIcon(post.industry), null, tint = NnoworkTheme.colors.mutedText, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(
                        buildList {
                            post.industry?.let { add(localizedIndustryLabel(it)) }
                            post.role?.takeIf(String::isNotBlank)?.let(::add)
                            post.companyName?.takeIf(String::isNotBlank)?.let(::add)
                        }.joinToString(" · ").ifBlank { "職場倖存者" },
                        style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.mutedText),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // 只在「我的貼文」顯示待審核/未通過狀態；已通過不顯示徽章。
            if (mine) post.status?.takeIf { it != "approved" }?.let { StatusBadge(it) }
            if (mine) {
                Spacer(Modifier.size(NnoworkSpacing.xs))
                PixelButton(
                    onClick = onDelete,
                    backgroundColor = NnoworkTheme.colors.danger,
                    modifier = Modifier.size(NnoworkSize.minTouchTarget),
                    contentPadding = PaddingValues(7.dp),
                ) {
                    Icon(Icons.Filled.Delete, "刪除貼文", tint = NnoworkTheme.colors.textOnDark, modifier = Modifier.size(17.dp))
                }
            }
            onSafetyActions?.let {
                Spacer(Modifier.size(NnoworkSpacing.xs))
                TreeHoleSafetyActionButton(onClick = it)
            }
        }
        Text(post.content, style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Medium, NnoworkTheme.colors.primaryText))
        if (post.isQuestion) {
            Text(
                if (post.commentCount > 0) {
                    stringResource(R.string.treehole_question_reply_count, post.commentCount)
                } else {
                    stringResource(R.string.treehole_question_waiting_for_answer)
                },
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.treeHole.questionAccentDark),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            ReactionButton(Icons.Filled.ThumbUp, "按讚", post.likesCount) { onReact("like") }
            ReactionButton(Icons.Filled.Campaign, "加油", post.cheerCount) { onReact("cheer") }
            ReactionButton(Icons.Filled.SentimentDissatisfied, "難過", post.cryCount) { onReact("cry") }
            Spacer(Modifier.weight(1f))
            onShare?.let {
                PixelButton(
                    onClick = it,
                    backgroundColor = NnoworkTheme.colors.highlight,
                    modifier = Modifier.size(NnoworkSize.minTouchTarget),
                    contentPadding = PaddingValues(horizontal = NnoworkSpacing.xs, vertical = NnoworkSpacing.tight),
                ) {
                    Icon(Icons.Filled.Share, "分享貼文", tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.size(NnoworkSpacing.xs))
            }
            PixelButton(
                onClick = onOpen,
                backgroundColor = NnoworkTheme.colors.disabledBG,
                modifier = Modifier.heightIn(min = NnoworkSize.minTouchTarget),
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = NnoworkSpacing.tight),
            ) {
                Icon(Icons.Filled.ChatBubble, "查看留言", tint = NnoworkTheme.colors.mutedText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(5.dp))
                Text("${post.commentCount}", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        }
        post.latestComment?.let {
            Text("最新回覆：$it", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MyCommentCard(comment: MyTreeHoleComment, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(NnoworkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            comment.status.takeIf { it != "approved" }?.let { StatusBadge(it) }
            Spacer(Modifier.weight(1f))
            PixelButton(
                onClick = onDelete,
                backgroundColor = NnoworkTheme.colors.danger,
                modifier = Modifier.size(NnoworkSize.minTouchTarget),
                contentPadding = PaddingValues(7.dp),
            ) {
                Icon(Icons.Filled.Delete, "刪除留言", tint = NnoworkTheme.colors.textOnDark, modifier = Modifier.size(17.dp))
            }
        }
        Text(comment.content, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        comment.postContent?.let {
            Text(
                "回覆貼文：$it",
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun IndustrySyncPromptDialog(
    status: IndustrySyncStatusResponse,
    updateUnsetOnly: Boolean,
    loading: Boolean,
    onUpdate: () -> Unit,
    onDecline: () -> Unit,
) {
    val count = if (updateUnsetOnly) status.unsetCount else status.mismatchedCount
    val industry = localizedIndustryLabel(status.currentIndustry)
    AlertDialog(
        onDismissRequest = { if (!loading) onDecline() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                stringResource(
                    if (updateUnsetOnly) R.string.treehole_industry_sync_unset_title
                    else R.string.treehole_industry_sync_mismatch_title,
                ),
                style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
            )
        },
        text = {
            Text(
                stringResource(
                    if (updateUnsetOnly) R.string.treehole_industry_sync_unset_body
                    else R.string.treehole_industry_sync_mismatch_body,
                    count,
                    industry,
                ),
                style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.secondaryText),
            )
        },
        confirmButton = {
            PixelButton(
                onClick = onUpdate,
                enabled = !loading,
                backgroundColor = NnoworkTheme.colors.highlight,
            ) {
                Text(
                    if (loading) stringResource(R.string.treehole_common_loading)
                    else stringResource(
                        if (updateUnsetOnly) R.string.treehole_industry_sync_action_update_unset
                        else R.string.treehole_industry_sync_action_update_all,
                    ),
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        },
        dismissButton = {
            PixelButton(
                onClick = onDecline,
                enabled = !loading,
                backgroundColor = NnoworkTheme.colors.disabledBG,
            ) {
                Text(
                    stringResource(
                        if (updateUnsetOnly) R.string.treehole_industry_sync_decline
                        else R.string.treehole_industry_sync_action_keep_mismatched,
                    ),
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        },
    )
}

@Composable
private fun QuestionTag() {
    Box(
        modifier = Modifier
            .background(NnoworkTheme.colors.treeHole.questionAccent)
            .border(NnoworkLine.hairline, NnoworkTheme.colors.border, RectangleShape)
            .padding(horizontal = NnoworkSpacing.tight, vertical = 2.dp),
    ) {
        Text(
            stringResource(R.string.treehole_question_tag),
            style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.primaryText),
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "approved" -> "已通過" to NnoworkTheme.colors.success
        "rejected" -> "未通過" to NnoworkTheme.colors.danger
        else -> "審核中" to NnoworkTheme.colors.warning
    }
    Box(
        modifier = Modifier
            .background(color)
            .border(NnoworkLine.control, NnoworkTheme.colors.border, RectangleShape)
            .padding(horizontal = NnoworkSpacing.xs, vertical = 3.dp),
    ) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.primaryText))
    }
}

@Composable
private fun ReactionButton(icon: ImageVector, label: String, count: Int, onClick: () -> Unit) {
    PixelButton(
        onClick = onClick,
        backgroundColor = NnoworkTheme.colors.elevatedSurface,
        borderColor = NnoworkTheme.colors.divider,
        contentPadding = PaddingValues(horizontal = NnoworkSpacing.xs, vertical = 5.dp),
    ) {
        Icon(icon, contentDescription = label, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(NnoworkSpacing.xxs))
        Text(count.toString(), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
    }
}

@Composable
private fun ReactionStat(icon: ImageVector, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
        Icon(icon, contentDescription = label, tint = NnoworkTheme.colors.secondaryText, modifier = Modifier.size(15.dp))
        Text(count.toString(), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
    }
}

/** author_key 兩邊都存在時是唯一身分來源；只有舊資料缺 key 才退回暱稱。 */
internal fun sameTreeHoleAuthor(
    firstAuthorKey: String?,
    secondAuthorKey: String?,
    firstNickname: String?,
    secondNickname: String?,
): Boolean {
    val firstKey = firstAuthorKey?.trim()?.takeIf(String::isNotEmpty)
    val secondKey = secondAuthorKey?.trim()?.takeIf(String::isNotEmpty)
    if (firstKey != null && secondKey != null) return firstKey == secondKey
    val firstName = firstNickname?.trim()?.takeIf(String::isNotEmpty) ?: return false
    val secondName = secondNickname?.trim()?.takeIf(String::isNotEmpty) ?: return false
    return firstName == secondName
}

@Composable
private fun CommentsDialog(
    post: TreeHolePost,
    comments: List<TreeHoleComment>,
    loading: Boolean,
    submitting: Boolean,
    feedback: Pair<String, Color>?,
    submissionSucceeded: Boolean,
    canMarkHelpful: Boolean,
    ownNickname: String?,
    pendingHelpfulCommentIds: Set<String>,
    ownCommentIds: Set<String>,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
    onToggleHelpful: (String) -> Unit,
    onOpenAuthor: (TreeHoleComment) -> Unit,
    onSafetyActionsPost: (() -> Unit)?,
    onSafetyActionsComment: (TreeHoleComment) -> Unit,
    onShare: (() -> Unit)?,
) {
    var draft by remember(post.id) { mutableStateOf("") }
    val badgeStrings = localizedAnswerBadgeStrings()

    // 保留草稿直到 server 成功；驗證、gate 或網路失敗都不清空。
    LaunchedEffect(submissionSucceeded) {
        if (submissionSucceeded) draft = ""
    }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.treehole_comments_title),
                    style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
                    modifier = Modifier.weight(1f),
                )
                onShare?.let {
                    PixelButton(
                        onClick = it,
                        backgroundColor = NnoworkTheme.colors.highlight,
                        modifier = Modifier.size(NnoworkSize.minTouchTarget),
                        contentPadding = PaddingValues(NnoworkSpacing.xs),
                    ) {
                        Icon(Icons.Filled.Share, "分享貼文", tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
                    }
                }
                onSafetyActionsPost?.let {
                    Spacer(Modifier.size(NnoworkSpacing.xs))
                    TreeHoleSafetyActionButton(onClick = it)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
            ) {
                Text(post.content, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
                if (loading) {
                    Text(stringResource(R.string.treehole_common_loading), style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
                } else if (comments.isEmpty()) {
                    Text(stringResource(R.string.treehole_comments_empty), style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
                } else {
                    comments.forEach { comment ->
                        val helpfulPending = comment.id in pendingHelpfulCommentIds
                        val isOwnComment = sameTreeHoleAuthor(
                            firstAuthorKey = post.authorKey,
                            secondAuthorKey = comment.authorKey,
                            firstNickname = ownNickname,
                            secondNickname = comment.nickname,
                        )
                        val canToggleThisComment = canMarkHelpful &&
                            !isOwnComment && comment.id.isNotBlank()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NnoworkTheme.colors.elevatedSurface)
                                .border(NnoworkLine.control, NnoworkTheme.colors.border, RectangleShape)
                                .padding(NnoworkSpacing.snug),
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(NnoworkSize.minTouchTarget)
                                    .clickable(
                                        enabled = !submitting,
                                        role = Role.Button,
                                        onClickLabel = stringResource(R.string.treehole_author_view_posts),
                                        onClick = { onOpenAuthor(comment) },
                                    ),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                TreeHoleInitialsAvatar(nickname = comment.nickname, size = 34.dp)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight),
                                ) {
                                    Text(
                                        comment.nickname,
                                        style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (canToggleThisComment) {
                                        val actionLabel = stringResource(
                                            if (comment.isHelpful) R.string.treehole_helpful_unmark
                                            else R.string.treehole_helpful_mark,
                                        )
                                        PixelButton(
                                            onClick = { onToggleHelpful(comment.id) },
                                            enabled = !helpfulPending,
                                            backgroundColor = if (comment.isHelpful) NnoworkTheme.colors.highlight else NnoworkTheme.colors.elevatedSurface,
                                            modifier = Modifier.size(NnoworkSize.minTouchTarget),
                                            contentPadding = PaddingValues(NnoworkSpacing.xs),
                                        ) {
                                            Icon(
                                                imageVector = if (comment.isHelpful) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                contentDescription = if (helpfulPending) {
                                                    stringResource(R.string.treehole_helpful_updating)
                                                } else {
                                                    actionLabel
                                                },
                                                tint = NnoworkTheme.colors.treeHole.noticeStroke,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    if (comment.id !in ownCommentIds) {
                                        TreeHoleSafetyActionButton(
                                            onClick = { onSafetyActionsComment(comment) },
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight),
                                ) {
                                    TreeHoleAnswerBadgeMark(
                                        exp = comment.authorExp,
                                        strings = badgeStrings,
                                    )
                                    if (comment.isHelpful) HelpfulAnswerTag()
                                }
                                comment.industry?.takeIf(String::isNotBlank)?.let { industry ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
                                    ) {
                                        Icon(
                                            treeHoleIndustryIcon(industry),
                                            contentDescription = null,
                                            tint = NnoworkTheme.colors.mutedText,
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Text(
                                            localizedIndustryLabel(industry),
                                            style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.mutedText),
                                        )
                                    }
                                }
                                Text(comment.content, style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.primaryText))
                            }
                        }
                    }
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it.take(TREE_HOLE_COMMENT_MAX_LENGTH) },
                    enabled = !submitting,
                    textStyle = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.primaryText),
                    cursorBrush = SolidColor(NnoworkTheme.colors.primaryText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                        .background(NnoworkTheme.colors.elevatedSurface)
                        .border(NnoworkLine.emphasis, NnoworkTheme.colors.border, RectangleShape)
                        .padding(NnoworkSpacing.snug),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            Text(
                                stringResource(R.string.treehole_comment_placeholder),
                                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
                            )
                        }
                        inner()
                    },
                )
                Text(
                    stringResource(R.string.treehole_comment_count, draft.length),
                    style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
                )
                feedback?.let { (message, color) ->
                    Text(message, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, color))
                }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = { onSubmit(draft) },
                enabled = draft.trim().length in 1..TREE_HOLE_COMMENT_MAX_LENGTH && !loading && !submitting,
                backgroundColor = if (draft.trim().length in 1..TREE_HOLE_COMMENT_MAX_LENGTH) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG,
            ) {
                Text(
                    stringResource(
                        if (submitting) R.string.treehole_comment_sending
                        else R.string.treehole_comment_send,
                    ),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        },
        dismissButton = {
            PixelButton(
                onClick = onDismiss,
                enabled = !submitting,
                backgroundColor = NnoworkTheme.colors.disabledBG,
            ) {
                Text(stringResource(R.string.treehole_action_close), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
    )
}

@Composable
private fun HelpfulAnswerTag() {
    Row(
        modifier = Modifier
            .background(NnoworkTheme.colors.treeHole.noticeSoft)
            .border(NnoworkLine.hairline, NnoworkTheme.colors.treeHole.noticeStroke, RectangleShape)
            .padding(horizontal = NnoworkSpacing.tight, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = NnoworkTheme.colors.treeHole.noticeStroke,
            modifier = Modifier.size(13.dp),
        )
        Text(
            stringResource(R.string.treehole_helpful_tag),
            style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.treeHole.noticeText),
        )
    }
}

@Composable
private fun PixelTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Medium, NnoworkTheme.colors.primaryText),
            cursorBrush = SolidColor(NnoworkTheme.colors.primaryText),
            modifier = Modifier
                .fillMaxWidth()
                .background(NnoworkTheme.colors.elevatedSurface)
                .border(NnoworkLine.emphasis, NnoworkTheme.colors.border, RectangleShape)
                .padding(NnoworkSpacing.sm),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.secondaryText))
                inner()
            },
        )
    }
}

@Composable
private fun treeHoleIndustryOptions(): List<TreeHoleIndustryOptionUi> =
    TreeHoleIndustryCatalog.entries.map { definition ->
        TreeHoleIndustryOptionUi(
            key = definition.canonicalValue,
            label = stringResource(definition.labelRes),
            icon = treeHoleIndustryIcon(definition.canonicalValue),
        )
    }

@Composable
private fun localizedIndustryLabel(canonicalValue: String?): String {
    val value = canonicalValue?.takeIf(String::isNotBlank) ?: return "—"
    val resource = TreeHoleIndustryCatalog.stringResFor(value)
    return resource?.let { stringResource(it) } ?: value
}

@Composable
private fun localizedAnswerBadgeStrings(): TreeHoleAnswerBadgeStrings = TreeHoleAnswerBadgeStrings(
    rookie = stringResource(R.string.treehole_answer_badge_rookie),
    passerby = stringResource(R.string.treehole_answer_badge_good_samaritan),
    senior = stringResource(R.string.treehole_answer_badge_corner_regular),
    strategist = stringResource(R.string.treehole_answer_badge_work_strategist),
    master = stringResource(R.string.treehole_answer_badge_answer_master),
    expSummaryTemplate = stringResource(R.string.treehole_answer_badge_exp_summary).androidFormatTemplate(),
    nextTierTemplate = stringResource(R.string.treehole_answer_badge_next_exp).androidFormatTemplate(),
    highestTier = stringResource(R.string.treehole_answer_badge_max),
)

@Composable
private fun localizedFeedFilterStrings(): TreeHoleFeedFilterStrings = TreeHoleFeedFilterStrings(
    all = stringResource(R.string.treehole_feed_filter_all),
    stamp = stringResource(R.string.treehole_feed_filter_mood),
    question = stringResource(R.string.treehole_feed_filter_answers),
    allIndustries = stringResource(R.string.treehole_industry_filter_all),
    openIndustryFilter = stringResource(R.string.treehole_feed_open_industry_filter),
    clearIndustryFilter = stringResource(R.string.treehole_feed_clear_industry_filter),
    selectedState = stringResource(R.string.treehole_feed_industry_selected, "").trim(),
    notSelectedState = stringResource(R.string.treehole_feed_industry_unselected, "").trim(),
)

@Composable
private fun localizedIndustryFilterStrings(): TreeHoleIndustryFilterStrings = TreeHoleIndustryFilterStrings(
    title = stringResource(R.string.treehole_industry_filter_choose),
    close = stringResource(R.string.treehole_action_close),
    all = stringResource(R.string.treehole_feed_filter_all),
    mine = stringResource(R.string.treehole_industry_filter_mine),
    clear = stringResource(R.string.treehole_feed_clear_industry_filter),
    showingAll = stringResource(R.string.treehole_industry_filter_showing_all),
    selectedCountTemplate = stringResource(R.string.treehole_industry_filter_selected_count).androidFormatTemplate(),
    applyAll = stringResource(R.string.treehole_industry_filter_show_all_posts),
    applyCountTemplate = stringResource(R.string.treehole_industry_filter_apply).androidFormatTemplate(),
    selectedState = stringResource(R.string.treehole_feed_industry_selected, "").trim(),
    notSelectedState = stringResource(R.string.treehole_feed_industry_unselected, "").trim(),
)

@Composable
private fun localizedIndustryPickerStrings(): TreeHoleIndustryPickerStrings = TreeHoleIndustryPickerStrings(
    title = stringResource(R.string.treehole_industry_filter_choose),
    close = stringResource(R.string.treehole_action_close),
    apply = stringResource(R.string.treehole_action_apply),
    selectedState = stringResource(R.string.treehole_feed_industry_selected, "").trim(),
    notSelectedState = stringResource(R.string.treehole_feed_industry_unselected, "").trim(),
)

@Composable
private fun localizedQuestionStrings(): TreeHoleQuestionStrings = TreeHoleQuestionStrings(
    title = stringResource(R.string.treehole_question_sheet_title),
    close = stringResource(R.string.treehole_question_close),
    introTitle = stringResource(R.string.treehole_question_review_notice),
    quotaUnknown = stringResource(R.string.treehole_question_quota_unknown),
    quotaRemainingTemplate = stringResource(R.string.treehole_question_quota_remaining).androidFormatTemplate(),
    quotaExhausted = stringResource(R.string.treehole_question_quota_exhausted),
    notJoined = stringResource(R.string.treehole_question_error_not_joined),
    editorLabel = stringResource(R.string.treehole_question_editor_label),
    editorPlaceholder = stringResource(R.string.treehole_question_placeholder),
    lengthHint = stringResource(R.string.treehole_question_error_invalid_length),
    submit = stringResource(R.string.treehole_question_send),
    submitting = stringResource(R.string.treehole_question_submitting),
    cancel = stringResource(R.string.treehole_question_not_now),
    avatarSuffix = stringResource(R.string.treehole_question_avatar_suffix),
)

@Composable
private fun localizedMyTreeHoleStrings(): TreeHoleMyContentStrings {
    val badgeStrings = localizedAnswerBadgeStrings()
    val all = stringResource(R.string.treehole_mine_status_all)
    val pending = stringResource(R.string.treehole_mine_status_pending)
    val approved = stringResource(R.string.treehole_mine_status_approved)
    return TreeHoleMyContentStrings(
        title = stringResource(R.string.treehole_mine_title),
        close = stringResource(R.string.treehole_action_close),
        editProfile = stringResource(R.string.treehole_profile_edit_title),
        avatarSuffix = stringResource(R.string.treehole_question_avatar_suffix),
        postsTabTemplate = stringResource(R.string.treehole_mine_post_count).androidFormatTemplate(),
        commentsTabTemplate = stringResource(R.string.treehole_mine_comment_count).androidFormatTemplate(),
        selectedState = stringResource(R.string.treehole_feed_industry_selected, "").trim(),
        notSelectedState = stringResource(R.string.treehole_feed_industry_unselected, "").trim(),
        allFilterTemplate = "$all %d",
        pendingFilterTemplate = "$pending %d",
        approvedFilterTemplate = "$approved %d",
        approvedStatus = approved,
        pendingStatus = pending,
        rejectedStatus = stringResource(R.string.treehole_mine_status_rejected),
        questionTag = stringResource(R.string.treehole_question_tag),
        deletePost = stringResource(R.string.treehole_action_delete),
        deleteComment = stringResource(R.string.treehole_action_delete),
        deletePostTitle = stringResource(R.string.treehole_mine_delete_post_title),
        deletePostBody = stringResource(R.string.treehole_mine_delete_post_body),
        deleteCommentTitle = stringResource(R.string.treehole_mine_delete_comment_title),
        deleteCommentBody = stringResource(R.string.treehole_mine_delete_comment_body),
        confirmDelete = stringResource(R.string.treehole_action_delete),
        cancel = stringResource(R.string.treehole_action_cancel),
        emptyPosts = stringResource(R.string.treehole_mine_empty_posts),
        emptyComments = stringResource(R.string.treehole_mine_empty_comments),
        loadingPosts = stringResource(R.string.treehole_common_loading),
        loadingComments = stringResource(R.string.treehole_common_loading),
        industrySyncLoading = stringResource(R.string.treehole_common_loading),
        badge = badgeStrings,
    )
}

@Composable
private fun localizedAuthorPostsStrings(): TreeHoleAuthorPostsStrings = TreeHoleAuthorPostsStrings(
    title = stringResource(R.string.treehole_author_posts_title),
    close = stringResource(R.string.treehole_author_posts_close),
    publicPosts = stringResource(R.string.treehole_author_stat_posts),
    reactions = stringResource(R.string.treehole_author_stat_reactions),
    questions = stringResource(R.string.treehole_author_stat_questions),
    loading = stringResource(R.string.treehole_author_loading),
    empty = stringResource(R.string.treehole_author_empty),
    loadFailed = stringResource(R.string.treehole_author_load_failed),
    retry = stringResource(R.string.treehole_author_retry),
    openPost = stringResource(R.string.treehole_author_open_post),
    questionTag = stringResource(R.string.treehole_question_tag),
    replyCountTemplate = stringResource(R.string.treehole_question_reply_count).androidFormatTemplate(),
    waitingForAnswer = stringResource(R.string.treehole_question_waiting_for_answer),
    like = stringResource(R.string.treehole_reaction_like),
    cheer = stringResource(R.string.treehole_reaction_cheer),
    cry = stringResource(R.string.treehole_reaction_cry),
    fallbackIndustry = stringResource(R.string.treehole_author_fallback_industry),
)

@Composable
private fun localizedQuestionError(state: TreeHoleUiState): String? = when (val result = state.questionSubmissionResult) {
    SubmitQuestionResult.NotJoined -> stringResource(R.string.treehole_question_error_not_joined)
    is SubmitQuestionResult.RateLimited -> stringResource(
        R.string.treehole_question_error_rate_limited,
        result.limit,
    )
    SubmitQuestionResult.InvalidLength -> stringResource(R.string.treehole_question_error_invalid_length)
    SubmitQuestionResult.Banned -> stringResource(R.string.treehole_author_banned)
    is SubmitQuestionResult.Rejected -> stringResource(R.string.treehole_question_error_send_failed)
    is SubmitQuestionResult.Submitted, null -> if (
        state.failure?.operation == TreeHoleFeatureOperation.SUBMIT_QUESTION
    ) {
        stringResource(R.string.treehole_question_error_network)
    } else {
        null
    }
}

@Composable
private fun localizedTreeHoleFeedback(state: TreeHoleUiState): Pair<String, Color>? {
    state.message?.takeIf(String::isNotBlank)?.let { return it to NnoworkTheme.colors.highlight }
    state.failure?.let { failure ->
        val message = when (failure.reason) {
            TreeHoleFeatureFailureReason.AUTHOR_BANNED -> stringResource(R.string.treehole_author_banned)
            TreeHoleFeatureFailureReason.COMMENT_RATE_LIMITED -> stringResource(R.string.treehole_comment_rate_limited)
            TreeHoleFeatureFailureReason.REPORT_RATE_LIMITED -> stringResource(R.string.treehole_report_rate_limited)
            TreeHoleFeatureFailureReason.REPORT_TARGET_NOT_FOUND -> stringResource(R.string.treehole_report_not_found)
            TreeHoleFeatureFailureReason.BLOCK_UNAVAILABLE -> stringResource(R.string.treehole_block_unavailable)
            TreeHoleFeatureFailureReason.INVALID_CONTENT_LENGTH -> if (
                failure.operation == TreeHoleFeatureOperation.SUBMIT_COMMENT
            ) {
                stringResource(R.string.treehole_comment_invalid_length)
            } else {
                stringResource(R.string.treehole_question_error_invalid_length)
            }
            else -> when (failure.operation) {
            TreeHoleFeatureOperation.RESET_FEED,
            TreeHoleFeatureOperation.LOAD_MORE_FEED,
            TreeHoleFeatureOperation.REFRESH,
            -> stringResource(R.string.treehole_feed_load_error)
            TreeHoleFeatureOperation.SUBMIT_QUESTION -> stringResource(R.string.treehole_question_error_send_failed)
            TreeHoleFeatureOperation.LOAD_COMMENTS -> stringResource(R.string.treehole_feed_load_error)
            TreeHoleFeatureOperation.SUBMIT_COMMENT -> stringResource(R.string.treehole_report_failed)
            TreeHoleFeatureOperation.REPORT_CONTENT -> stringResource(R.string.treehole_report_failed)
            TreeHoleFeatureOperation.BLOCK_AUTHOR -> stringResource(R.string.treehole_block_unavailable)
            TreeHoleFeatureOperation.MARK_COMMENT_HELPFUL -> stringResource(R.string.treehole_helpful_error)
            TreeHoleFeatureOperation.OPEN_NEW_ANSWER -> stringResource(R.string.treehole_new_answers_error)
            TreeHoleFeatureOperation.LOAD_AUTHOR_POSTS -> stringResource(R.string.treehole_author_load_failed)
            TreeHoleFeatureOperation.UPDATE_INDUSTRY,
            TreeHoleFeatureOperation.DECLINE_INDUSTRY_UPDATE,
            -> stringResource(R.string.treehole_industry_sync_error)
            else -> stringResource(R.string.treehole_question_error_network)
            }
        }
        return message to NnoworkTheme.colors.highlight
    }
    state.industryUpdateResult?.let { result ->
        return stringResource(
            R.string.treehole_industry_sync_success,
            result.updatedCount,
            localizedIndustryLabel(result.industry),
        ) to NnoworkTheme.colors.success
    }
    state.success?.let { success ->
        val message = when (success.kind) {
            TreeHoleFeatureSuccessKind.QUESTION_SUBMITTED -> stringResource(R.string.treehole_question_submitted)
            TreeHoleFeatureSuccessKind.POST_DELETED,
            TreeHoleFeatureSuccessKind.COMMENT_DELETED,
            -> stringResource(R.string.treehole_mine_delete_success)
            TreeHoleFeatureSuccessKind.PROFILE_SAVED -> success.affectedCount?.let { count ->
                stringResource(R.string.treehole_profile_nickname_synced, count)
            } ?: stringResource(R.string.treehole_profile_save)
            TreeHoleFeatureSuccessKind.COMMENT_SUBMITTED -> stringResource(R.string.treehole_comment_submitted)
            TreeHoleFeatureSuccessKind.HELPFUL_ANSWER_UPDATED -> stringResource(R.string.treehole_helpful_updated)
            TreeHoleFeatureSuccessKind.REPORT_SUBMITTED -> stringResource(R.string.treehole_report_success)
            TreeHoleFeatureSuccessKind.REPORT_SUBMITTED_AND_BLOCKED ->
                stringResource(R.string.treehole_report_and_block_success)
            TreeHoleFeatureSuccessKind.AUTHOR_BLOCKED -> stringResource(R.string.treehole_blocked_success)
            TreeHoleFeatureSuccessKind.COINS_AND_APPROVALS_SYNCED -> state.rewardSyncResult?.let {
                "金幣已同步：${it.coinBalance} 枚。"
            }
            else -> null
        }
        if (message != null) return message to NnoworkTheme.colors.success
    }
    return null
}

/** UI components use platform-neutral %d/%s placeholders; bridge Android positional placeholders here. */
private fun String.androidFormatTemplate(): String =
    replace("%1\$d", "%d").replace("%2\$s", "%s")

@Preview(name = "取暖樹洞", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun TreeHolePreview() {
    TreeHoleContent(
        state = TreeHoleUiState(
            profile = TreeHoleProfile("快下班的小 N", "科技業", "工程師", "血汗科技", false),
            acceptedTermsVersion = CURRENT_TREE_HOLE_TERMS_VERSION,
            feedPosts = listOf(
                TreeHolePost(
                    id = "1",
                    content = "今天會議開了兩小時，結論是明天再開一次。大家有沒有推薦的離職歌單？",
                    industry = "科技業",
                    role = "工程師",
                    nickname = "會議倖存者",
                    likesCount = 12,
                    cheerCount = 7,
                    cryCount = 3,
                    commentCount = 2,
                    latestComment = "先把履歷打開，呼吸會順一點。",
                ),
            ),
            myPosts = listOf(
                TreeHolePost(id = "2", content = "我只是想準時下班。", status = "pending", nickname = "快下班的小 N"),
            ),
            myComments = listOf(
                MyTreeHoleComment(id = "c1", content = "先把履歷打開，呼吸會順一點。", status = "approved", postContent = "今天又開了三小時會議。"),
            ),
        ),
        onAcceptTerms = {},
        onDeclineTerms = {},
        onSaveProfile = {},
        onRefresh = {},
        onFeedModeSelected = {},
        onIndustryFiltersChanged = {},
        onClearIndustryFilters = {},
        onLoadMore = {},
        onSubmitQuestion = {},
        onClearQuestionResult = {},
        onDelete = {},
        onDeleteComment = {},
        onDeleteAccount = {},
        onOpenPost = {},
        onOpenAuthor = { _, _, _ -> },
        onCloseAuthor = {},
        onRetryAuthor = {},
        onOpenNewAnswers = {},
        onClosePost = {},
        onSubmitComment = { false },
        onToggleHelpful = {},
        onSubmitReport = { _, _, _, _, _, _, _ -> },
        onBlockAuthor = { _, _ -> },
        onReact = { _, _ -> },
        onSyncCoins = {},
        onUpdateUnsetIndustries = {},
        onUpdateAllIndustries = {},
        onDeclineIndustrySync = {},
        onClearTransientFeedback = {},
        onOpenVoice = {},
        onCloseVoice = {},
        onVoiceSwipe = { _, _ -> },
        onShare = { _, _ -> },
    )
}
