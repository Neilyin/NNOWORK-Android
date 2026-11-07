package com.neilyin.nnowork.feature.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.PixelBadge
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
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
                .padding(top = 8.dp, end = 16.dp),
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
            Spacer(Modifier.height(16.dp))
            HeaderSection(state, theme)
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(16.dp))
            BottomButton(
                state = state,
                theme = theme,
                onStamp = { showStampDialog = true },
                onNext = {
                    if (state.settings.currentRound >= 3) showMasterOverlay = true else showCompletionDialog = true
                },
            )
            Spacer(Modifier.height(24.dp))
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
            title = { Text("😤 看起來你很生氣", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.yellow)) },
            text = {
                Text(
                    "戳這麼多下…要再多蓋一個章嗎？\n（這次不算每日上限，但也不會多給金幣）",
                    style = pixelTextStyle(14.sp, color = PixelColors.white),
                )
            },
            confirmButton = {
                PixelButton(
                    onClick = {
                        showEasterDialog = false
                        isEasterFlow = true
                        showStampDialog = true
                    },
                    backgroundColor = PixelColors.orange,
                ) {
                    Text("再蓋一個！", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
                }
            },
            dismissButton = {
                PixelButton(onClick = { showEasterDialog = false }, backgroundColor = PixelColors.gray) {
                    Text("算了", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
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
                color = if (theme.cardSurfaceColor.luminance() > 0.5f) PixelColors.black else PixelColors.white,
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
        title = { Text("分享到取暖樹洞？", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Park, null, tint = PixelColors.green, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("審核通過後會公開，並獲得離職金幣。", style = pixelTextStyle(13.sp, color = PixelColors.gray))
                }
                Text(content, style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.white))
                if (!hasProfile) {
                    Text("你還沒建立樹洞身份，先到「樹洞」分頁設定暱稱、產業與職稱。", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.yellow))
                }
                message?.let {
                    Text(it, style = pixelTextStyle(13.sp, FontWeight.Bold, if (it.startsWith("已")) PixelColors.green else PixelColors.yellow))
                }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = onShare,
                enabled = hasProfile && !isSharing,
                backgroundColor = if (hasProfile) PixelColors.green else PixelColors.gray,
            ) {
                Text(
                    if (isSharing) "分享中…" else "分享",
                    style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black),
                )
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, enabled = !isSharing, backgroundColor = PixelColors.gray) {
                Text("略過", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
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
                .border(3.dp, PixelColors.black, RectangleShape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎉 集滿一張卡！", style = pixelTextStyle(22.sp, FontWeight.Bold, PixelColors.yellow))
            Spacer(Modifier.height(16.dp))
            Text("下一張要幾格？", style = pixelTextStyle(16.sp, color = PixelColors.white))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 20, 30).forEach { n ->
                    PixelButton(
                        onClick = { selectedMax = n },
                        backgroundColor = if (selectedMax == n) PixelColors.green else PixelColors.gray,
                    ) {
                        Text("$n", style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.black))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            PixelButton(onClick = { onNewCard(selectedMax) }, backgroundColor = PixelColors.blue, modifier = Modifier.fillMaxWidth()) {
                Text("開始新的一張", style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.white))
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.TextButton(onClick = onReceipt) {
                Text("查看離職收據", style = pixelTextStyle(14.sp, color = PixelColors.white))
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
            style = pixelTextStyle(24.sp, FontWeight.Bold, theme.titleColor),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .pixelCard(
                    backgroundColor = theme.primaryColor,
                    borderColor = PixelColors.black,
                    shape = RoundedCornerShape(NnoworkRadius.compact),
                    elevation = 2.dp,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                DateUtils.formattedToday(),
                style = pixelTextStyle(14.sp, FontWeight.Bold, theme.palette.onPrimary),
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "第 ${state.settings.currentRound} 張卡片",
                style = pixelTextStyle(
                    18.sp,
                    FontWeight.Bold,
                    if (theme.cardSurfaceColor.luminance() > 0.5f) PixelColors.black else PixelColors.white,
                ),
            )
            Spacer(Modifier.weight(1f))
            PixelBadge(backgroundColor = theme.accentColor) {
                Text(
                    "${state.currentRoundStamps} / ${state.settings.maxStamps}",
                    style = pixelTextStyle(20.sp, FontWeight.Bold, theme.palette.onAccent),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 卡片佔據剩餘空間，格子依空間自動縮放，整頁不需捲動。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(NnoworkElevation.hero, RoundedCornerShape(NnoworkRadius.hero), clip = false)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.cardSurfaceColor)
                .border(
                    if (theme == CardTheme.NOTION) 1.dp else 4.dp,
                    theme.borderColor,
                    RoundedCornerShape(20.dp),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEasterTap,
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            StampGrid(state, theme)
        }

        if (state.isCurrentCardComplete) {
            Spacer(Modifier.height(12.dp))
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
            .border(2.dp, theme.borderColor.copy(alpha = borderOpacity), shape),
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
                style = pixelTextStyle(14.sp, FontWeight.Bold, theme.palette.onContent),
            )
        }
    }
}

@Composable
private fun CompletionBanner(isMaster: Boolean) {
    PixelBadge(backgroundColor = if (isMaster) PixelColors.purple else PixelColors.orange) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isMaster) Icons.Filled.WorkspacePremium else Icons.Filled.Star,
                contentDescription = null,
                tint = PixelColors.white,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (isMaster) "大師級社畜" else "集滿啦！",
                style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white),
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
        state.isMasterLevel -> { bg = PixelColors.purple; contentColor = PixelColors.white; text = "What's Next" }
        state.isCurrentCardComplete -> { bg = PixelColors.orange; contentColor = PixelColors.black; text = "What's Next" }
        state.hasStampedToday -> { bg = PixelColors.gray; contentColor = PixelColors.black; text = "今天已蓋章" }
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
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.titleLarge.copy(color = contentColor))
            Spacer(Modifier.width(12.dp))
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
                .background(PixelColors.white)
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
                Text("蓋章成功", style = pixelTextStyle(16.sp, FontWeight.Bold, theme.primaryColor))
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
            modifier = Modifier.padding(40.dp),
        ) {
            Text("🔥🔥🔥", style = pixelTextStyle(48.sp))
            Spacer(Modifier.height(16.dp))
            Text("是個狠人！", style = pixelTextStyle(36.sp, FontWeight.Bold, PixelColors.yellow))
            Spacer(Modifier.height(12.dp))
            Text("你已經集滿第 $round 張卡了", style = pixelTextStyle(18.sp, color = PixelColors.white))
            Spacer(Modifier.height(24.dp))
            PixelButton(onClick = onOpen, backgroundColor = PixelColors.purple) {
                Text("打開驚喜", style = pixelTextStyle(22.sp, FontWeight.Bold, PixelColors.white))
            }
        }
    }
}
