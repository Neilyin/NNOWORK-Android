package com.neilyin.nnowork.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import com.neilyin.nnowork.util.DateUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun HomeScreen(
    onCardCompleted: () -> Unit = {},
    onMasterCompleted: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val theme = state.theme

    var showStampDialog by remember { mutableStateOf(false) }
    var showAnimation by remember { mutableStateOf(false) }
    var showMasterOverlay by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor),
    ) {
        // 金幣 HUD（右上角）
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🪙", style = pixelTextStyle(14.sp))
            Spacer(Modifier.width(4.dp))
            Text(
                "${state.settings.coinCount}",
                style = pixelTextStyle(14.sp, FontWeight.Bold, theme.titleColor),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            HeaderSection(state, theme)
            Spacer(Modifier.weight(1f))
            StampCardSection(state, theme)
            Spacer(Modifier.weight(1f))
            BottomButton(
                state = state,
                theme = theme,
                onStamp = { showStampDialog = true },
                onNext = {
                    if (state.settings.currentRound >= 3) showMasterOverlay = true else showCompletionDialog = true
                },
            )
            Spacer(Modifier.height(40.dp))
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
            onDismiss = { showStampDialog = false },
            onConfirm = { reason, addToCommon ->
                showStampDialog = false
                viewModel.addStamp(reason, addToCommon) { justCompleted ->
                    showAnimation = true
                    if (justCompleted) {
                        if (state.settings.currentRound >= 3) showMasterOverlay = true else showCompletionDialog = true
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

    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            delay(1000)
            showAnimation = false
        }
    }
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
                .background(PixelColors.darkGray)
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
                .background(theme.primaryColor)
                .border(2.dp, PixelColors.black, RectangleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                DateUtils.formattedToday(),
                style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white),
            )
        }
    }
}

@Composable
private fun StampCardSection(state: HomeUiState, theme: CardTheme) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.cardSurfaceColor)
                .border(3.dp, theme.borderColor, RectangleShape)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "第 ${state.settings.currentRound} 張卡片",
                style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.black),
            )
            Spacer(Modifier.weight(1f))
            PixelBadge(backgroundColor = theme.accentColor) {
                Text(
                    "${state.currentRoundStamps} / ${state.settings.maxStamps}",
                    style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.black),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.cardSurfaceColor)
                .border(
                    if (theme == CardTheme.NOTION) 1.dp else 4.dp,
                    theme.borderColor,
                    RoundedCornerShape(20.dp),
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            StampGrid(state, theme)
        }

        Spacer(Modifier.height(20.dp))

        if (state.isCurrentCardComplete) {
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

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (r in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (c in 0 until columns) {
                    val index = r * columns + c
                    if (index < maxStamps) {
                        StampCell(
                            index = index,
                            isStamped = index < stamped,
                            theme = theme,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
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
    val rotation = remember(index) { Random.nextInt(-15, 16).toFloat() }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(fill)
            .border(2.dp, theme.borderColor.copy(alpha = borderOpacity), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (isStamped) {
            Icon(
                theme.stampIcon,
                contentDescription = null,
                tint = theme.stampIconColor,
                modifier = Modifier
                    .fillMaxSize(0.55f)
                    .rotate(rotation),
            )
        } else {
            Text(
                "${index + 1}",
                style = pixelTextStyle(14.sp, FontWeight.Bold, theme.borderColor.copy(alpha = 0.7f)),
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
    val text: String
    val icon = when {
        state.isCurrentCardComplete -> Icons.Filled.ArrowForward
        state.hasStampedToday -> Icons.Filled.Check
        else -> theme.buttonActionIcon
    }
    when {
        state.isMasterLevel -> { bg = PixelColors.purple; text = "What's Next" }
        state.isCurrentCardComplete -> { bg = PixelColors.orange; text = "What's Next" }
        state.hasStampedToday -> { bg = PixelColors.gray; text = "今天已蓋章" }
        else -> { bg = PixelColors.blue; text = "我要蓋章" }
    }
    PixelButton(
        onClick = { if (state.isCurrentCardComplete) onNext() else if (state.canStamp) onStamp() },
        backgroundColor = bg,
        enabled = state.isCurrentCardComplete || state.canStamp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = PixelColors.white, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, style = pixelTextStyle(22.sp, FontWeight.Bold, PixelColors.white))
            Spacer(Modifier.width(12.dp))
            Icon(icon, contentDescription = null, tint = PixelColors.white, modifier = Modifier.size(24.dp))
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
                Icon(theme.stampIcon, contentDescription = null, tint = theme.primaryColor, modifier = Modifier.size(50.dp))
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
