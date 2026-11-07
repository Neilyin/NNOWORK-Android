package com.neilyin.nnowork.feature.home

import com.neilyin.nnowork.data.treehole.TreeHoleIndustryCatalog
import com.neilyin.nnowork.feature.treehole.TreeHoleProgressRow
import com.neilyin.nnowork.feature.treehole.TreeHoleProgressStyle
import com.neilyin.nnowork.feature.treehole.treeHoleProgressText
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import com.neilyin.nnowork.ui.theme.cardShadowColor
import com.neilyin.nnowork.ui.theme.softShadow
import com.neilyin.nnowork.ui.theme.NnoworkSize
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Brush
import com.neilyin.nnowork.ui.theme.NnoworkDialogDefaults
import com.neilyin.nnowork.R
import com.neilyin.nnowork.feature.history.FreedomScreen
import com.neilyin.nnowork.feature.history.computeFreedomStats
import com.neilyin.nnowork.feature.jobarchive.JobTransitionSheet
import com.neilyin.nnowork.ui.theme.PixelButtonVariant
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.AnimatedVisibility
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.semantics.semantics
import com.neilyin.nnowork.ui.theme.NnoworkCardBorderAlpha
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.saveable.rememberSaveable
import com.neilyin.nnowork.ui.adaptive.HomeLayout
import com.neilyin.nnowork.ui.adaptive.LocalNnoworkWindowInfo
import com.neilyin.nnowork.ui.adaptive.NnoworkAdaptive
import com.neilyin.nnowork.ui.adaptive.NnoworkTwoPane
import com.neilyin.nnowork.ui.adaptive.readableWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.PixelBadge
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.NnoworkElevation
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.ThemeStampMark
import com.neilyin.nnowork.ui.theme.palette
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import com.neilyin.nnowork.ui.theme.stampTiltDegrees
import com.neilyin.nnowork.util.DateUtils
import kotlinx.coroutines.delay

/**
 * @param onMasterCompleted 舊版「忍耐大師 → 歷史頁收據」的出口。iOS 第 3 張起改成在首頁看階段總結、
 * 自己選「再來一張／勇敢離職」，所以首頁已不再呼叫它；保留參數讓導覽層不用跟著改。
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    onCardCompleted: () -> Unit = {},
    onMasterCompleted: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val theme = state.theme
    val haptic = LocalHapticFeedback.current

    // 對話框開關與彩蛋流程用 rememberSaveable：摺疊／展開、旋轉重建 Activity 後仍開著（輸入中的原因在 StampInputDialog 內）。
    var showStampDialog by rememberSaveable { mutableStateOf(false) }
    var showAnimation by remember { mutableStateOf(false) }
    var showMasterOverlay by rememberSaveable { mutableStateOf(false) }
    var showCompletionDialog by rememberSaveable { mutableStateOf(false) }
    // 忍耐大師流程（iOS HomeView）：里程碑遮罩 → 階段總結 →「勇敢離職」→ 恭喜離職成功 → 自由畫面 → 換工作。
    var showMasterSummary by rememberSaveable { mutableStateOf(false) }
    var showResignCongrats by rememberSaveable { mutableStateOf(false) }
    var showFreedom by rememberSaveable { mutableStateOf(false) }
    var showJobTransition by rememberSaveable { mutableStateOf(false) }
    // StampCreationResult 不是 Parcelable，且分享對話框只在剛蓋完章時出現，維持 remember。
    var pendingTreeHoleShare by remember { mutableStateOf<StampCreationResult?>(null) }
    var treeHoleShareMessage by remember { mutableStateOf<String?>(null) }
    var isTreeHoleSharing by remember { mutableStateOf(false) }
    var easterTapCount by rememberSaveable { mutableStateOf(0) }
    var showEasterDialog by rememberSaveable { mutableStateOf(false) }
    var isEasterFlow by rememberSaveable { mutableStateOf(false) }

    val onEasterTap: () -> Unit = {
        if (state.hasStampedToday && !state.isCurrentCardComplete) {
            easterTapCount++
            if (easterTapCount >= 10) {
                easterTapCount = 0
                showEasterDialog = true
            }
        }
    }
    val onStamp: () -> Unit = { showStampDialog = true }
    val onNext: () -> Unit = {
        if (state.settings.currentRound >= 3) showMasterOverlay = true else showCompletionDialog = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NnoworkTheme.colors.appBG, NnoworkTheme.colors.appBGBottom)),
            ),
    ) {
        when (LocalNnoworkWindowInfo.current.homeLayout) {
            HomeLayout.Phone -> PhoneHomeContent(state, theme, onEasterTap, onStamp, onNext)
            HomeLayout.WideStacked -> WideStackedHomeContent(state, theme, onEasterTap, onStamp, onNext)
            HomeLayout.WideSideBySide -> WideSideBySideHomeContent(state, theme, onEasterTap, onStamp, onNext)
        }

        AnimatedVisibility(visible = showAnimation) {
            StampSuccessOverlay(theme)
        }

        if (showMasterOverlay) {
            MasterMilestoneOverlay(
                round = state.settings.currentRound,
                onOpen = {
                    showMasterOverlay = false
                    showMasterSummary = true
                },
            )
        }
    }

    if (showMasterSummary) {
        MasterLevelSummaryDialog(
            stats = computeMasterSummaryStats(state.settings, state.stamps, state.savedAmount),
            onDismiss = { showMasterSummary = false },
            onStartNewCard = { maxStamps ->
                showMasterSummary = false
                viewModel.startNewCard(maxStamps)
            },
            onQuit = {
                // 集滿時 addStamp 已記下 cardCompletedDate／roundCompletedDates（iOS 這裡才補記），不用再寫一次。
                showMasterSummary = false
                showResignCongrats = true
            },
        )
    }

    if (showResignCongrats) {
        AlertDialog(
            onDismissRequest = { showResignCongrats = false },
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = {
                Text(
                    stringResource(R.string.master_congrats_title),
                    style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated),
                )
            },
            text = {
                Text(
                    stringResource(R.string.master_congrats_message),
                    style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated),
                )
            },
            confirmButton = {
                PixelButton(
                    onClick = {
                        showResignCongrats = false
                        showFreedom = true
                    },
                    variant = PixelButtonVariant.Primary,
                ) {
                    Text(stringResource(R.string.master_congrats_action), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold))
                }
            },
        )
    }

    if (showFreedom) {
        // 自由畫面全螢幕蓋在首頁上（iOS fullScreenCover）；「重新集點（新公司）」走既有換工作流程。
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFreedom = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            FreedomScreen(
                stats = computeFreedomStats(state.settings, state.stamps),
                onViewHistory = { showFreedom = false },
                onStartNewCompany = { showJobTransition = true },
            )
        }
    }

    if (showJobTransition) {
        JobTransitionSheet(
            onDismiss = { showJobTransition = false },
            onCompleted = {
                showJobTransition = false
                showFreedom = false
            },
        )
    }

    if (showStampDialog) {
        StampInputDialog(
            theme = theme,
            commonReasons = state.settings.commonReasons,
            onDismiss = {
                showStampDialog = false
                isEasterFlow = false
            },
            onConfirm = { reason, addToCommon ->
                showStampDialog = false
                val onResult: (StampCreationResult) -> Unit = { result ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAnimation = true
                    pendingTreeHoleShare = result
                    if (result.justCompleted) {
                        if (result.roundNumber >= 3) showMasterOverlay = true else showCompletionDialog = true
                    }
                }
                if (isEasterFlow) {
                    isEasterFlow = false
                    viewModel.addEasterEggStamp(reason, addToCommon, onResult)
                } else {
                    viewModel.addStamp(reason, addToCommon, onResult)
                }
            },
        )
    }

    pendingTreeHoleShare?.let { stamp ->
        // iOS TreeHoleShareSheet：行業名稱後接「· （周目 ·）第 N 張卡 · 第 M 個印章」，用蓋章當下的快照。
        val industry = state.treeHoleProfile?.industry?.takeIf(String::isNotBlank)?.let { value ->
            TreeHoleIndustryCatalog.stringResFor(value)?.let { stringResource(it) } ?: value
        }
        val progress = treeHoleProgressText(stamp.roundNumber, stamp.stampCount, stamp.runNumber, TreeHoleProgressStyle.SHEET)
        TreeHoleShareDialog(
            content = stamp.shareContent,
            progressLabel = progress?.let { listOfNotNull(industry, it).joinToString(" ") },
            hasProfile = state.treeHoleProfile?.isComplete == true,
            isSharing = isTreeHoleSharing,
            message = treeHoleShareMessage,
            onDismiss = {
                if (!isTreeHoleSharing) {
                    pendingTreeHoleShare = null
                    treeHoleShareMessage = null
                }
            },
            onShare = {
                if (!isTreeHoleSharing) {
                    isTreeHoleSharing = true
                    viewModel.shareToTreeHole(stamp) { message ->
                        isTreeHoleSharing = false
                        treeHoleShareMessage = message
                        if (message.startsWith("已")) {
                            pendingTreeHoleShare = null
                        }
                    }
                }
            },
        )
    }

    if (showCompletionDialog) {
        CardCompletionDialog(
            onNewCard = { newMax ->
                showCompletionDialog = false
                viewModel.startNewCard(newMax)
            },
            onReceipt = {
                showCompletionDialog = false
                onCardCompleted()
            },
            onDismiss = { showCompletionDialog = false },
        )
    }

    if (showEasterDialog) {
        AlertDialog(
            onDismissRequest = { showEasterDialog = false },
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = { Text("😤 看起來你很生氣", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.highlight)) },
            text = {
                Text(
                    "戳這麼多下…要再多蓋一個章嗎？\n（這次不算每日上限，但也不會多給金幣）",
                    style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated),
                )
            },
            confirmButton = {
                PixelButton(
                    onClick = {
                        showEasterDialog = false
                        isEasterFlow = true
                        showStampDialog = true
                    },
                    backgroundColor = NnoworkTheme.colors.warning,
                ) {
                    Text("再蓋一個！", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            },
            dismissButton = {
                PixelButton(onClick = { showEasterDialog = false }, backgroundColor = NnoworkTheme.colors.disabledBG) {
                    Text("算了", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            },
        )
    }

    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            delay(1000)
            showAnimation = false
        }
    }
}

/** 一般手機直向：原本版面（不變）。 */
@Composable
private fun BoxScope.PhoneHomeContent(
    state: HomeUiState,
    theme: CardTheme,
    onEasterTap: () -> Unit,
    onStamp: () -> Unit,
    onNext: () -> Unit,
) {
    // 金幣 HUD（右上角）
    CoinHud(
        coins = state.settings.coinCount,
        theme = theme,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = NnoworkSpacing.xs, end = NnoworkSpacing.md),
    )

    // 版面對齊 iOS HomeView（直向）：標題靠上，回合列＋集點卡＋按鈕成一組在剩餘空間置中。
    // 集點卡依內容高度（不再撐滿整個畫面），空間不夠（例如 30 格的小螢幕）時整頁可捲動。
    BoxWithConstraints(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .widthIn(max = 640.dp)
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(horizontal = NnoworkSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            // SpaceBetween＋最小高度＝視窗高：標題貼頂、卡片＋按鈕這組落在剩餘空間中間。
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(NnoworkSpacing.md))
                HeaderSection(state, theme)
                Spacer(Modifier.height(NnoworkSpacing.lg))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StampCardSection(state = state, theme = theme, onEasterTap = onEasterTap)
                Spacer(Modifier.height(NnoworkSpacing.lg))
                BottomButton(state = state, theme = theme, onStamp = onStamp, onNext = onNext)
            }
            Spacer(Modifier.height(NnoworkSpacing.xl))
        }
    }
}

/**
 * 寬螢幕直立（摺疊機展開直向）：iOS wideStackedContent。
 * 標題列（公司＋金幣）→ 日期 → 集點卡 → 回合 → 進度統計 → 按鈕，限寬 560 置中，可捲動。
 */
@Composable
private fun WideStackedHomeContent(
    state: HomeUiState,
    theme: CardTheme,
    onEasterTap: () -> Unit,
    onStamp: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .readableWidth(NnoworkAdaptive.homeStackedMaxWidth + NnoworkSpacing.xl * 2)
                .padding(horizontal = NnoworkSpacing.xl, vertical = NnoworkSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.page),
        ) {
            WideTitleRow(state, theme)
            DateChip()
            StampCardBox(state = state, theme = theme, onEasterTap = onEasterTap)
            RoundIndicator(state)
            HomeWideInfoPanel(state, showsRecentRecords = true)
            BottomButton(state = state, theme = theme, onStamp = onStamp, onNext = onNext)
        }
    }
}

/**
 * 寬螢幕橫向（或高度不足）：iOS wideSideBySideContent。
 * 上方標題列（公司＋金幣，對應 iOS 導覽列那一排）；下方左集點卡、右日期＋回合＋進度統計＋最近紀錄＋按鈕。
 * 分界走 [NnoworkTwoPane]（有摺痕時對準摺痕）。
 */
@Composable
private fun WideSideBySideHomeContent(
    state: HomeUiState,
    theme: CardTheme,
    onEasterTap: () -> Unit,
    onStamp: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = NnoworkSpacing.lg)
            .padding(top = NnoworkSpacing.md),
    ) {
        WideTitleRow(state, theme)
        Spacer(Modifier.height(NnoworkSpacing.md))
        // 集點卡的格子大小由欄寬決定，左欄給 57% 讓卡片放大；有摺痕時仍對準摺痕。
        NnoworkTwoPane(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            ratio = 0.57f,
            spacing = NnoworkSpacing.xl,
            showDivider = false,
            first = {
                // 左欄集點卡在可用高度內垂直置中，太矮時可捲動。
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val viewportHeight = maxHeight
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = viewportHeight)
                            .padding(bottom = NnoworkSpacing.md),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        StampCardBox(state = state, theme = theme, onEasterTap = onEasterTap)
                    }
                }
            },
            second = {
                // 右欄限寬 440 置中，和左欄一樣在可用高度內垂直置中（不留底部大片空白），太矮時可捲動。
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val viewportHeight = maxHeight
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = viewportHeight),
                        verticalArrangement = Arrangement.Center,
                    ) {
                    Column(
                        modifier = Modifier
                            .readableWidth(NnoworkAdaptive.homeSecondaryMaxWidth)
                            .padding(bottom = NnoworkSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
                    ) {
                        DateChip()
                        RoundIndicator(state)
                        HomeWideInfoPanel(state, showsRecentRecords = true)
                        Spacer(Modifier.height(NnoworkSpacing.xs))
                        BottomButton(state = state, theme = theme, onStamp = onStamp, onNext = onNext)
                    }
                    }
                }
            },
        )
    }
}

/** 寬螢幕標題列：左公司標題（iOS DuoNavTitle）、右金幣（iOS DuoNavPlainItem）。 */
@Composable
private fun WideTitleRow(state: HomeUiState, theme: CardTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = NnoworkSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val company = state.settings.companyName.ifBlank { "公司" }
        Text(
            "$company 離職集點卡",
            style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(NnoworkSpacing.md))
        CoinHud(coins = state.settings.coinCount, theme = theme)
    }
}

/**
 * 寬螢幕右欄的第二層資訊（iOS homeWideInfoPanel）：本張進度條、累計統計、最近 3 筆蓋章。
 * 純顯示，只讀既有資料。
 */
@Composable
private fun HomeWideInfoPanel(state: HomeUiState, showsRecentRecords: Boolean) {
    val maxStamps = state.settings.maxStamps.coerceAtLeast(1)
    val progress = state.currentRoundStamps.toFloat() / maxStamps
    val recent = remember(state.stamps) { state.stamps.sortedByDescending { it.date }.take(3) }
    val enduredDays = remember(state.stamps) {
        state.stamps.minOfOrNull { it.date }?.let { first ->
            ((DateUtils.startOfDay(System.currentTimeMillis()) - DateUtils.startOfDay(first)) / 86_400_000L)
                .coerceAtLeast(0L).toInt()
        } ?: 0
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(NnoworkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(NnoworkSpacing.sm)
                    .clip(RoundedCornerShape(NnoworkRadius.small))
                    .background(NnoworkTheme.colors.disabledBG),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(NnoworkRadius.small))
                        .background(NnoworkTheme.colors.accent),
                )
            }
            Spacer(Modifier.width(NnoworkSpacing.sm))
            Text(
                "${(progress * 100).toInt()}%",
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
            WideStatTile(Icons.Filled.CheckCircle, state.stamps.size.toString(), "總蓋章數", Modifier.weight(1f))
            WideStatTile(Icons.Filled.CalendarMonth, "$enduredDays 天", "忍耐天數", Modifier.weight(1f))
        }
        if (showsRecentRecords && recent.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
                Text(
                    "離職紀錄",
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText),
                )
                recent.forEach { record ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            DateUtils.format("M/d", record.date),
                            style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.secondaryText, monospace = true),
                            modifier = Modifier.width(NnoworkSize.minTouchTarget),
                        )
                        Text(
                            record.reason,
                            style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.primaryText),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (record.isEasterEgg) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = NnoworkTheme.colors.warning,
                                modifier = Modifier.size(NnoworkSize.compactIcon),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WideStatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(NnoworkLine.hairline, NnoworkTheme.colors.divider, RoundedCornerShape(NnoworkRadius.small))
            .padding(vertical = NnoworkSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
    ) {
        Icon(icon, contentDescription = null, tint = NnoworkTheme.colors.accent, modifier = Modifier.size(NnoworkSize.compactIcon))
        Text(value, style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true), maxLines = 1)
        Text(label, style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.SemiBold, NnoworkTheme.colors.secondaryText), maxLines = 1)
    }
}

@Composable
private fun CoinHud(
    coins: Int,
    theme: CardTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .pixelCard(shape = RoundedCornerShape(50), elevation = NnoworkElevation.control)
            .padding(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.MonetizationOn,
            contentDescription = "離職金幣",
            tint = NnoworkTheme.colors.accent,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            coins.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = NnoworkTheme.colors.primaryText,
            ),
        )
    }
}

@Composable
private fun TreeHoleShareDialog(
    content: String,
    progressLabel: String?,
    hasProfile: Boolean,
    isSharing: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSharing) onDismiss() },
        containerColor = NnoworkDialogDefaults.containerColor,
        shape = NnoworkDialogDefaults.shape,
        title = { Text("分享到取暖樹洞？", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Park, null, tint = NnoworkTheme.colors.success, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(NnoworkSpacing.xs))
                    Text("審核通過後會公開，並獲得離職金幣。", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
                }
                Text(content, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                progressLabel?.let { TreeHoleProgressRow(it) }
                if (!hasProfile) {
                    Text("你還沒建立樹洞身份，先到「樹洞」分頁設定暱稱、產業與職稱。", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.highlight))
                }
                message?.let {
                    Text(it, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, if (it.startsWith("已")) NnoworkTheme.colors.success else NnoworkTheme.colors.highlight))
                }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = onShare,
                enabled = hasProfile && !isSharing,
                backgroundColor = if (hasProfile) NnoworkTheme.colors.success else NnoworkTheme.colors.disabledBG,
            ) {
                Text(
                    if (isSharing) "分享中…" else "分享",
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, enabled = !isSharing, backgroundColor = NnoworkTheme.colors.disabledBG) {
                Text("略過", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
    )
}

@Composable
private fun CardCompletionDialog(
    onNewCard: (newMax: Int) -> Unit,
    onReceipt: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedMax by rememberSaveable { mutableStateOf(10) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(NnoworkDialogDefaults.shape)
                .background(NnoworkDialogDefaults.containerColor)
                .padding(NnoworkSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.Celebration, contentDescription = null, tint = NnoworkTheme.colors.accent, modifier = Modifier.size(NnoworkSize.icon * 2))
            Spacer(Modifier.height(NnoworkSpacing.xs))
            Text("集滿一張卡！", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.highlight))
            Spacer(Modifier.height(NnoworkSpacing.md))
            Text("下一張要幾格？", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, color = NnoworkTheme.colors.onElevated))
            Spacer(Modifier.height(NnoworkSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                listOf(10, 20, 30).forEach { n ->
                    PixelButton(
                        onClick = { selectedMax = n },
                        backgroundColor = if (selectedMax == n) NnoworkTheme.colors.success else NnoworkTheme.colors.disabledBG,
                    ) {
                        Text("$n", style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                }
            }
            Spacer(Modifier.height(NnoworkSpacing.page))
            PixelButton(onClick = { onNewCard(selectedMax) }, backgroundColor = NnoworkTheme.colors.buttonBG, modifier = Modifier.fillMaxWidth()) {
                Text("開始新的一張", style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.onElevated))
            }
            Spacer(Modifier.height(NnoworkSpacing.xs))
            androidx.compose.material3.TextButton(onClick = onReceipt) {
                Text("查看離職收據", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.accentForeground))
            }
        }
    }
}

@Composable
private fun HeaderSection(state: HomeUiState, theme: CardTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val company = state.settings.companyName.ifBlank { "公司" }
        Text(
            "$company 離職集點卡",
            style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(NnoworkSpacing.sm))
        DateChip()
    }
}

@Composable
private fun DateChip() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(NnoworkRadius.small))
            .background(NnoworkTheme.colors.accentSoft)
            .padding(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.xxs),
    ) {
        Text(
            DateUtils.formattedToday(),
            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.accentForeground, monospace = true),
        )
    }
}

@Composable
private fun StampCardSection(
    state: HomeUiState,
    theme: CardTheme,
    modifier: Modifier = Modifier,
    onEasterTap: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RoundIndicator(state)
        Spacer(Modifier.height(NnoworkSpacing.md))
        StampCardBox(state = state, theme = theme, onEasterTap = onEasterTap)
    }
}

/** 「第 N 張卡片｜x / y」回合列（iOS roundIndicator）。 */
@Composable
private fun RoundIndicator(state: HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "第 ${state.settings.currentRound} 張卡片",
            style = pixelTextStyle(
                NnoworkTheme.type.subtitle,
                FontWeight.Bold,
                NnoworkTheme.colors.primaryText,
            ),
        )
        Spacer(Modifier.weight(1f))
        PixelBadge(backgroundColor = NnoworkTheme.colors.accent) {
            Text(
                "${state.currentRoundStamps} / ${state.settings.maxStamps}",
                style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.onAccent, monospace = true),
            )
        }
    }
}

/** 集點卡主視覺＋集滿橫幅（iOS stampCardWithBanner）。 */
@Composable
private fun StampCardBox(
    state: HomeUiState,
    theme: CardTheme,
    onEasterTap: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 卡片高度貼合格子（iOS stampCard：padding 24、格子上限 70pt），不再撐滿畫面留大片空白。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 集點卡主視覺保留像素品牌：主題底色＋粗框（emphasis 3dp），外圍用柔和投影取代 Material 硬陰影。
                .softShadow(RoundedCornerShape(NnoworkRadius.hero), cardShadowColor(theme.cardSurfaceColor))
                .clip(RoundedCornerShape(NnoworkRadius.hero))
                .background(theme.cardSurfaceColor)
                .border(
                    if (theme == CardTheme.NOTION) NnoworkLine.hairline else NnoworkLine.emphasis,
                    theme.borderColor,
                    RoundedCornerShape(NnoworkRadius.hero),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEasterTap,
                )
                .padding(NnoworkSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            StampGrid(state, theme)
        }

        if (state.isCurrentCardComplete) {
            Spacer(Modifier.height(NnoworkSpacing.sm))
            CompletionBanner(state.isMasterLevel)
        }
    }
}

private val StampCellMaxSize = 70.dp

@Composable
private fun StampGrid(state: HomeUiState, theme: CardTheme) {
    val maxStamps = state.settings.maxStamps
    val columns = if (maxStamps == 30) 6 else 5
    val rows = (maxStamps + columns - 1) / columns
    val stamped = state.currentRoundStamps
    val gap = 8.dp

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // 格子大小由卡片寬度決定，上限 StampCellMaxSize（iOS 70pt），卡片高度跟著格子走。
        val cellByWidth = (maxWidth - gap * (columns - 1)) / columns
        val cell = minOf(cellByWidth, StampCellMaxSize)

        Column(
            verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (r in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until columns) {
                        val index = r * columns + c
                        if (index < maxStamps) {
                            StampCell(
                                index = index,
                                isStamped = index < stamped,
                                theme = theme,
                                modifier = Modifier.size(cell),
                            )
                        } else {
                            Spacer(Modifier.size(cell))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StampCell(index: Int, isStamped: Boolean, theme: CardTheme, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(maxOf(theme.cellCornerRadius, NnoworkRadius.small))
    val fill = if (isStamped) theme.stampedCellColor else theme.emptyCellColor
    val borderOpacity = if (isStamped) theme.stampedCellBorderOpacity else theme.emptyCellBorderOpacity
    val rotation = stampTiltDegrees(index)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(fill)
            .border(NnoworkLine.control, theme.borderColor.copy(alpha = borderOpacity), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (isStamped) {
            ThemeStampMark(
                theme = theme,
                index = index,
                contentDescription = null,
                tint = theme.stampIconColor,
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .rotate(rotation),
            )
        } else {
            Text(
                "${index + 1}",
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, theme.emptyCellTextColor, monospace = true),
            )
        }
    }
}

@Composable
private fun CompletionBanner(isMaster: Boolean) {
    PixelBadge(backgroundColor = if (isMaster) NnoworkTheme.colors.special else NnoworkTheme.colors.warning) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isMaster) Icons.Filled.WorkspacePremium else Icons.Filled.Star,
                contentDescription = null,
                tint = NnoworkTheme.colors.onElevated,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(NnoworkSpacing.xs))
            Text(
                if (isMaster) "大師級社畜" else "集滿啦！",
                style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated),
            )
        }
    }
}

@Composable
private fun BottomButton(
    state: HomeUiState,
    theme: CardTheme,
    onStamp: () -> Unit,
    onNext: () -> Unit,
) {
    if (state.hasStampedToday && !state.isCurrentCardComplete && !state.isMasterLevel) {
        StampedTodayStatus()
        return
    }
    val bg: Color
    val text: String
    val icon = when {
        state.isCurrentCardComplete -> Icons.AutoMirrored.Filled.ArrowForward
        state.hasStampedToday -> Icons.Filled.Check
        else -> theme.buttonActionIcon
    }
    when {
        state.isMasterLevel -> { bg = NnoworkTheme.colors.special; text = "What's Next" }
        state.isCurrentCardComplete -> { bg = NnoworkTheme.colors.buttonBG; text = "What's Next" }
        state.hasStampedToday -> { bg = NnoworkTheme.colors.disabledBG; text = "今天已蓋章" }
        else -> {
            bg = NnoworkTheme.colors.buttonBG
            text = "我要蓋章"
        }
    }
    PixelButton(
        onClick = { if (state.isCurrentCardComplete) onNext() else if (state.canStamp) onStamp() },
        backgroundColor = bg,
        enabled = state.isCurrentCardComplete || state.canStamp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // 字色由 PixelButton 依底色提供（LocalContentColor），不再各自指定。
        val contentColor = LocalContentColor.current
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(NnoworkSpacing.sm))
            Text(text, style = MaterialTheme.typography.titleLarge.copy(color = contentColor))
            Spacer(Modifier.width(NnoworkSpacing.sm))
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * 「今天已蓋章」是狀態不是動作：不畫成半透明的停用按鈕（對比不足 4.5），
 * 改成 accentSoft 底＋accentForeground 字的狀態列（與首頁日期標籤同組配色，已驗過 AA）。
 */
@Composable
private fun StampedTodayStatus() {
    val shape = RoundedCornerShape(NnoworkRadius.button)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NnoworkSize.controlHeight)
            .clip(shape)
            .background(NnoworkTheme.colors.accentSoft, shape)
            .border(NnoworkLine.hairline, NnoworkTheme.colors.accent.copy(alpha = NnoworkCardBorderAlpha), shape)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val color = NnoworkTheme.colors.accentForeground
        Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(NnoworkSize.icon))
        Spacer(Modifier.width(NnoworkSpacing.sm))
        Text("今天已蓋章", style = MaterialTheme.typography.titleLarge.copy(color = color))
        Spacer(Modifier.width(NnoworkSpacing.sm))
        Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(NnoworkSize.icon))
    }
}

@Composable
private fun StampSuccessOverlay(theme: CardTheme) {
    var target by remember { mutableFloatStateOf(0.5f) }
    val animScale by animateFloatAsState(target, spring(dampingRatio = 0.6f), label = "stampScale")
    LaunchedEffect(Unit) { target = 1.1f }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NnoworkTheme.colors.scrim.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(animScale)
                .clip(CircleShape)
                .background(NnoworkTheme.colors.elevatedSurface)
                .border(6.dp, theme.primaryColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ThemeStampMark(
                    theme = theme,
                    index = 0,
                    contentDescription = null,
                    tint = theme.primaryColor,
                    modifier = Modifier.size(50.dp),
                )
                Text("蓋章成功", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, theme.primaryColor))
            }
        }
    }
}
