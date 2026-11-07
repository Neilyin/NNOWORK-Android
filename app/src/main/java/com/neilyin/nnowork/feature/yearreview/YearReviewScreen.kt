package com.neilyin.nnowork.feature.yearreview

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neilyin.nnowork.R
import com.neilyin.nnowork.feature.history.ReceiptSharer
import com.neilyin.nnowork.feature.home.MasterSummaryExporter
import com.neilyin.nnowork.ui.common.FullScreenDialog
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot

/** 頁碼與計時（§22.7）。 */
class YearReviewPlayer {
    var pages by mutableStateOf(listOf(YearReviewPage.OPENING))
        private set
    var pageIndex by mutableIntStateOf(0)
        private set
    var elapsed by mutableFloatStateOf(0f)
        private set
    /** 每次換頁（含重播同一頁）都換一個值，讓頁面重建、動畫重跑。 */
    var playToken by mutableIntStateOf(0)
        private set
    /** 開場播完、統計還在背景算（清單只有開場）：底部改顯示「離職紀錄載入中」。 */
    var isWaitingForData by mutableStateOf(false)
        private set
    /** 手指按著、分享預覽、App 不在前景、TalkBack、樹洞頁載入中時暫停計時（由畫面同步）。 */
    var isBlocked = false

    val currentPage: YearReviewPage get() = pages[pageIndex.coerceIn(0, pages.lastIndex)]

    /** 約 30fps 累加；每 tick 最多算 0.1 秒。 */
    suspend fun run() {
        var last = System.nanoTime()
        while (true) {
            delay(33)
            val now = System.nanoTime()
            val delta = ((now - last) / 1e9f).coerceAtMost(0.1f)
            last = now
            if (isBlocked) continue
            val duration = currentPage.durationSeconds
            if (elapsed < duration) elapsed = minOf(duration, elapsed + delta)
            // 最後一頁播完停住（讓使用者分享），不自動關閉
            if (elapsed >= duration && pageIndex < pages.lastIndex) go(pageIndex + 1)
            val waiting = pages == listOf(YearReviewPage.OPENING) && elapsed >= duration
            if (waiting != isWaitingForData) isWaitingForData = waiting
        }
    }

    /** 目前這頁還在就留在原地不重播；被拿掉了就接著播同位置的下一頁。 */
    fun updatePages(newPages: List<YearReviewPage>) {
        if (newPages.isEmpty() || newPages == pages) return
        val current = currentPage
        pages = newPages
        isWaitingForData = false
        val index = newPages.indexOf(current)
        if (index >= 0) pageIndex = index else go(minOf(pageIndex, newPages.lastIndex))
    }

    /** 下一頁；已是最後一頁回傳 false。 */
    fun next(): Boolean {
        if (pageIndex >= pages.lastIndex) return false
        go(pageIndex + 1)
        return true
    }

    /** 上一頁；第一頁則從頭重播。 */
    fun previous() = go(maxOf(0, pageIndex - 1))

    private fun go(index: Int) {
        elapsed = 0f
        isWaitingForData = false
        pageIndex = index
        playToken++
    }
}

/** 進度條、關閉鈕、分享鈕的配色跟著頁面背景。 */
private data class YearReviewChrome(val foreground: Color, val buttonBackground: Color, val buttonForeground: Color)

@Composable
private fun chromeFor(page: YearReviewPage): YearReviewChrome = if (page.isNight) {
    YearReviewChrome(YearReviewNight.text, YearReviewNight.gold, YearReviewNight.onGold)
} else {
    val colors = NnoworkTheme.colors
    YearReviewChrome(colors.primaryText, colors.buttonBG, colors.buttonText)
}

/** 全螢幕年度回顧（設定頁入口、邀請卡「立即觀看」共用）。 */
@Composable
fun YearReviewDialog(year: Int, cardTheme: CardTheme, onDismiss: () -> Unit) {
    val player = remember { YearReviewPlayer() }
    val page = player.currentPage
    val light = !page.isNight && NnoworkTheme.colors.appBG.luminance() > 0.5f
    FullScreenDialog(
        onDismissRequest = onDismiss,
        background = SolidColor(Color.Black),
        lightSystemBars = light,
        applySystemBarInsets = false,
    ) {
        YearReviewStory(year, cardTheme, player, onDismiss)
    }
}

/** 故事本體（也給邀請卡在同一個容器裡淡入使用）。 */
@Composable
fun YearReviewStory(year: Int, cardTheme: CardTheme, player: YearReviewPlayer, onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val reduceMotion = rememberYearReviewReduceMotion()
    val animated = !reduceMotion
    val talkBack = rememberTalkBackEnabled()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val resumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val flags by YearReviewPrefs.flags.collectAsStateWithLifecycle()

    var placeholder by remember { mutableStateOf(YearReviewData.placeholder(year, null)) }
    var data by remember { mutableStateOf<YearReviewData?>(null) }
    var sharingPage by remember { mutableStateOf<YearReviewPage?>(null) }
    var touching by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        YearReviewSource.nickname()?.let { if (data == null) placeholder = YearReviewData.placeholder(year, it) }
    }
    // 統計在背景算（原因斷詞較花時間），開場頁先播；算好後接著抓樹洞資料，不擋其他頁的播放
    LaunchedEffect(Unit) {
        val demo = flags.demoData
        val inputs = if (demo) YearReviewDemo.inputs(context, year) else YearReviewSource.inputs(context, year)
        val built = withContext(Dispatchers.Default) { YearReviewData.build(inputs) }
        if (flags.loadDelay) delay(5_000)
        if (demo) {
            data = built.withTreeHole(YearReviewDemo.treeHole)
            return@LaunchedEffect
        }
        data = built
        if (built.treeHoleStatus == YearReviewTreeHoleStatus.LOADING) {
            val stats = YearReviewTreeHoleLoader.load(year)
            data = data?.withTreeHole(stats)
        }
    }
    val pages = data?.pages
    LaunchedEffect(pages) {
        val latest = pages ?: return@LaunchedEffect
        if (player.isWaitingForData) {
            // 載入面板先補滿 100%（0.3 秒）、停一下，再接到下一頁
            delay(650)
            data?.pages?.let(player::updatePages)
        } else {
            player.updatePages(latest)
        }
    }
    LaunchedEffect(Unit) { player.run() }

    val shownData = data?.takeIf { player.pages.size > 1 } ?: placeholder
    val page = player.currentPage
    val treeHoleLoading = page == YearReviewPage.TREE_HOLE && data?.treeHoleStatus == YearReviewTreeHoleStatus.LOADING
    val blocked = touching || sharingPage != null || !resumed || talkBack || treeHoleLoading
    SideEffect { player.isBlocked = blocked }
    LaunchedEffect(resumed) {
        if (!resumed) holding = false
    }

    val nextLabel = stringResource(R.string.yr_next_page)
    val previousLabel = stringResource(R.string.yr_previous_page)
    val loadingLabel = stringResource(R.string.yr_loading_title)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxHeight.value > 0 && maxWidth.value / maxHeight.value > YearReviewLayout.ASPECT + 0.01f
        val dragged = drag.value
        val dragScale = 1f - (dragged.coerceAtMost(400f) / 400f) * 0.1f
        // 寬螢幕兩側：同一頁背景（靜態）壓暗 50%
        if (wide) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (dragged > 0f) 0f else 1f }) {
                YearReviewPageBackground(page, animated = false, paused = true)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragged * density
                    scaleX = dragScale
                    scaleY = dragScale
                },
        ) {
            if (!wide) {
                Crossfade(page, animationSpec = tween(300), label = "yrBackground") { pg ->
                    YearReviewPageBackground(pg, animated = animated, paused = blocked)
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) {
                val storyWidth = if (wide) maxHeight * YearReviewLayout.ASPECT else maxWidth
                val storyHeight = maxHeight
                val s = storyWidth.value / YearReviewLayout.BASE_WIDTH
                val cardShape = RoundedCornerShape(NnoworkRadius.hero)
                Box(
                    Modifier
                        .size(storyWidth, storyHeight)
                        .then(if (wide) Modifier.shadow(24.dp, cardShape).clip(cardShape) else Modifier),
                ) {
                    if (wide) {
                        Crossfade(page, animationSpec = tween(300), label = "yrCardBackground") { pg ->
                            YearReviewPageBackground(pg, animated = animated, paused = blocked)
                        }
                    }
                    val baseDensity = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(baseDensity.density, 1f),
                        LocalYearReviewCardTheme provides cardTheme,
                    ) {
                        Crossfade(player.playToken to page, animationSpec = tween(300), label = "yrPage") { (_, pg) ->
                            YearReviewPageContent(
                                page = pg, data = shownData, s = s, animated = animated, isExport = false, paused = blocked,
                                // TalkBack：整頁讀成一段，用自訂動作換頁（手勢層對無障礙隱藏）
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    customActions = listOf(
                                        CustomAccessibilityAction(nextLabel) {
                                            if (player.pages.size <= 1) {
                                                @Suppress("DEPRECATION")
                                                view.announceForAccessibility(loadingLabel)
                                            } else if (!player.next()) {
                                                onClose()
                                            }
                                            true
                                        },
                                        CustomAccessibilityAction(previousLabel) { player.previous(); true },
                                    )
                                },
                            )
                        }
                    }
                    // 手勢層：點左右換頁、按住暫停、下滑關閉
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clearAndSetSemantics { }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    touching = true
                                    val holdJob: Job = scope.launch {
                                        delay(HOLD_THRESHOLD_MS)
                                        holding = true
                                    }
                                    val tracker = VelocityTracker()
                                    tracker.addPosition(down.uptimeMillis, down.position)
                                    var total = Offset.Zero
                                    var upTime = down.uptimeMillis
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        tracker.addPosition(change.uptimeMillis, change.position)
                                        total = change.position - down.position
                                        upTime = change.uptimeMillis
                                        val dy = total.y / density
                                        val dx = total.x / density
                                        val offset = if (dy > 12f && dy > abs(dx)) dy else 0f
                                        scope.launch { drag.snapTo(offset) }
                                        if (!change.pressed) break
                                    }
                                    touching = false
                                    holdJob.cancel()
                                    holding = false
                                    val dy = total.y / density
                                    val dx = total.x / density
                                    if (dy > 12f && dy > abs(dx)) {
                                        val velocity = tracker.calculateVelocity().y / density
                                        if (dy > 140f || dy + velocity * 0.25f > 400f) {
                                            onClose()
                                        } else {
                                            scope.launch { drag.animateTo(0f, tween(220)) }
                                        }
                                        return@awaitEachGesture
                                    }
                                    scope.launch { drag.snapTo(0f) }
                                    // 長按放開只恢復播放；短點才換頁
                                    if (upTime - down.uptimeMillis >= HOLD_THRESHOLD_MS || hypot(dx, dy) >= 12f) return@awaitEachGesture
                                    if (down.position.x < size.width / 2) {
                                        player.previous()
                                    } else if (!player.next() && player.pages.size > 1) {
                                        // 最後一頁再點右邊＝看完了
                                        onClose()
                                    }
                                }
                            },
                    )
                    val chromeAlpha by animateFloatAsState(if (holding) 0f else 1f, tween(200), label = "yrChrome")
                    ChromeOverlay(
                        year = year,
                        player = player,
                        page = page,
                        dataReady = data != null,
                        canShare = !treeHoleLoading,
                        animated = animated,
                        onClose = onClose,
                        onShare = { sharingPage = page },
                        modifier = Modifier.graphicsLayer { alpha = chromeAlpha },
                    )
                }
            }
        }
    }

    sharingPage?.let { sharing ->
        YearReviewShareSheet(page = sharing, data = shownData, cardTheme = cardTheme, onDismiss = { sharingPage = null })
    }
}

private const val HOLD_THRESHOLD_MS = 250L

@Composable
private fun ChromeOverlay(
    year: Int,
    player: YearReviewPlayer,
    page: YearReviewPage,
    dataReady: Boolean,
    canShare: Boolean,
    animated: Boolean,
    onClose: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = chromeFor(page)
    Column(modifier.fillMaxSize()) {
        val positionLabel = stringResource(R.string.yr_page_position, player.pageIndex + 1, player.pages.size)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp)
                .clearAndSetSemantics { contentDescription = positionLabel },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            player.pages.forEachIndexed { index, pg ->
                val fill = when {
                    index < player.pageIndex -> 1f
                    index > player.pageIndex -> 0f
                    else -> (player.elapsed / pg.durationSeconds).coerceIn(0f, 1f)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(chrome.foreground.copy(alpha = 0.3f)),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(chrome.foreground))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.yr_year_in_review, year),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = chrome.foreground.copy(alpha = 0.9f), letterSpacing = 0.5.sp),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.yr_close), tint = chrome.foreground)
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            androidx.compose.animation.AnimatedVisibility(visible = player.isWaitingForData, enter = fadeIn(), exit = fadeOut()) {
                LoadingPanel(isReady = dataReady, animated = animated, modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 4.dp))
            }
            if (!player.isWaitingForData) {
                val isSummary = page == YearReviewPage.SUMMARY
                val breathing = if (isSummary && animated) {
                    val transition = rememberInfiniteTransition(label = "yrBreath")
                    transition.animateFloat(1f, 1.06f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "yrBreathScale").value
                } else {
                    1f
                }
                Row(
                    Modifier
                        .padding(bottom = 18.dp)
                        .graphicsLayer {
                            scaleX = breathing
                            scaleY = breathing
                            alpha = if (canShare) 1f else 0f
                        }
                        .heightIn(min = if (isSummary) 52.dp else 48.dp)
                        .clip(RoundedCornerShape(NnoworkRadius.button))
                        .background(chrome.buttonBackground)
                        .clickable(enabled = canShare, onClick = onShare)
                        .padding(horizontal = if (isSummary) 30.dp else 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.IosShare, null, tint = chrome.buttonForeground, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(if (isSummary) R.string.yr_share_summary else R.string.yr_share_page),
                        style = TextStyle(fontSize = if (isSummary) 17.sp else 15.sp, fontWeight = FontWeight.Bold, color = chrome.buttonForeground),
                    )
                }
            }
        }
    }
}

/** 「離職紀錄載入中」：分階段的假進度（只出現在開場的星空頁，精簡版標題與階段輪流）。 */
@Composable
private fun LoadingPanel(isReady: Boolean, animated: Boolean, modifier: Modifier = Modifier) {
    var progress by remember { mutableFloatStateOf(0f) }
    var stage by remember { mutableIntStateOf(0) }
    var showsTitle by remember { mutableStateOf(true) }
    val shownProgress by animateFloatAsState(progress, if (animated) tween(if (isReady) 300 else 50) else tween(0), label = "yrLoading")
    LaunchedEffect(isReady) {
        if (isReady) {
            progress = 1f
            return@LaunchedEffect
        }
        val start = System.nanoTime()
        while (true) {
            val seconds = (System.nanoTime() - start) / 1e9
            val (value, newStage) = yearReviewFakeProgress(seconds)
            progress = value.toFloat()
            stage = newStage
            showsTitle = (seconds / 1.6).toInt() % 2 == 0
            delay(50)
        }
    }
    val stageText = stringResource(
        when (stage) {
            0 -> R.string.yr_loading_stage_1
            1 -> R.string.yr_loading_stage_2
            else -> R.string.yr_loading_stage_3
        },
    )
    val title = stringResource(R.string.yr_loading_title)
    val shape = RoundedCornerShape(NnoworkRadius.field)
    Column(
        modifier
            .fillMaxWidth()
            .background(YearReviewNight.windowFill, shape)
            .border(2.dp, YearReviewNight.windowBorder.copy(alpha = 0.9f), shape)
            .padding(10.dp)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Crossfade(showsTitle, label = "yrLoadingTitle", modifier = Modifier.weight(1f)) { titleShown ->
                Text(
                    if (titleShown) title else stageText,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = YearReviewNight.title),
                    maxLines = 1,
                )
            }
            Text(
                "${(shownProgress * 100).toInt()}%",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = YearReviewNight.text.copy(alpha = 0.85f)),
            )
        }
        val lit = (shownProgress * 20).toInt()
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(20) { index ->
                Box(Modifier.weight(1f).height(10.dp).background(if (index < lit) YearReviewNight.title else YearReviewNight.text.copy(alpha = 0.15f)))
            }
        }
    }
}

// MARK: - 分享圖

/**
 * 分享出去的 9:16 畫面：該頁動畫結束後的靜態樣子＋上方「%d 年度回顧」、下方 App 圖示與名稱。
 * 不放公司名稱。固定 360×640dp，以 density 3 畫成 1080×1920。
 */
@Composable
fun YearReviewShareCanvas(page: YearReviewPage, data: YearReviewData, cardTheme: CardTheme) {
    val context = LocalContext.current
    val chrome = chromeFor(page)
    val icon = remember(context) {
        runCatching { context.packageManager.getApplicationIcon(context.packageName).toBitmap(78, 78).asImageBitmap() }.getOrNull()
    }
    CompositionLocalProvider(LocalYearReviewCardTheme provides cardTheme) {
        Box(Modifier.size(YearReviewLayout.BASE_WIDTH.dp, YearReviewLayout.BASE_HEIGHT.dp)) {
            YearReviewPageBackground(page, animated = false, paused = true)
            YearReviewPageContent(page, data, s = 1f, animated = false, isExport = true, paused = true)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.yr_year_in_review, data.year),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = chrome.foreground, letterSpacing = 1.sp),
                    modifier = Modifier
                        .padding(top = 22.dp)
                        .border(1.5.dp, chrome.foreground.copy(alpha = 0.45f), RoundedCornerShape(NnoworkRadius.small))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Row(Modifier.padding(bottom = 30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (icon != null) {
                        Image(icon, null, modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)))
                    }
                    Text(
                        stringResource(R.string.yr_app_name),
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = chrome.foreground),
                    )
                }
            }
        }
    }
}

/** 分享前預覽：顯示實際輸出的圖（1080×1920），再交系統分享選單或存到相簿。 */
@Composable
private fun YearReviewShareSheet(page: YearReviewPage, data: YearReviewData, cardTheme: CardTheme, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = NnoworkTheme.colors
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var ready by remember { mutableStateOf(false) }
    val title = stringResource(R.string.yr_year_in_review, data.year)
    val chooserTitle = stringResource(R.string.yr_share)
    val savedMessage = stringResource(R.string.master_saved)
    val failedMessage = stringResource(R.string.master_image_failed)

    suspend fun capture(): Bitmap {
        val image = layer.toImageBitmap().asAndroidBitmap()
        return image.copy(Bitmap.Config.ARGB_8888, false)
    }

    val save: () -> Unit = {
        scope.launch {
            runCatching {
                val bitmap = capture()
                MasterSummaryExporter.saveBitmapToGallery(context, "nnowork_year_review_${System.currentTimeMillis()}.png") { bitmap }
            }
                .onSuccess { Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show() }
        }
    }
    val legacyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save() else Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show()
    }
    val onSave: () -> Unit = {
        val needsLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsLegacyPermission) legacyPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else save()
    }
    val onShare: () -> Unit = {
        scope.launch {
            runCatching {
                val bitmap = capture()
                ReceiptSharer.shareImage(context, clipLabel = title, chooserTitle = chooserTitle) { bitmap }
            }.onFailure { Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show() }
        }
    }
    LaunchedEffect(Unit) {
        // 等一幀讓離屏內容畫進 graphics layer
        delay(120)
        ready = true
    }

    FullScreenDialog(
        onDismissRequest = onDismiss,
        background = SolidColor(colors.appBG),
        lightSystemBars = colors.appBG.luminance() > 0.5f,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = NnoworkSpacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(vertical = NnoworkSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.yr_close),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, colors.primaryText),
                    modifier = Modifier.clip(RoundedCornerShape(NnoworkRadius.field)).clickable(onClick = onDismiss).padding(NnoworkSpacing.sm),
                )
                Text(
                    stringResource(R.string.yr_share_page),
                    style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, colors.primaryText),
                    modifier = Modifier.weight(1f).semantics { heading() },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.width(64.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .aspectRatio(YearReviewLayout.ASPECT)
                        .fillMaxHeight()
                        .shadow(16.dp, RoundedCornerShape(NnoworkRadius.card))
                        .clip(RoundedCornerShape(NnoworkRadius.card))
                        .clearAndSetSemantics { contentDescription = title },
                ) {
                    // 離屏以 density 3 畫 360×640dp ＝ 1080×1920px，再縮小顯示；分享的是 layer 的原尺寸圖
                    Box(
                        Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(Constraints.fixed(EXPORT_WIDTH, EXPORT_HEIGHT))
                            val k = constraints.maxWidth / EXPORT_WIDTH.toFloat()
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.placeWithLayer(0, 0) {
                                    scaleX = k
                                    scaleY = k
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                            }
                        },
                    ) {
                        CompositionLocalProvider(LocalDensity provides Density(EXPORT_DENSITY, 1f)) {
                            Box(
                                Modifier.drawWithContent {
                                    layer.record { this@drawWithContent.drawContent() }
                                    drawLayer(layer)
                                },
                            ) {
                                YearReviewShareCanvas(page, data, cardTheme)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(NnoworkSpacing.md))
            if (ready) {
                PixelButton(onClick = onShare, backgroundColor = colors.buttonBG, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.IosShare, null, tint = colors.buttonText)
                    Spacer(Modifier.width(NnoworkSpacing.xs))
                    Text(stringResource(R.string.yr_share), style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, colors.buttonText))
                }
                Spacer(Modifier.height(NnoworkSpacing.xs))
                PixelButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.SaveAlt, null, tint = NnoworkTheme.colors.primaryText)
                    Spacer(Modifier.width(NnoworkSpacing.xs))
                    Text(stringResource(R.string.master_save_photos), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            } else {
                Row(Modifier.heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = colors.secondaryText, strokeWidth = 2.dp)
                    Spacer(Modifier.width(NnoworkSpacing.xs))
                    Text(stringResource(R.string.yr_share_preparing), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.SemiBold, colors.secondaryText))
                }
            }
            Row(Modifier.padding(vertical = NnoworkSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, null, tint = colors.secondaryText, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(NnoworkSpacing.xxs))
                Text(stringResource(R.string.yr_share_no_company), style = pixelTextStyle(NnoworkTheme.type.small, color = colors.secondaryText))
            }
        }
    }
}

private const val EXPORT_DENSITY = 3f
private const val EXPORT_WIDTH = 1080
private const val EXPORT_HEIGHT = 1920
