package com.neilyin.nnowork.feature.history

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.R
import com.neilyin.nnowork.ui.adaptive.NnoworkAdaptive
import com.neilyin.nnowork.ui.adaptive.readableWidth
import com.neilyin.nnowork.ui.theme.NnoworkElevation
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.delay

/**
 * 離職成功的「破關畫面」（iOS FreedomView V3.6 RPG 化）：
 * 「任務完成！」標題 → 慶祝角色 → 打字機對話框（點一下跳過）→ 冒險成果 → 選單（▶ 游標）。
 * iOS 的夜空／像素牛馬／深藍 RPG 視窗改成主題 token 的卡片，淺色主題不出現深色視窗，Night 主題自然是深色。
 */
@Composable
fun FreedomScreen(
    stats: FreedomStats,
    onViewHistory: () -> Unit,
    onStartNewCompany: () -> Unit,
) {
    BackHandler(onBack = onViewHistory)
    // 進場快照：換工作完成後公司名與章數會被清掉，關閉前那一瞬間不能跟著變（iOS snapshot）。
    val snapshot = remember { stats }
    val reduceMotion = rememberReduceMotion()
    val company = snapshot.companyName.ifBlank { stringResource(R.string.master_company_fallback) }
    val dialogText = freedomDialogText(
        congrats = stringResource(R.string.freedom_line_congrats),
        free = stringResource(R.string.freedom_line_free),
        bye = stringResource(R.string.freedom_line_bye),
        companyLine = stringResource(R.string.freedom_line_company, company),
        wish1 = stringResource(R.string.freedom_line_wish1),
        wish2 = stringResource(R.string.freedom_line_wish2),
    )

    // 0 標題／1 對話框／2 成果／3 選單；旋轉後不重播。
    var stage by rememberSaveable { mutableIntStateOf(if (reduceMotion) 3 else 0) }
    var typed by rememberSaveable { mutableIntStateOf(if (reduceMotion) Int.MAX_VALUE else 0) }
    val finishedTyping = typed >= dialogText.length
    val scrollState = rememberScrollState()
    val titleState = remember { MutableTransitionState(reduceMotion).apply { targetState = true } }

    LaunchedEffect(Unit) {
        if (stage >= 3) return@LaunchedEffect
        if (stage < 1) {
            delay(600)
            stage = 1
            delay(250)
        }
        while (typed < dialogText.length) {
            typed++
            delay(38)
        }
        delay(250)
        stage = maxOf(stage, 2)
        delay(350)
        stage = 3
    }
    // 小螢幕：選單出現時捲到底，使用者才看得到下一步。
    LaunchedEffect(stage) {
        if (stage == 3) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NnoworkTheme.colors.appBG, NnoworkTheme.colors.appBGBottom)))
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .readableWidth(NnoworkAdaptive.formMaxWidth)
                .padding(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
        ) {
            AnimatedVisibility(visibleState = titleState, enter = if (reduceMotion) fadeIn() else scaleIn(initialScale = 1.8f) + fadeIn()) {
                Text(
                    stringResource(R.string.freedom_title),
                    style = pixelTextStyle(NnoworkTheme.type.display, FontWeight.Black, NnoworkTheme.colors.highlight),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier
                        .padding(top = NnoworkSpacing.page)
                        .semantics { heading() },
                )
            }

            HeroStage(animated = !reduceMotion)

            AnimatedVisibility(visible = stage >= 1, enter = windowEnter(reduceMotion)) {
                QuestDialogWindow(
                    fullText = dialogText,
                    typedCount = typed.coerceAtMost(dialogText.length),
                    finished = finishedTyping,
                    animated = !reduceMotion,
                    onSkip = { typed = dialogText.length },
                )
            }

            AnimatedVisibility(visible = stage >= 2, enter = windowEnter(reduceMotion)) {
                QuestWindow(title = stringResource(R.string.freedom_results_title)) {
                    ResultRow(Icons.Filled.GridView, NnoworkTheme.colors.highlight, stringResource(R.string.freedom_cards), snapshot.cards.toString())
                    ResultRow(Icons.Filled.Verified, NnoworkTheme.colors.success, stringResource(R.string.freedom_stamps), snapshot.stamps.toString())
                    ResultRow(
                        Icons.Filled.CalendarMonth,
                        NnoworkTheme.colors.info,
                        stringResource(R.string.freedom_days),
                        stringResource(R.string.master_value_days, snapshot.days),
                    )
                }
            }

            AnimatedVisibility(visible = stage >= 3, enter = windowEnter(reduceMotion)) {
                QuestWindow(spacing = NnoworkSpacing.xxs) {
                    QuestMenuOption(
                        icon = Icons.Filled.Sync,
                        title = stringResource(R.string.freedom_restart),
                        isPrimary = true,
                        animated = !reduceMotion,
                        onClick = onStartNewCompany,
                    )
                    QuestMenuOption(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = stringResource(R.string.freedom_history),
                        isPrimary = false,
                        animated = !reduceMotion,
                        onClick = onViewHistory,
                    )
                }
            }
            Spacer(Modifier.size(NnoworkSpacing.xl))
        }
    }
}

private fun windowEnter(reduceMotion: Boolean) =
    if (reduceMotion) fadeIn() else slideInVertically { it / 3 } + fadeIn()

/** 系統「移除動畫」（動畫時長縮放 0）時視同 iOS Reduce Motion：不打字、不跳動，直接全部顯示。 */
@Composable
private fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }
}

/** 舞台：慶祝圖示上下跳，兩側星星閃爍（iOS HoppingCow／PixelSparkle 的簡化版）。 */
@Composable
private fun HeroStage(animated: Boolean) {
    val transition = rememberInfiniteTransition(label = "freedomHero")
    val hop by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "hop",
    )
    val sparkleA by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 0.25f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "sparkleA",
    )
    val sparkleB by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 0.25f else 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse, StartOffset(600)),
        label = "sparkleB",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xl, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = NnoworkTheme.colors.highlight,
            modifier = Modifier.padding(bottom = 64.dp).size(NnoworkSize.icon).alpha(sparkleA),
        )
        Box(
            modifier = Modifier
                .offset(y = (-18).dp * hop)
                .size(88.dp)
                .clip(CircleShape)
                .background(NnoworkTheme.colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Celebration, contentDescription = null, tint = NnoworkTheme.colors.accentForeground, modifier = Modifier.size(48.dp))
        }
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = NnoworkTheme.colors.highlight,
            modifier = Modifier.padding(bottom = 76.dp).size(NnoworkSize.icon).alpha(sparkleB),
        )
    }
}

/** RPG 視窗：主題卡片（elevatedSurface＋hairline），標題用強調色小字。 */
@Composable
private fun QuestWindow(
    title: String? = null,
    spacing: androidx.compose.ui.unit.Dp = NnoworkSpacing.sm,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = NnoworkTheme.colors.elevatedSurface, elevation = NnoworkElevation.card)
            .padding(NnoworkSpacing.roomy),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        if (title != null) {
            Text(
                title,
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.ExtraBold, NnoworkTheme.colors.accentForeground)
                    .copy(letterSpacing = 1.sp),
                modifier = Modifier.semantics { heading() },
            )
        }
        content()
    }
}

/** 打字機對話框：未打出的字用透明色排好，換行與高度從一開始就固定；點一下直接顯示全文。 */
@Composable
private fun QuestDialogWindow(
    fullText: String,
    typedCount: Int,
    finished: Boolean,
    animated: Boolean,
    onSkip: () -> Unit,
) {
    val textColor = NnoworkTheme.colors.primaryText
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = textColor)) { append(fullText.take(typedCount)) }
        withStyle(SpanStyle(color = Color.Transparent)) { append(fullText.drop(typedCount)) }
    }
    QuestWindow(
        modifier = Modifier
            .clip(RoundedCornerShape(NnoworkRadius.card))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSkip)
            .clearAndSetSemantics { contentDescription = fullText },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                annotated,
                style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.SemiBold, textColor),
                modifier = Modifier.fillMaxWidth(),
            )
            if (finished) {
                BlinkingCursor(
                    symbol = "▼",
                    animated = animated,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

@Composable
private fun ResultRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(NnoworkSize.compactIcon))
        Spacer(Modifier.width(NnoworkSpacing.snug))
        Text(
            label,
            style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Medium, NnoworkTheme.colors.secondaryText),
            modifier = Modifier.weight(1f),
        )
        Text(value, style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true))
    }
}

/** 選單選項：主選項前有閃爍的 ▶ 游標、用強調色；次選項一般文字色。 */
@Composable
private fun QuestMenuOption(
    icon: ImageVector,
    title: String,
    isPrimary: Boolean,
    animated: Boolean,
    onClick: () -> Unit,
) {
    val color = if (isPrimary) NnoworkTheme.colors.accentForeground else NnoworkTheme.colors.primaryText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = NnoworkSize.minTouchTarget)
            .clip(RoundedCornerShape(NnoworkRadius.small))
            .clickable(onClick = onClick)
            .padding(horizontal = NnoworkSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(NnoworkSpacing.cozy)) {
            if (isPrimary) BlinkingCursor(symbol = "▶", animated = animated)
        }
        Spacer(Modifier.width(NnoworkSpacing.snug))
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(NnoworkSize.compactIcon))
        Spacer(Modifier.width(NnoworkSpacing.snug))
        Text(
            title,
            style = pixelTextStyle(NnoworkTheme.type.bodyLarge, if (isPrimary) FontWeight.Bold else FontWeight.SemiBold, color),
            maxLines = 2,
        )
    }
}

@Composable
private fun BlinkingCursor(symbol: String, animated: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 0.15f else 1f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    Text(
        symbol,
        style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.accentForeground),
        modifier = modifier.alpha(alpha).clearAndSetSemantics {},
    )
}
