package com.neilyin.nnowork.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neilyin.nnowork.data.jobarchive.JobArchiveCoding
import com.neilyin.nnowork.data.jobarchive.JobArchiveEntity
import com.neilyin.nnowork.data.jobarchive.JobArchivePayload
import com.neilyin.nnowork.data.jobarchive.JobArchivedStamp
import com.neilyin.nnowork.data.jobarchive.JobArchivedTodo
import com.neilyin.nnowork.data.jobarchive.JobReasonCount
import com.neilyin.nnowork.data.jobarchive.JobReceiptSnapshot
import com.neilyin.nnowork.data.jobarchive.JobSalarySnapshot
import com.neilyin.nnowork.data.treehole.TreeHoleComment
import com.neilyin.nnowork.data.treehole.TreeHoleIndustryCatalog
import com.neilyin.nnowork.data.treehole.TreeHolePost
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.feature.jobarchive.CurrentJobSummary
import com.neilyin.nnowork.feature.jobarchive.JobArchiveDetailScreen
import com.neilyin.nnowork.feature.jobarchive.JobTransitionContent
import com.neilyin.nnowork.feature.jobarchive.JobTransitionDiscardDialog
import com.neilyin.nnowork.feature.jobarchive.JobTransitionUiState
import com.neilyin.nnowork.feature.jobarchive.PastJobsSection
import com.neilyin.nnowork.feature.treehole.CommentsDialog
import com.neilyin.nnowork.feature.treehole.TreeHoleIndustryFilterSheet
import com.neilyin.nnowork.feature.treehole.TreeHoleIndustryOptionUi
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkPageTitle
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.appWideTheme

/**
 * Debug-only：在目前主題下直接打開正式元件，供截圖驗證外觀；不需要先同意樹洞條款、登入或連網，
 * 也不會寫入任何資料（全部是假資料）。release 建置不包含。
 *
 * adb shell am start -n com.chi157.resignationpointscard/com.neilyin.nnowork.debug.DebugSheetActivity [--es screen <名稱>]
 * screen：industry_filter（預設）、past_jobs、past_job_detail、comment_long、transition_discard
 */
class DebugSheetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent.getStringExtra("screen") ?: "industry_filter"
        setContent {
            val settings by ServiceLocator.settingsRepository.settings.collectAsState(initial = null)
            val current = settings ?: return@setContent
            val theme = CardTheme.fromId(current.cardStyle).appWideTheme(current.unlockedStyles)
            NnoworkTheme(cardTheme = theme) {
                when (screen) {
                    "past_jobs" -> PageWithTitle("離職紀錄") {
                        PastJobsSection(archives = DebugJobArchiveSamples.archives, onSelect = {})
                    }
                    "past_job_detail" -> JobArchiveDetailScreen(
                        archive = DebugJobArchiveSamples.archives.first(),
                        onBack = { finish() },
                    )
                    "comment_long" -> {
                        PageWithTitle("取暖樹洞") {}
                        CommentsDialog(
                            post = DebugJobArchiveSamples.secondRunPost,
                            comments = listOf(
                                TreeHoleComment(id = "c1", nickname = "同路人", industry = "科技業", content = "抱抱你，我上一份工作也是這樣，後來換工作真的好很多。"),
                            ),
                            loading = false,
                            submitting = false,
                            feedback = null,
                            submissionSucceeded = false,
                            canMarkHelpful = false,
                            ownNickname = null,
                            pendingHelpfulCommentIds = emptySet(),
                            ownCommentIds = emptySet(),
                            onDismiss = { finish() },
                            onSubmit = { false },
                            onToggleHelpful = {},
                            onOpenAuthor = {},
                            onSafetyActionsPost = null,
                            onSafetyActionsComment = {},
                            onShare = null,
                            initialDraft = DebugJobArchiveSamples.longComment,
                        )
                    }
                    "transition_discard" -> {
                        Box(Modifier.fillMaxSize().background(NnoworkTheme.colors.elevatedSurface).statusBarsPadding()) {
                            JobTransitionContent(
                                state = JobTransitionUiState(companyName = "新的開始股份有限公司"),
                                currentJob = CurrentJobSummary("血汗科技", stampCount = 23, cardCount = 3),
                                isSignedIn = false,
                                onCompanyNameChange = {},
                                onSelectMaxStamps = {},
                                onArchive = {},
                                onLinkAccount = {},
                                onRequestDiscard = {},
                                onCancel = { finish() },
                            )
                        }
                        JobTransitionDiscardDialog(
                            companyName = "血汗科技",
                            stampCount = 23,
                            onConfirm = { finish() },
                            onDismiss = { finish() },
                        )
                    }
                    else -> {
                        PageWithTitle("取暖樹洞") {}
                        val options = TreeHoleIndustryCatalog.entries.take(8).map {
                            TreeHoleIndustryOptionUi(key = it.canonicalValue, label = stringResource(it.labelRes))
                        }
                        TreeHoleIndustryFilterSheet(
                            industries = options,
                            selectedIndustries = options.take(2).map { it.key }.toSet(),
                            myIndustry = options.first().key,
                            onApply = { finish() },
                            onDismiss = { finish() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageWithTitle(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(NnoworkTheme.colors.appBG)
            .statusBarsPadding()
            .padding(NnoworkSpacing.page),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
    ) {
        NnoworkPageTitle(title)
        content()
    }
}

/** 截圖用的假封存與貼文。 */
private object DebugJobArchiveSamples {
    private fun stamp(date: String, reason: String, round: Int, egg: Boolean = false) =
        JobArchivedStamp(date = date, reason = reason, roundNumber = round, isEasterEgg = egg)

    private val masterPayload = JobArchivePayload(
        jobIndex = 2,
        companyName = "血汗科技",
        startDate = "2025-03-03T01:00:00Z",
        endDate = "2026-10-31T10:00:00Z",
        cardCount = 3,
        stampCount = 6,
        topReasons = listOf(
            JobReasonCount("無效會議", 3),
            JobReasonCount("需求一直改", 2),
            JobReasonCount("老闆廢話太多", 1),
        ),
        savedAmount = 180_000.0,
        targetAmount = 300_000.0,
        salary = JobSalarySnapshot(
            payMode = 0,
            monthlySalary = 52_000.0,
            hourlyRate = 0.0,
            currencySymbol = "NT$",
            hourlyManualMode = false,
            startMinutes = 9 * 60,
            endMinutes = 18 * 60,
        ),
        receipt = JobReceiptSnapshot(
            kind = JobReceiptSnapshot.KIND_MASTER,
            companyName = "血汗科技",
            firstStampDate = "2025-03-03T01:00:00Z",
            completionDate = "2026-10-31T10:00:00Z",
            totalDays = 607,
            totalStamps = 6,
            savedAmount = 180_000.0,
            issuedAt = "2026-10-31T10:00:00Z",
        ),
        stamps = listOf(
            stamp("2025-03-03T01:00:00Z", "無效會議", 1),
            stamp("2025-06-12T01:00:00Z", "需求一直改", 1),
            stamp("2025-11-20T01:00:00Z", "無效會議", 2),
            stamp("2026-02-14T01:00:00Z", "老闆廢話太多", 2, egg = true),
            stamp("2026-07-01T01:00:00Z", "需求一直改", 3),
            stamp("2026-10-30T01:00:00Z", "無效會議", 3),
        ),
        todos = listOf(JobArchivedTodo("更新履歷", isCompleted = true), JobArchivedTodo("約前同事吃飯", isCompleted = false)),
        createdAt = "2026-10-31T10:00:00Z",
    )

    private val firstPayload = masterPayload.copy(
        jobIndex = 1,
        companyName = "第一間公司",
        startDate = "2023-07-01T01:00:00Z",
        endDate = "2025-02-28T10:00:00Z",
        cardCount = 1,
        stampCount = 10,
        receipt = masterPayload.receipt.copy(kind = JobReceiptSnapshot.KIND_RESIGNATION, companyName = "第一間公司"),
        createdAt = "2025-02-28T10:00:00Z",
    )

    val archives: List<JobArchiveEntity> = listOf(masterPayload to "debug-2", firstPayload to "debug-1").map { (payload, id) ->
        JobArchiveEntity(
            id = id,
            ownerUserId = "debug",
            jobIndex = payload.jobIndex,
            companyName = payload.companyName,
            endDate = 0L,
            createdAt = 0L,
            payloadJson = JobArchiveCoding.encodePayload(payload),
        )
    }

    val secondRunPost = TreeHolePost(
        id = "debug-post",
        content = "換到新公司第一週，主管說「我們很重視 work-life balance」，然後晚上十點在群組 @ 全部人。",
        industry = "科技業",
        nickname = "二周目社畜",
        roundNumber = 1,
        stampCount = 3,
        jobIndex = 2,
        commentCount = 1,
    )

    val longComment = buildString {
        append("先抱抱你。我也是換工作後才發現，原來不是每間公司都這樣。")
        append("我的建議是：第一，先把工時跟訊息紀錄留下來；第二，找機會跟主管一對一聊期待，講清楚下班後非緊急的事隔天處理；")
        append("第三，真的不行就把這份工作當跳板，存好預備金再走。")
        append("留言現在不限字數了，所以我多寫一點：我之前在群組被 @ 到半夜，後來設定勿擾、只留電話給真的緊急的事，")
        append("一開始很焦慮，但兩週後大家就習慣了。你已經很努力了，記得照顧自己。")
    }
}
