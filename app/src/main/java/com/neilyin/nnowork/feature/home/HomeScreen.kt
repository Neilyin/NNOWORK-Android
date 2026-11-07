package com.neilyin.nnowork.feature.home

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

@Composable
fun HomeScreen(
    onCardCompleted: () -> Unit = {},
    onMasterCompleted: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val theme = state.theme
    val haptic = LocalHapticFeedback.current

    var showStampDialog by remember { mutableStateOf(false) }
    var showAnimation by remember { mutableStateOf(false) }
    var showMasterOverlay by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var pendingTreeHoleShare by remember { mutableStateOf<StampCreationResult?>(null) }
    var treeHoleShareMessage by remember { mutableStateOf<String?>(null) }
    var isTreeHoleSharing by remember { mutableStateOf(false) }
    var easterTapCount by remember { mutableStateOf(0) }
    var showEasterDialog by remember { mutableStateOf(false) }
    var isEasterFlow by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor),
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

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(horizontal = NnoworkSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(NnoworkSpacing.md))
            HeaderSection(state, theme)
            Spacer(Modifier.height(NnoworkSpacing.md))
            StampCardSection(
                state = state,
                theme = theme,
                modifier = Modifier.weight(1f),
                onEasterTap = {
                    if (state.hasStampedToday && !state.isCurrentCardComplete) {
                        easterTapCount++
                        if (easterTapCount >= 10) {
                            easterTapCount = 0
                            showEasterDialog = true
                        }
                    }
                },
            )
            Spacer(Modifier.height(NnoworkSpacing.md))
            BottomButton(
                state = state,
                theme = theme,
                onStamp = { showStampDialog = true },
                onNext = {
                    if (state.settings.currentRound >= 3) showMasterOverlay = true else showCompletionDialog = true
                },
            )
            Spacer(Modifier.height(NnoworkSpacing.lg))
        }

        AnimatedVisibility(visible = showAnimation) {
            StampSuccessOverlay(theme)
        }

        if (showMasterOverlay) {
            MasterEasterEggOverlay(
                round = state.settings.currentRound,
                onOpen = {
                    showMasterOverlay = false
                    onMasterCompleted()
                },
            )
        }
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
        TreeHoleShareDialog(
            content = stamp.shareContent,
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("😤 看起來你很生氣", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.highlight)) },
            text = {
                Text(
                    "戳這麼多下…要再多蓋一個章嗎？\n（這次不算每日上限，但也不會多給金幣）",
                    style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.textOnDark),
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

@Composable
private fun CoinHud(
    coins: Int,
    theme: CardTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .pixelCard(
                backgroundColor = theme.cardSurfaceColor,
                borderColor = theme.borderColor,
                shape = RoundedCornerShape(50),
                elevation = NnoworkElevation.control,
            )
            .padding(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.tight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.MonetizationOn,
            contentDescription = "離職金幣",
            tint = theme.accentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            coins.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (theme.cardSurfaceColor.luminance() > 0.5f) NnoworkTheme.colors.primaryText else NnoworkTheme.colors.textOnDark,
            ),
        )
    }
}

@Composable
private fun TreeHoleShareDialog(
    content: String,
    hasProfile: Boolean,
    isSharing: Boolean,
    message: String?,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSharing) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("分享到取暖樹洞？", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Park, null, tint = NnoworkTheme.colors.success, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(NnoworkSpacing.xs))
                    Text("審核通過後會公開，並獲得離職金幣。", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
                }
                Text(content, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
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
    var selectedMax by remember { mutableStateOf(10) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(NnoworkLine.emphasis, NnoworkTheme.colors.border, RectangleShape)
                .padding(NnoworkSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎉 集滿一張卡！", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.highlight))
            Spacer(Modifier.height(NnoworkSpacing.md))
            Text("下一張要幾格？", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, color = NnoworkTheme.colors.textOnDark))
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
                Text("開始新的一張", style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
            }
            Spacer(Modifier.height(NnoworkSpacing.xs))
            androidx.compose.material3.TextButton(onClick = onReceipt) {
                Text("查看離職收據", style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.textOnDark))
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
            style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, theme.titleColor),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(NnoworkSpacing.sm))
        Box(
            modifier = Modifier
                .pixelCard(
                    backgroundColor = theme.primaryColor,
                    borderColor = NnoworkTheme.colors.border,
                    shape = RoundedCornerShape(NnoworkRadius.compact),
                    elevation = 2.dp,
                )
                .padding(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.tight),
        ) {
            Text(
                DateUtils.formattedToday(),
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, theme.palette.onPrimary),
            )
        }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pixelCard(
                    backgroundColor = theme.cardSurfaceColor,
                    borderColor = theme.borderColor,
                    shape = RoundedCornerShape(NnoworkRadius.card),
                    elevation = NnoworkElevation.card,
                )
                .padding(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "第 ${state.settings.currentRound} 張卡片",
                style = pixelTextStyle(
                    NnoworkTheme.type.subtitle,
                    FontWeight.Bold,
                    if (theme.cardSurfaceColor.luminance() > 0.5f) NnoworkTheme.colors.primaryText else NnoworkTheme.colors.textOnDark,
                ),
            )
            Spacer(Modifier.weight(1f))
            PixelBadge(backgroundColor = theme.accentColor) {
                Text(
                    "${state.currentRoundStamps} / ${state.settings.maxStamps}",
                    style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, theme.palette.onAccent),
                )
            }
        }

        Spacer(Modifier.height(NnoworkSpacing.md))

        // 卡片佔據剩餘空間，格子依空間自動縮放，整頁不需捲動。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(NnoworkElevation.hero, RoundedCornerShape(NnoworkRadius.hero), clip = false)
                .clip(RoundedCornerShape(NnoworkRadius.hero))
                .background(theme.cardSurfaceColor)
                .border(
                    if (theme == CardTheme.NOTION) 1.dp else 4.dp,
                    theme.borderColor,
                    RoundedCornerShape(NnoworkRadius.hero),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEasterTap,
                )
                .padding(NnoworkSpacing.page),
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

@Composable
private fun StampGrid(state: HomeUiState, theme: CardTheme) {
    val maxStamps = state.settings.maxStamps
    val columns = if (maxStamps == 30) 6 else 5
    val rows = (maxStamps + columns - 1) / columns
    val stamped = state.currentRoundStamps
    val gap = 8.dp

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // 取寬高限制中較小者算出方形格子尺寸，確保整個集點卡完整顯示。
        val cellByWidth = (maxWidth - gap * (columns - 1)) / columns
        val cellByHeight = (maxHeight - gap * (rows - 1)) / rows
        val cell = minOf(cellByWidth, cellByHeight)

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
    val shape = if (theme.cellCornerRadius > 0.dp) RoundedCornerShape(theme.cellCornerRadius) else RectangleShape
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
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, theme.palette.onContent),
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
                tint = NnoworkTheme.colors.textOnDark,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(NnoworkSpacing.xs))
            Text(
                if (isMaster) "大師級社畜" else "集滿啦！",
                style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark),
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
    val bg: Color
    val contentColor: Color
    val text: String
    val icon = when {
        state.isCurrentCardComplete -> Icons.AutoMirrored.Filled.ArrowForward
        state.hasStampedToday -> Icons.Filled.Check
        else -> theme.buttonActionIcon
    }
    when {
        state.isMasterLevel -> { bg = NnoworkTheme.colors.special; contentColor = NnoworkTheme.colors.textOnDark; text = "What's Next" }
        state.isCurrentCardComplete -> { bg = NnoworkTheme.colors.warning; contentColor = NnoworkTheme.colors.primaryText; text = "What's Next" }
        state.hasStampedToday -> { bg = NnoworkTheme.colors.disabledBG; contentColor = NnoworkTheme.colors.primaryText; text = "今天已蓋章" }
        else -> {
            bg = theme.stampActionBackgroundColor
            contentColor = theme.stampActionContentColor
            text = "我要蓋章"
        }
    }
    PixelButton(
        onClick = { if (state.isCurrentCardComplete) onNext() else if (state.canStamp) onStamp() },
        backgroundColor = bg,
        enabled = state.isCurrentCardComplete || state.canStamp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(NnoworkSpacing.sm))
            Text(text, style = MaterialTheme.typography.titleLarge.copy(color = contentColor))
            Spacer(Modifier.width(NnoworkSpacing.sm))
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        }
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
            .background(Color.Black.copy(alpha = 0.3f)),
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

@Composable
private fun MasterEasterEggOverlay(round: Int, onOpen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(NnoworkSpacing.xxl),
        ) {
            Text("🔥🔥🔥", style = pixelTextStyle(NnoworkTheme.type.hero))
            Spacer(Modifier.height(NnoworkSpacing.md))
            Text("是個狠人！", style = pixelTextStyle(NnoworkTheme.type.display, FontWeight.Bold, NnoworkTheme.colors.highlight))
            Spacer(Modifier.height(NnoworkSpacing.sm))
            Text("你已經集滿第 $round 張卡了", style = pixelTextStyle(NnoworkTheme.type.subtitle, color = NnoworkTheme.colors.textOnDark))
            Spacer(Modifier.height(NnoworkSpacing.lg))
            PixelButton(onClick = onOpen, backgroundColor = NnoworkTheme.colors.special) {
                Text("打開驚喜", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
            }
        }
    }
}
