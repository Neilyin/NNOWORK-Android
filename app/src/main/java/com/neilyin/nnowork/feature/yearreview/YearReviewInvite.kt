package com.neilyin.nnowork.feature.yearreview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neilyin.nnowork.R
import com.neilyin.nnowork.ui.adaptive.LocalNnoworkWindowInfo
import com.neilyin.nnowork.ui.common.FullScreenDialog
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 一次性邀請卡的宿主（§22.12）：放在主畫面（已過 onboarding／更新說明／帳號綁定引導）上。
 *
 * - App 在前景時才等：先等 2 秒，之後每 0.8 秒檢查，連續兩次「沒有其他畫面」才顯示。
 *   「沒有其他畫面」用主視窗是否有焦點判斷：任何 Dialog、bottom sheet、評分視窗、全螢幕畫面都會拿走焦點。
 * - 卡片出現當下就記看過；回背景時等待取消、不記看過。
 * - DEBUG「立即顯示邀請卡」：略過「看過」與「有章」，首次只等 0.6 秒。
 */
@Composable
fun YearReviewInviteHost(cardTheme: CardTheme) {
    val view = LocalView.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val resumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val flags by YearReviewPrefs.flags.collectAsStateWithLifecycle()
    var presentedYear by remember { mutableStateOf<Int?>(null) }
    var forceRequests by remember { mutableIntStateOf(0) }
    var handledForce by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        YearReviewPrefs.showInviteNowRequests.collect { forceRequests++ }
    }
    LaunchedEffect(resumed, forceRequests, presentedYear, flags.simulateInviteWindow) {
        if (!resumed || presentedYear != null) return@LaunchedEffect
        val forced = forceRequests > handledForce
        val today = YearReviewWindow.today()
        val year = if (forced) {
            YearReviewWindow.forcedYear(today, flags.simulateInviteWindow)
        } else {
            YearReviewWindow.inviteWindowYear(today, flags.simulateInviteWindow) ?: return@LaunchedEffect
        }
        suspend fun eligible() = forced ||
            YearReviewInvitePolicy.shouldInvite(year, YearReviewPrefs.hasShownInvite(year), YearReviewSource.hasStamps(year))
        if (!eligible()) return@LaunchedEffect
        delay(if (forced) YearReviewInvitePolicy.FORCED_INITIAL_DELAY_MS else YearReviewInvitePolicy.INITIAL_DELAY_MS)
        val checks = mutableListOf<Boolean>()
        while (YearReviewInvitePolicy.firstShowableCheck(checks) == null) {
            if (checks.isNotEmpty()) delay(YearReviewInvitePolicy.POLL_INTERVAL_MS)
            checks += view.hasWindowFocus()
        }
        // 顯示前再確認一次「沒看過」與「有章」
        if (!eligible()) return@LaunchedEffect
        if (forced) handledForce = forceRequests
        YearReviewPrefs.markInviteShown(year)
        presentedYear = year
    }

    presentedYear?.let { year ->
        YearReviewInviteCover(year = year, cardTheme = cardTheme, onClose = { presentedYear = null })
    }
}

/** 邀請卡的全螢幕容器：按「立即觀看」就在同一個容器裡淡入回顧（0.25 秒）。 */
@Composable
private fun YearReviewInviteCover(year: Int, cardTheme: CardTheme, onClose: () -> Unit) {
    val reduceMotion = rememberYearReviewReduceMotion()
    var watching by remember { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val player = remember { YearReviewPlayer() }
    LaunchedEffect(Unit) { appeared = true }
    val later: () -> Unit = {
        if (appeared) {
            appeared = false
            scope.launch {
                delay(200)
                onClose()
            }
        }
    }
    val page = player.currentPage
    val light = watching && !page.isNight && NnoworkTheme.colors.appBG.luminance() > 0.5f
    FullScreenDialog(
        onDismissRequest = { if (watching) onClose() else later() },
        background = SolidColor(Color.Transparent),
        lightSystemBars = light,
        applySystemBarInsets = false,
    ) {
        AnimatedContent(
            targetState = watching,
            transitionSpec = { fadeIn(tween(if (reduceMotion) 0 else 250)) togetherWith fadeOut(tween(if (reduceMotion) 0 else 250)) },
            label = "yrInvite",
        ) { isWatching ->
            if (isWatching) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    YearReviewStory(year, cardTheme, player, onClose)
                }
            } else {
                YearReviewInviteCard(year = year, appeared = appeared, reduceMotion = reduceMotion, onWatch = { watching = true }, onLater = later)
            }
        }
    }
}

@Composable
private fun YearReviewInviteCard(year: Int, appeared: Boolean, reduceMotion: Boolean, onWatch: () -> Unit, onLater: () -> Unit) {
    val colors = NnoworkTheme.colors
    val fold = LocalNnoworkWindowInfo.current.fold
    // 摺疊機半折、直向摺痕左緣 ≥ 300dp：卡片移到摺痕左半邊
    val foldLeft = fold.start.takeIf { fold.hasVerticalFold && fold.isSeparating && fold.start >= 300.dp }
    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(if (appeared) (if (reduceMotion) 200 else 300) else 180),
        label = "yrInviteCard",
    )
    val laterHint = stringResource(R.string.yr_invite_later_hint)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress }.background(Color.Black.copy(alpha = 0.45f)).clearAndSetSemantics { })
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .then(if (foldLeft != null) Modifier.width(foldLeft) else Modifier.weight(1f))
                    .fillMaxSize()
                    .padding(horizontal = NnoworkSpacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                val shape = RoundedCornerShape(NnoworkRadius.card)
                Column(
                    Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = progress
                            val k = if (reduceMotion) 1f else 0.94f + 0.06f * progress
                            scaleX = k
                            scaleY = k
                        }
                        .background(colors.elevatedSurface, shape)
                        .border(NnoworkLine.control, colors.divider, shape)
                        .padding(NnoworkSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
                ) {
                    Text("🎞️", fontSize = 40.sp, modifier = Modifier.clearAndSetSemantics { })
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                        Text(
                            stringResource(R.string.yr_invite_title, year),
                            style = pixelTextStyle(19.sp, FontWeight.Bold, colors.primaryText),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            stringResource(R.string.yr_invite_subtitle),
                            style = pixelTextStyle(13.sp, color = colors.primaryText.copy(alpha = 0.82f)),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
                        PixelButton(onClick = onWatch, backgroundColor = colors.buttonBG, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.yr_invite_watch), style = pixelTextStyle(16.sp, FontWeight.Bold, colors.buttonText))
                        }
                        Text(
                            stringResource(R.string.yr_invite_later),
                            style = pixelTextStyle(15.sp, FontWeight.Bold, colors.primaryText),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = laterHint, onClick = onLater)
                                .padding(vertical = NnoworkSpacing.sm),
                        )
                    }
                }
            }
            if (foldLeft != null) Spacer(Modifier.weight(1f))
        }
    }
}
