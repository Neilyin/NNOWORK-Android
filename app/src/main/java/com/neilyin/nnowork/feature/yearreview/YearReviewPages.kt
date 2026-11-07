package com.neilyin.nnowork.feature.yearreview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.R
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.ThemeStampMark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** 一頁的內容（上下預留進度條、分享鈕的位置）。 */
@Composable
fun YearReviewPageContent(
    page: YearReviewPage,
    data: YearReviewData,
    s: Float,
    animated: Boolean,
    isExport: Boolean,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .padding(top = (YearReviewLayout.TOP_INSET * s).dp, bottom = (YearReviewLayout.BOTTOM_INSET * s).dp),
    ) {
        val p = PageArgs(data, s, animated, isExport, paused)
        when (page) {
            YearReviewPage.OPENING -> OpeningPage(p)
            YearReviewPage.ORIGIN -> OriginPage(p)
            YearReviewPage.STAMP_COUNT -> StampCountPage(p)
            YearReviewPage.REASONS -> ReasonsPage(p)
            YearReviewPage.TIME_OF_DAY -> TimeOfDayPage(p)
            YearReviewPage.WEEKDAY -> WeekdayPage(p)
            YearReviewPage.STREAK -> StreakPage(p)
            YearReviewPage.CARDS -> CardsPage(p)
            YearReviewPage.SAVINGS -> SavingsPage(p)
            YearReviewPage.TREE_HOLE -> TreeHolePage(p)
            YearReviewPage.COINS -> CoinsPage(p)
            YearReviewPage.TITLE -> TitlePage(p)
            YearReviewPage.SUMMARY -> SummaryPage(p)
        }
    }
}

private class PageArgs(
    val data: YearReviewData,
    val s: Float,
    val animated: Boolean,
    val isExport: Boolean,
    val paused: Boolean,
) {
    /** 跑進場動畫；減少動態效果與分享圖直接呈現結束後的樣子。 */
    val plays: Boolean get() = animated && !isExport
}

@Composable
private fun rememberFormat(): YearReviewFormat {
    val context = LocalContext.current
    return remember(context) { YearReviewFormat(context) }
}

/** iOS Spacer(minLength:)：至少 [min]，多的空間平均分給各個彈性空白。 */
@Composable
private fun ColumnScope.Flex(min: Float, s: Float) {
    if (min > 0f) Spacer(Modifier.height((min * s).dp))
    Spacer(Modifier.weight(1f))
}

/** 延遲後淡入的進度（不播時直接 1）。 */
@Composable
private fun delayedProgress(plays: Boolean, trigger: Boolean, delayMs: Long, durationMs: Int = 400): Float {
    val progress = remember { Animatable(if (plays) 0f else 1f) }
    LaunchedEffect(trigger) {
        if (!plays || !trigger) return@LaunchedEffect
        delay(delayMs)
        progress.animateTo(1f, tween(durationMs, easing = LinearOutSlowInEasing))
    }
    return progress.value
}

@Composable
private fun PageColumn(s: Float, horizontal: Float = 24f, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = (horizontal * s).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

// MARK: - 開場

@Composable
private fun OpeningPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val s = p.s
    val data = p.data
    val digits = data.year.toString()
    val dialogText = (data.nickname?.let { fmt.string(R.string.yr_greeting_named, it) } ?: fmt.string(R.string.yr_greeting_anonymous)) +
        "\n" + fmt.string(R.string.yr_greeting_body)
    var revealed by remember { mutableStateOf(!p.plays) }
    var typed by remember { mutableIntStateOf(if (p.plays) 0 else Int.MAX_VALUE) }
    // 暱稱可能在播放中才讀到：迴圈要看最新的長度
    val latestText by androidx.compose.runtime.rememberUpdatedState(dialogText)
    LaunchedEffect(Unit) {
        if (!p.plays) return@LaunchedEffect
        revealed = true
        delay(260)
        repeat(digits.length) {
            haptics.tick()
            delay(120)
        }
        delay(700)
        while (typed < latestText.length) {
            typed++
            delay(38)
        }
    }
    val journey = fmt.string(R.string.yr_journey)
    PageColumn(s, horizontal = 22f) {
        Flex(4f, s)
        // 大年份：數字一個個掉下來
        Row(
            Modifier.semantics(mergeDescendants = false) {
                heading()
                contentDescription = fmt.string(R.string.yr_year_in_review, data.year) + ", " + journey
            },
            horizontalArrangement = Arrangement.spacedBy((2 * s).dp),
        ) {
            digits.forEachIndexed { index, digit ->
                val drop = remember { Animatable(if (p.plays) 0f else 1f) }
                LaunchedEffect(revealed) {
                    if (!p.plays || !revealed) return@LaunchedEffect
                    delay(index * 120L)
                    drop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 230f))
                }
                Text(
                    digit.toString(),
                    modifier = Modifier
                        .clearAndSetSemantics { }
                        .graphicsLayer {
                            translationY = (1 - drop.value) * -90f * s * density
                            alpha = drop.value.coerceIn(0f, 1f)
                        },
                    style = TextStyle(fontSize = (84 * s).sp, fontWeight = FontWeight.Black, color = YearReviewNight.title),
                )
            }
        }
        val subtitle = delayedProgress(p.plays, revealed, 550)
        YrText(
            journey, 24f, s, YearReviewNight.text,
            modifier = Modifier
                .padding(top = (2 * s).dp)
                .clearAndSetSemantics { }
                .graphicsLayer {
                    alpha = subtitle
                    translationY = (1 - subtitle) * 14f * s * density
                },
            weight = FontWeight.ExtraBold, maxLines = 2,
        )
        Flex(8f, s)
        val cow = delayedProgress(p.plays, revealed, 700)
        YearReviewNightCow(
            s = s, animated = p.plays, paused = p.paused,
            modifier = Modifier.graphicsLayer {
                alpha = cow
                val k = 0.85f + 0.15f * cow
                scaleX = k
                scaleY = k
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
        )
        Flex(8f, s)
        val window = delayedProgress(p.plays, revealed, 850)
        YearReviewNightWindow(
            s = s,
            modifier = Modifier
                .semantics { contentDescription = dialogText }
                .graphicsLayer {
                    alpha = window
                    translationY = (1 - window) * 20f * s * density
                },
        ) {
            // 未打出的部分用透明色排在同一段文字裡：視窗高度與換行位置一開始就固定
            val shown = typed.coerceAtMost(dialogText.length)
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = YearReviewNight.text)) { append(dialogText.substring(0, shown)) }
                    withStyle(SpanStyle(color = Color.Transparent)) { append(dialogText.substring(shown)) }
                },
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
                style = TextStyle(fontSize = (16 * s).sp, fontWeight = FontWeight.SemiBold, lineHeight = (24 * s).sp),
            )
        }
        if (!p.isExport && !data.isPlaceholder) {
            Row(
                Modifier.padding(top = (10 * s).dp).clearAndSetSemantics { },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6 * s).dp),
            ) {
                YrText(fmt.string(R.string.yr_tap_hint), 12f, s, YearReviewNight.textSoft, weight = FontWeight.SemiBold)
                YrBlinkingCursor("▶", s, YearReviewNight.textSoft, p.animated)
            }
        }
        Flex(4f, s)
    }
}

// MARK: - 起點

private val FLAG = listOf("PRRRRR", "PRYYRR", "PRRRRR", "P.....", "P.....", "P.....", "P.....", "PP....")

@Composable
private fun OriginPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val colors = NnoworkTheme.colors
    val s = p.s
    val data = p.data
    val days = data.adventureDays ?: 0
    var step by remember { mutableIntStateOf(if (p.plays) 0 else 99) }
    var punch by remember { mutableIntStateOf(0) }
    val line = remember { Animatable(if (p.plays) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!p.plays) return@LaunchedEffect
        step = 1
        delay(400)
        step = 2
        delay(250)
        haptics.stamp(full = false)
        launch { line.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
        for ((index, ms) in listOf(700L, 600L, 650L).withIndex()) {
            delay(ms)
            step = 3 + index
            if (index == 2) {
                haptics.tick()
                punch++
            }
        }
    }
    val firstReason = data.firstStamp?.reason?.trim().orEmpty().let { raw ->
        if (raw.isEmpty()) "" else YearReviewBuiltInReasons.display(LocalContext.current, raw)
    }
    val firstDate = data.firstStamp?.let { Instant.ofEpochMilli(it.epochMillis).atZone(data.zone).toLocalDate() }
    PageColumn(s) {
        Flex(0f, s)
        YrHeader(fmt.string(R.string.yr_origin_header), s, Modifier.yrReveal(step >= 1, (10 * s).dp))
        Flex(18f, s)
        // 時間軸：像素小旗插下，虛線往右延伸到「今天／年底」
        Column(
            Modifier.fillMaxWidth().yrPanel(s).padding((16 * s).dp).clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy((8 * s).dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy((6 * s).dp)) {
                val flagIn by animateFloatAsState(if (step >= 2) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 250f), label = "flag")
                YrPixelSprite(
                    FLAG, mapOf('P' to colors.primaryText, 'R' to PixelColors.red, 'Y' to PixelColors.yellow), (5 * s).dp,
                    Modifier.graphicsLayer {
                        alpha = flagIn.coerceIn(0f, 1f)
                        translationY = (1 - flagIn) * -70f * s * density
                    },
                )
                Canvas(Modifier.weight(1f).height((4 * s).dp).padding(bottom = 0.dp)) {
                    val dash = 8 * s * density
                    val gap = 6 * s * density
                    val limit = size.width * line.value
                    var x = 0f
                    while (x < limit) {
                        drawRect(colors.accent, Offset(x, 0f), Size(minOf(dash, limit - x), size.height))
                        x += dash + gap
                    }
                }
                YrPixelSprite(
                    listOf("..X..", "..X..", "XXXXX", "..X..", "..X.."), mapOf('X' to PixelColors.yellow), (4 * s).dp,
                    Modifier.graphicsLayer {
                        val on = if (line.value >= 1f) 1f else 0f
                        alpha = on
                        scaleX = 0.3f + 0.7f * on
                        scaleY = 0.3f + 0.7f * on
                    },
                )
            }
            Row(Modifier.fillMaxWidth().graphicsLayer { alpha = if (step >= 2) 1f else 0f }) {
                YrText(firstDate?.let(fmt::shortDate).orEmpty(), 12f, s, colors.secondaryText, weight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                YrText(
                    fmt.string(if (data.endsAtYearEnd) R.string.yr_origin_year_end else R.string.yr_origin_today),
                    12f, s, colors.secondaryText, weight = FontWeight.SemiBold,
                )
            }
        }
        Flex(18f, s)
        if (firstDate != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.yrReveal(step >= 3, (10 * s).dp)) {
                val dateText = fmt.fullDate(firstDate)
                val sentence = fmt.string(R.string.yr_origin_first, dateText)
                val start = sentence.indexOf(dateText)
                Text(
                    buildAnnotatedString {
                        append(sentence)
                        if (start >= 0) {
                            addStyle(SpanStyle(color = colors.accentForeground, fontWeight = FontWeight.ExtraBold, fontSize = (21 * s).sp), start, start + dateText.length)
                        }
                    },
                    style = TextStyle(fontSize = (19 * s).sp, fontWeight = FontWeight.SemiBold, color = colors.primaryText, textAlign = TextAlign.Center, lineHeight = (28 * s).sp),
                )
                Spacer(Modifier.height((6 * s).dp))
                YrText(fmt.string(R.string.yr_origin_started), 15f, s, colors.secondaryText, weight = FontWeight.Medium)
            }
        }
        if (firstReason.isNotEmpty()) {
            Column(
                Modifier
                    .padding(top = (14 * s).dp)
                    .yrReveal(step >= 4, (12 * s).dp)
                    .fillMaxWidth()
                    .yrPanel(s)
                    .padding(vertical = (12 * s).dp, horizontal = (16 * s).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((6 * s).dp),
            ) {
                YrText(fmt.string(R.string.yr_origin_reason), 13f, s, colors.secondaryText, weight = FontWeight.SemiBold)
                YrText(fmt.string(R.string.yr_quoted, firstReason), 18f, s, colors.primaryText, weight = FontWeight.Bold, maxLines = 2)
            }
        }
        Flex(18f, s)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.yrReveal(step >= 5, (12 * s).dp)) {
            YrText(
                fmt.string(if (data.endsAtYearEnd) R.string.yr_origin_until_year_end else R.string.yr_origin_until_today),
                17f, s, colors.primaryText, weight = FontWeight.SemiBold,
            )
            Row(Modifier.yrPunch(punch), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy((6 * s).dp)) {
                YrText(fmt.number(days), 64f, s, colors.accentForeground, weight = FontWeight.Black, maxLines = 1, autoShrink = true)
                YrText(
                    fmt.plural(R.plurals.yr_days_unit, days), 20f, s, colors.primaryText, weight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = (12 * s).dp),
                )
            }
        }
        Flex(0f, s)
    }
}

// MARK: - 蓋章總數

private class BurstPiece(val angle: Float, val distance: Float, val size: Float, val colorIndex: Int)

private val BURST: List<BurstPiece> = Random(1231).let { rng ->
    List(22) { i ->
        BurstPiece(
            angle = i / 22f * 2 * Math.PI.toFloat() + (rng.nextFloat() * 0.3f - 0.15f),
            distance = 0.55f + rng.nextFloat() * 0.45f,
            size = listOf(5f, 6f, 8f, 8f)[rng.nextInt(4)],
            colorIndex = rng.nextInt(6),
        )
    }
}

@Composable
private fun StampCountPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val colors = NnoworkTheme.colors
    val theme = LocalYearReviewCardTheme.current
    val s = p.s
    val data = p.data
    val total = data.totalStamps
    var displayed by remember { mutableIntStateOf(if (p.plays) 0 else total) }
    var showDetails by remember { mutableStateOf(!p.plays) }
    val punchScale = remember { Animatable(1f) }
    val wobble = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (p.isExport) return@LaunchedEffect
        if (!p.animated) {
            // 減少動態效果：數字直接呈現，仍給一下蓋章手感
            haptics.stamp(full = total > 0)
            return@LaunchedEffect
        }
        delay(500)
        if (total > 0) yearReviewCountUp(total, minOf(1.6, 0.5 + total * 0.015)) { displayed = it }
        haptics.stamp(full = total > 0)
        delay(80)
        if (total > 0) {
            launch { burst.snapTo(0f); burst.animateTo(1f, tween(900)) }
            launch {
                punchScale.animateTo(1.3f, tween(80))
                punchScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
            }
        } else {
            launch {
                wobble.animateTo(-9f, tween(100))
                wobble.animateTo(8f, tween(140))
                wobble.animateTo(-4f, tween(120))
                wobble.animateTo(0f, spring())
            }
        }
        delay(300)
        showDetails = true
    }
    val headline = when (total) {
        0 -> fmt.string(R.string.yr_stamp_none_headline)
        1 -> fmt.string(R.string.yr_stamp_one_headline)
        else -> data.averageDaysPerStamp?.let { average ->
            when {
                average >= 1.5 -> fmt.string(R.string.yr_stamp_every_n_days, average.roundToInt())
                average <= 0.67 -> fmt.string(R.string.yr_stamp_n_per_day, maxOf(2, (1 / average).roundToInt()))
                else -> fmt.string(R.string.yr_stamp_almost_daily)
            }
        }.orEmpty()
    }
    val flavor = fmt.string(
        when {
            total == 0 -> R.string.yr_stamp_flavor_0
            total == 1 -> R.string.yr_stamp_flavor_1
            total < 10 -> R.string.yr_stamp_flavor_2
            total < 50 -> R.string.yr_stamp_flavor_10
            total < 150 -> R.string.yr_stamp_flavor_50
            else -> R.string.yr_stamp_flavor_150
        },
    )
    val lead = fmt.string(R.string.yr_stamp_count_lead)
    val unit = fmt.plural(R.plurals.yr_stamp_unit, total)
    PageColumn(s) {
        Column(
            Modifier.fillMaxSize().clearAndSetSemantics { contentDescription = listOf(lead, total.toString(), unit, headline, flavor).joinToString(" ") },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Flex(0f, s)
            YrText(lead, 24f, s, colors.primaryText, weight = FontWeight.Bold)
            Box(contentAlignment = Alignment.Center) {
                val burstColors = listOf(colors.accent, PixelColors.yellow, PixelColors.red, PixelColors.green, PixelColors.blue, PixelColors.purple)
                Canvas(Modifier.requiredSize((340 * s).dp, (300 * s).dp)) {
                    val progress = burst.value
                    if (progress >= 1f) return@Canvas
                    val eased = 1 - (1 - progress) * (1 - progress) * (1 - progress)
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = minOf(size.width, size.height) / 2
                    BURST.forEach { piece ->
                        val d = piece.distance * radius * eased
                        val x = center.x + cos(piece.angle) * d
                        val y = center.y + sin(piece.angle) * d + 40 * density * progress * progress
                        val side = piece.size * density
                        drawRect(burstColors[piece.colorIndex].copy(alpha = 1 - progress), Offset(x, y), Size(side, side))
                    }
                }
                YrText(
                    displayed.toString(), 120f, s, colors.accentForeground, weight = FontWeight.Black, maxLines = 1, autoShrink = true,
                    modifier = Modifier.graphicsLayer {
                        scaleX = punchScale.value
                        scaleY = punchScale.value
                        rotationZ = wobble.value
                    },
                )
            }
            YrText(unit, 24f, s, colors.primaryText, weight = FontWeight.Bold)
            Flex(16f, s)
            MiniCard(theme, displayed, total, showDetails, s, p.plays)
            Flex(18f, s)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((8 * s).dp),
                modifier = Modifier.yrReveal(showDetails, (12 * s).dp),
            ) {
                YrText(headline, 20f, s, colors.primaryText, weight = FontWeight.Bold)
                YrText(flavor, 15f, s, colors.secondaryText, weight = FontWeight.Medium, lineSpacing = 3f)
            }
            Flex(0f, s)
        }
    }
}

@Composable
private fun MiniCard(theme: CardTheme, displayed: Int, total: Int, showDetails: Boolean, s: Float, plays: Boolean) {
    val colors = NnoworkTheme.colors
    val columns = 6
    val rows = 3
    val cellCount = columns * rows
    val filled = minOf(cellCount, displayed)
    val cell = (34 * s).dp
    val radius = (NnoworkRadius.card.value * s).dp
    Box(Modifier.rotate(-2f)) {
        Column(
            Modifier
                .drawBehind {
                    drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(0f, 4 * s * density), size, CornerRadius(radius.toPx()))
                }
                .background(theme.cardSurfaceColor, RoundedCornerShape(radius))
                .border(2.dp, theme.borderColor.copy(alpha = 0.85f), RoundedCornerShape(radius))
                .padding((12 * s).dp),
            verticalArrangement = Arrangement.spacedBy((8 * s).dp),
        ) {
            repeat(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy((8 * s).dp)) {
                    repeat(columns) { column ->
                        val index = row * columns + column
                        StampCell(theme, index, index < filled, cell, s, plays)
                    }
                }
            }
        }
        if (total > cellCount && showDetails) {
            OverflowBadge("+${total - cellCount}", s, colors.accent, colors.onAccent, Modifier.align(Alignment.BottomEnd).offset((12 * s).dp, (12 * s).dp))
        }
    }
}

@Composable
private fun StampCell(theme: CardTheme, index: Int, stamped: Boolean, size: androidx.compose.ui.unit.Dp, s: Float, plays: Boolean) {
    val radius = (minOf(theme.cellCornerRadius.value, NnoworkRadius.small.value) * s).dp
    val shape = RoundedCornerShape(radius)
    val progress by animateFloatAsState(
        targetValue = if (stamped) 1f else 0f,
        animationSpec = if (plays) spring(dampingRatio = 0.55f, stiffness = 580f) else tween(0),
        label = "stampCell",
    )
    Box(
        Modifier
            .size(size)
            .background(if (stamped) theme.stampedCellColor else theme.emptyCellColor, shape)
            .border(1.5.dp, theme.borderColor.copy(alpha = if (stamped) theme.stampedCellBorderOpacity else theme.emptyCellBorderOpacity), shape),
        contentAlignment = Alignment.Center,
    ) {
        ThemeStampMark(
            theme = theme, index = index,
            modifier = Modifier.size(size * 0.55f).graphicsLayer {
                alpha = progress.coerceIn(0f, 1f)
                val k = 1.8f - 0.8f * progress
                scaleX = k
                scaleY = k
            },
        )
    }
}

@Composable
private fun OverflowBadge(text: String, s: Float, background: Color, foreground: Color, modifier: Modifier) {
    Box(
        modifier
            .rotate(8f)
            .background(background, RoundedCornerShape(NnoworkRadius.small))
            .padding(horizontal = (10 * s).dp, vertical = (4 * s).dp),
    ) {
        YrText(text, 16f, s, foreground, weight = FontWeight.ExtraBold)
    }
}

// MARK: - 原因

@Composable
private fun ReasonsPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val colors = NnoworkTheme.colors
    val s = p.s
    val insight = p.data.reasonInsight
    val items = insight?.items.orEmpty()
    val isKeywords = insight?.isKeywords == true
    val runnerUps = items.drop(1)
    var punch by remember { mutableIntStateOf(0) }
    val rowDelays = runnerUps.indices.map { if (it == 0) 900L else if (isKeywords) 400L else 600L }
    val step = rememberYearReviewSteps(p.plays, listOf(150L, 600L) + rowDelays + 600L) { value ->
        if (value == 2) {
            haptics.stamp(full = false)
            punch++
        } else if (value >= 3 && value < 3 + runnerUps.size) {
            haptics.tick()
        }
    }
    val flavor = if (isKeywords) fmt.string(R.string.yr_keywords_flavor) else fmt.string(
        when (items.firstOrNull()?.builtInKey) {
            "老闆廢話太多" -> R.string.yr_reason_flavor_boss
            "無效會議" -> R.string.yr_reason_flavor_meeting
            "需求一直改" -> R.string.yr_reason_flavor_requirements
            "同事雷" -> R.string.yr_reason_flavor_coworker
            "加班沒錢" -> R.string.yr_reason_flavor_overtime
            "心情不好" -> R.string.yr_reason_flavor_mood
            else -> R.string.yr_reason_flavor_custom
        },
    )
    PageColumn(s) {
        Flex(0f, s)
        YrHeader(
            fmt.string(if (isKeywords) R.string.yr_keywords_header else R.string.yr_reasons_header), s,
            Modifier.yrReveal(step >= 1, (10 * s).dp),
        )
        Flex(18f, s)
        items.firstOrNull()?.let { first ->
            Column(
                Modifier
                    .yrStampIn(step >= 2, 1.25f)
                    .yrPunch(punch)
                    .fillMaxWidth()
                    .yrPanel(s)
                    .padding(vertical = (20 * s).dp, horizontal = (16 * s).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((10 * s).dp),
            ) {
                Row(
                    Modifier
                        .background(colors.accent, RoundedCornerShape((NnoworkRadius.small.value * s).dp))
                        .padding(horizontal = (12 * s).dp, vertical = (5 * s).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((6 * s).dp),
                ) {
                    Icon(Icons.Filled.WorkspacePremium, null, tint = colors.onAccent, modifier = Modifier.size((15 * s).dp))
                    YrText(fmt.string(R.string.yr_reasons_rank, 1), 14f, s, colors.onAccent, weight = FontWeight.ExtraBold)
                }
                YrText(fmt.reasonText(first), 30f, s, colors.accentForeground, weight = FontWeight.ExtraBold, maxLines = 2)
                YrText(fmt.string(R.string.yr_times, first.count), 17f, s, colors.secondaryText, weight = FontWeight.SemiBold)
            }
        }
        Column(
            Modifier.padding(top = (14 * s).dp),
            verticalArrangement = Arrangement.spacedBy(((if (isKeywords) 8 else 10) * s).dp),
        ) {
            runnerUps.forEachIndexed { index, item ->
                Row(
                    Modifier
                        .yrReveal(step >= 3 + index, 0.dp, dx = (40 * s).dp)
                        .fillMaxWidth()
                        .yrPanel(s)
                        .padding(horizontal = (16 * s).dp, vertical = ((if (isKeywords) 10 else 12) * s).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((12 * s).dp),
                ) {
                    YrText("${index + 2}", 22f, s, colors.accentForeground, weight = FontWeight.Black, modifier = Modifier.width((26 * s).dp))
                    YrText(fmt.reasonText(item), 18f, s, colors.primaryText, weight = FontWeight.Bold, maxLines = 2, align = TextAlign.Start, modifier = Modifier.weight(1f))
                    YrText(fmt.string(R.string.yr_times, item.count), 15f, s, colors.secondaryText, weight = FontWeight.SemiBold)
                }
            }
        }
        Flex(18f, s)
        YrFlavor(flavor, s, Modifier.yrReveal(step >= 3 + runnerUps.size, (10 * s).dp))
        Flex(0f, s)
    }
}

// MARK: - 時段

@Composable
private fun TimeOfDayPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val colors = NnoworkTheme.colors
    val s = p.s
    val data = p.data
    val slot = data.dominantTimeSlot ?: YearReviewTimeSlot.AFTERNOON
    var punch by remember { mutableIntStateOf(0) }
    val step = rememberYearReviewSteps(p.plays, listOf(150L, 450L, 1100L, 450L, 600L)) { value ->
        if (value == 3) {
            haptics.stamp(full = false)
            punch++
        }
    }
    PageColumn(s) {
        Flex(0f, s)
        YrHeader(fmt.string(R.string.yr_time_header), s, Modifier.yrReveal(step >= 1, (10 * s).dp))
        Flex(20f, s)
        YrText(
            fmt.slotTypeName(slot), 38f, s, colors.accentForeground, weight = FontWeight.ExtraBold, maxLines = 2, autoShrink = true,
            modifier = Modifier.yrStampIn(step >= 3, 1.3f).yrPunch(punch),
        )
        data.peakHour(slot)?.let { hour ->
            YrText(
                fmt.string(R.string.yr_time_peak_hour, fmt.hour(hour)), 18f, s, colors.primaryText, weight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = (10 * s).dp).yrReveal(step >= 4, (10 * s).dp),
            )
        }
        Flex(20f, s)
        Column(
            Modifier.yrReveal(step >= 2, (16 * s).dp).fillMaxWidth().yrPanel(s).padding((16 * s).dp).clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy((6 * s).dp),
        ) {
            val bins = data.hourHistogram
            val maxCount = maxOf(bins.max(), 1)
            val peak = data.dominantTimeSlot?.let { data.peakHour(it) }
            val chartHeight = 96 * s
            Row(
                Modifier.fillMaxWidth().height(chartHeight.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy((3 * s).dp),
            ) {
                for (hour in 0 until 24) {
                    val target = if (step >= 2) maxOf(3 * s, chartHeight * bins[hour] / maxCount) else 3 * s
                    val height by animateFloatAsState(
                        target,
                        if (p.plays) spring(dampingRatio = 0.75f, stiffness = 160f) else tween(0),
                        label = "hourBar",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(height.dp)
                            .background(if (hour == peak) colors.accent else colors.primaryText.copy(alpha = 0.18f), RoundedCornerShape((2 * s).dp)),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy((3 * s).dp)) {
                for (hour in 0 until 24) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (hour % 6 == 0) {
                            Text(
                                "$hour",
                                style = TextStyle(fontSize = (10 * s).sp, fontWeight = FontWeight.SemiBold, color = colors.secondaryText),
                                softWrap = false,
                                modifier = Modifier.requiredSize(width = (24 * s).dp, height = (14 * s).dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
        Flex(18f, s)
        YrFlavor(
            fmt.string(
                when (slot) {
                    YearReviewTimeSlot.EARLY_MORNING -> R.string.yr_time_flavor_early_morning
                    YearReviewTimeSlot.WORK_MORNING -> R.string.yr_time_flavor_work_morning
                    YearReviewTimeSlot.AFTERNOON -> R.string.yr_time_flavor_afternoon
                    YearReviewTimeSlot.EVENING -> R.string.yr_time_flavor_evening
                    YearReviewTimeSlot.LATE_NIGHT -> R.string.yr_time_flavor_late_night
                },
            ),
            s, Modifier.yrReveal(step >= 5, (10 * s).dp),
        )
        Flex(0f, s)
    }
}

// MARK: - 星期

@Composable
private fun WeekdayPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val colors = NnoworkTheme.colors
    val s = p.s
    val data = p.data
    val weekday = data.peakWeekday ?: java.time.DayOfWeek.MONDAY
    var punch by remember { mutableIntStateOf(0) }
    val step = rememberYearReviewSteps(p.plays, listOf(150L, 400L, 1100L, 500L, 600L)) { value ->
        if (value == 3) {
            haptics.stamp(full = false)
            punch++
        }
    }
    PageColumn(s) {
        Flex(0f, s)
        YrHeader(fmt.string(R.string.yr_weekday_header), s, Modifier.yrReveal(step >= 1, (10 * s).dp))
        Flex(20f, s)
        val counts = data.weekdayCounts
        val maxCount = maxOf(counts.values.maxOrNull() ?: 1, 1)
        val chartHeight = 110 * s
        Row(
            Modifier
                .yrReveal(step >= 2, (16 * s).dp)
                .fillMaxWidth()
                .yrPanel(s)
                .padding((16 * s).dp)
                .height((chartHeight + 44 * s).dp)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy((10 * s).dp),
        ) {
            java.time.DayOfWeek.entries.forEachIndexed { _, day ->
                val count = counts[day] ?: 0
                val isPeak = day == data.peakWeekday
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((6 * s).dp, Alignment.Bottom)) {
                    val grown = step >= 2
                    YrText("$count", 12f, s, if (isPeak) colors.accentForeground else colors.secondaryText, weight = FontWeight.Bold, modifier = Modifier.graphicsLayer { alpha = if (grown) 1f else 0f })
                    val height by animateFloatAsState(
                        if (grown) maxOf(4 * s, chartHeight * count / maxCount) else 4 * s,
                        if (p.plays) spring(dampingRatio = 0.7f, stiffness = 160f) else tween(0),
                        label = "weekdayBar",
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(height.dp)
                            .background(if (isPeak) colors.accent else colors.primaryText.copy(alpha = 0.18f), RoundedCornerShape((3 * s).dp)),
                    )
                    YrText(
                        fmt.weekdayShort(day), 12f, s, if (isPeak) colors.primaryText else colors.secondaryText,
                        weight = if (isPeak) FontWeight.ExtraBold else FontWeight.SemiBold, maxLines = 1, autoShrink = true,
                    )
                }
            }
        }
        Flex(20f, s)
        YrText(
            fmt.string(R.string.yr_weekday_nemesis, fmt.weekdayName(weekday)), 32f, s, colors.accentForeground,
            weight = FontWeight.ExtraBold, maxLines = 2, autoShrink = true,
            modifier = Modifier.yrStampIn(step >= 3, 1.3f).yrPunch(punch),
        )
        data.peakMonth?.let { month ->
            YrText(
                fmt.string(R.string.yr_weekday_peak_month, fmt.monthName(month)), 18f, s, colors.primaryText, weight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = (12 * s).dp).yrReveal(step >= 4, (10 * s).dp),
            )
        }
        Flex(18f, s)
        YrFlavor(
            fmt.string(
                when (weekday) {
                    java.time.DayOfWeek.MONDAY -> R.string.yr_weekday_flavor_monday
                    java.time.DayOfWeek.FRIDAY -> R.string.yr_weekday_flavor_friday
                    java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY -> R.string.yr_weekday_flavor_weekend
                    else -> R.string.yr_weekday_flavor_midweek
                },
            ),
            s, Modifier.yrReveal(step >= 5, (10 * s).dp),
        )
        Flex(0f, s)
    }
}

// MARK: - 連續

@Composable
private fun StreakPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val colors = NnoworkTheme.colors
    val theme = LocalYearReviewCardTheme.current
    val s = p.s
    val streak = p.data.longestStreak
    val length = streak?.length ?: 0
    var displayed by remember { mutableIntStateOf(if (p.plays) 0 else length) }
    var step by remember { mutableIntStateOf(if (p.plays) 0 else 99) }
    var punch by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        if (!p.plays) return@LaunchedEffect
        step = 1
        delay(400)
        yearReviewCountUp(length, minOf(1.4, 0.5 + length * 0.05)) { displayed = it }
        haptics.stamp(full = true)
        delay(80)
        punch++
        delay(200)
        step = 2
        delay(500)
        step = 3
    }
    val maxBlocks = 14
    PageColumn(s) {
        Flex(0f, s)
        YrHeader(fmt.string(R.string.yr_streak_header), s, Modifier.yrReveal(step >= 1, (10 * s).dp))
        YrText("$displayed", 110f, s, colors.accentForeground, weight = FontWeight.Black, maxLines = 1, autoShrink = true, modifier = Modifier.yrPunch(punch))
        YrText(fmt.string(R.string.yr_streak_unit), 22f, s, colors.primaryText, weight = FontWeight.Bold)
        if (streak != null) {
            YrText(
                fmt.string(R.string.yr_date_range, fmt.monthDay(streak.start), fmt.monthDay(streak.end)), 16f, s, colors.secondaryText,
                weight = FontWeight.SemiBold, modifier = Modifier.padding(top = (8 * s).dp).yrReveal(step >= 2, (8 * s).dp),
            )
        }
        Flex(20f, s)
        val shown = minOf(length, maxBlocks)
        val lit = minOf(displayed, maxBlocks)
        val columns = 7
        val rows = (shown + columns - 1) / columns
        val radius = (NnoworkRadius.card.value * s).dp
        Box(Modifier.rotate(1.5f).clearAndSetSemantics { }) {
            Column(
                Modifier
                    .drawBehind { drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(0f, 4 * s * density), size, CornerRadius(radius.toPx())) }
                    .background(theme.cardSurfaceColor, RoundedCornerShape(radius))
                    .border(2.dp, theme.borderColor.copy(alpha = 0.85f), RoundedCornerShape(radius))
                    .padding((12 * s).dp),
                verticalArrangement = Arrangement.spacedBy((6 * s).dp),
            ) {
                repeat(rows) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy((6 * s).dp)) {
                        repeat(columns) { column ->
                            val index = row * columns + column
                            if (index < shown) {
                                StampCell(theme, index, index < lit, (30 * s).dp, s, p.plays)
                            } else {
                                Spacer(Modifier.size((30 * s).dp))
                            }
                        }
                    }
                }
            }
            if (length > maxBlocks && step >= 2) {
                OverflowBadge("+${length - maxBlocks}", s, colors.accent, colors.onAccent, Modifier.align(Alignment.BottomEnd).offset((10 * s).dp, (10 * s).dp))
            }
        }
        Flex(18f, s)
        YrFlavor(
            fmt.string(
                when {
                    length < 4 -> R.string.yr_streak_flavor_short
                    length < 7 -> R.string.yr_streak_flavor_week
                    length < 14 -> R.string.yr_streak_flavor_marathon
                    else -> R.string.yr_streak_flavor_habit
                },
            ),
            s, Modifier.yrReveal(step >= 3, (10 * s).dp),
        )
        Flex(0f, s)
    }
}

// MARK: - 卡片與工作

private class StatRow(val icon: ImageVector, val tint: Color, val label: String, val value: String)

@Composable
private fun CardsPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val colors = NnoworkTheme.colors
    val s = p.s
    val data = p.data
    val rows = buildList {
        if (data.completedCardCount > 0) {
            add(StatRow(Icons.Filled.GridView, PixelColors.orange, fmt.string(R.string.yr_cards_completed), fmt.string(R.string.yr_cards_count, data.completedCardCount)))
        }
        if (data.becameMaster) {
            add(StatRow(Icons.Filled.WorkspacePremium, PixelColors.purple, fmt.string(R.string.yr_cards_master), fmt.string(R.string.yr_cards_master_value)))
        }
        if (data.jobsChanged > 0) {
            add(StatRow(Icons.Filled.Work, PixelColors.blue, fmt.string(R.string.yr_cards_jobs), fmt.string(R.string.yr_jobs_count, data.jobsChanged)))
        }
    }
    val step = rememberYearReviewSteps(p.plays, listOf(150L) + List(rows.size) { 650L } + 600L) { value ->
        if (value >= 2 && value < 2 + rows.size) haptics.stamp(full = false)
    }
    PageColumn(s) {
        Flex(0f, s)
        YrHeader(fmt.string(R.string.yr_cards_header), s, Modifier.yrReveal(step >= 1, (10 * s).dp))
        Flex(24f, s)
        Column(verticalArrangement = Arrangement.spacedBy((14 * s).dp)) {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier
                        .yrStampIn(step >= 2 + index, 0.85f)
                        .fillMaxWidth()
                        .yrPanel(s)
                        .padding(horizontal = (16 * s).dp, vertical = (14 * s).dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((14 * s).dp),
                ) {
                    Box(
                        Modifier.size((46 * s).dp).background(row.tint, RoundedCornerShape((NnoworkRadius.field.value * s).dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(row.icon, null, tint = Color.White, modifier = Modifier.size((24 * s).dp))
                    }
                    YrText(row.label, 18f, s, colors.primaryText, weight = FontWeight.SemiBold, align = TextAlign.Start, maxLines = 2, modifier = Modifier.weight(1f))
                    YrText(row.value, 24f, s, colors.accentForeground, weight = FontWeight.ExtraBold, maxLines = 1, autoShrink = true)
                }
            }
        }
        Flex(24f, s)
        YrFlavor(
            fmt.string(
                when {
                    data.becameMaster -> R.string.yr_cards_flavor_master
                    data.jobsChanged > 0 -> R.string.yr_cards_flavor_jobs
                    else -> R.string.yr_cards_flavor_default
                },
            ),
            s, Modifier.yrReveal(step >= 2 + rows.size, (10 * s).dp),
        )
        Flex(0f, s)
    }
}

// MARK: - 自由預備金

@Composable
private fun SavingsPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val colors = NnoworkTheme.colors
    val s = p.s
    val data = p.data
    val target = data.savingsPercent
    var percent by remember { mutableIntStateOf(if (p.plays) 0 else target) }
    var step by remember { mutableIntStateOf(if (p.plays) 0 else 99) }
    var punch by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        if (!p.plays) return@LaunchedEffect
        step = 1
        delay(450)
        yearReviewCountUp(target, 1.3) { percent = it }
        haptics.stamp(full = target >= 100)
        delay(80)
        punch++
        delay(250)
        step = 2
        delay(500)
        step = 3
    }
    val segments = 20
    val progress = data.savingsProgress ?: 0.0
    PageColumn(s) {
        Flex(0f, s)
        YrHeader(fmt.string(R.string.yr_savings_header), s, Modifier.yrReveal(step >= 1, (10 * s).dp))
        YrText(fmt.string(R.string.yr_savings_subtitle), 15f, s, colors.secondaryText, weight = FontWeight.Medium, modifier = Modifier.padding(top = (6 * s).dp).yrReveal(step >= 1, (10 * s).dp))
        Flex(16f, s)
        YrText("$percent%", 96f, s, colors.accentForeground, weight = FontWeight.Black, maxLines = 1, autoShrink = true, modifier = Modifier.yrPunch(punch))
        val lit = (percent / 100.0 * segments).toInt()
        Row(
            Modifier.padding(top = (12 * s).dp).fillMaxWidth().yrPanel(s).padding((6 * s).dp).clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy((3 * s).dp),
        ) {
            repeat(segments) { index ->
                Box(
                    Modifier.weight(1f).height((20 * s).dp)
                        .background(if (index < lit) colors.accent else colors.primaryText.copy(alpha = 0.14f), RoundedCornerShape((2 * s).dp)),
                )
            }
        }
        // 金額只在 App 裡看得到；分享圖只放百分比
        if (!p.isExport) {
            YrText(
                fmt.string(R.string.yr_savings_amounts, fmt.money(data.savedAmount), fmt.money(data.targetAmount)), 16f, s, colors.secondaryText,
                weight = FontWeight.SemiBold, modifier = Modifier.padding(top = (12 * s).dp).yrReveal(step >= 2, (8 * s).dp),
            )
        }
        Flex(20f, s)
        YrFlavor(
            fmt.string(
                when {
                    progress >= 1 -> R.string.yr_savings_flavor_done
                    progress >= 0.5 -> R.string.yr_savings_flavor_half
                    else -> R.string.yr_savings_flavor_start
                },
            ),
            s, Modifier.yrReveal(step >= 3, (10 * s).dp),
        )
        Flex(0f, s)
    }
}

// MARK: - 取暖樹洞

private enum class AnswerTier(val threshold: Int, val nameRes: Int, val icon: ImageVector) {
    ROOKIE(0, R.string.treehole_answer_badge_rookie, Icons.Filled.Spa),
    PASSERBY(100, R.string.treehole_answer_badge_good_samaritan, Icons.Filled.Person),
    SENIOR(500, R.string.treehole_answer_badge_corner_regular, Icons.Filled.Star),
    STRATEGIST(2_000, R.string.treehole_answer_badge_work_strategist, Icons.Filled.Lightbulb),
    MASTER(5_000, R.string.treehole_answer_badge_answer_master, Icons.Filled.EmojiEvents);

    companion object {
        fun forExp(exp: Int): AnswerTier = entries.last { exp.coerceAtLeast(0) >= it.threshold }
    }
}

@Composable
private fun AnswerTier.color(): Color = when (this) {
    AnswerTier.ROOKIE -> NnoworkTheme.colors.treeHole.tierRookie
    AnswerTier.PASSERBY -> NnoworkTheme.colors.treeHole.tierPasserby
    AnswerTier.SENIOR -> NnoworkTheme.colors.treeHole.tierSenior
    AnswerTier.STRATEGIST -> NnoworkTheme.colors.treeHole.tierStrategist
    AnswerTier.MASTER -> NnoworkTheme.colors.treeHole.tierMaster
}

@Composable
private fun TreeHolePage(p: PageArgs) {
    val fmt = rememberFormat()
    val colors = NnoworkTheme.colors
    val s = p.s
    val stats = p.data.treeHole
    if (stats == null) {
        // 資料還沒回來：顯示載入，播放器在這頁暫停計時（失敗或逾時會自動跳過這頁）
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(color = colors.primaryText, modifier = Modifier.size((28 * s).dp), strokeWidth = 3.dp)
            Spacer(Modifier.height((14 * s).dp))
            YrText(fmt.string(R.string.yr_treehole_loading), 15f, s, colors.secondaryText, weight = FontWeight.SemiBold)
        }
        return
    }
    val haptics = rememberYearReviewHaptics()
    val step = rememberYearReviewSteps(p.plays, listOf(150L, 350L, 250L, 250L, 250L, 600L, 500L)) { value ->
        if (value in 2..5) haptics.tick()
        if (value == 6) haptics.stamp(full = false)
    }
    val tier = AnswerTier.forExp(stats.exp)
    PageColumn(s) {
        Flex(0f, s)
        YrHeader(fmt.string(R.string.yr_treehole_header), s, Modifier.yrReveal(step >= 1, (10 * s).dp))
        Flex(18f, s)
        val tiles = listOf(
            Triple(stats.stampPosts, R.string.yr_treehole_posts, null),
            Triple(stats.questions, R.string.yr_treehole_questions, null),
            Triple(stats.comments, R.string.yr_treehole_comments, null),
            Triple(stats.reactions, R.string.yr_treehole_reactions, "❤️ ${fmt.number(stats.hearts)}  💪 ${fmt.number(stats.cheers)}  😢 ${fmt.number(stats.cries)}"),
        )
        Column(verticalArrangement = Arrangement.spacedBy((10 * s).dp)) {
            tiles.chunked(2).forEachIndexed { rowIndex, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy((10 * s).dp)) {
                    pair.forEachIndexed { columnIndex, (value, label, detail) ->
                        val order = rowIndex * 2 + columnIndex
                        Column(
                            Modifier
                                .weight(1f)
                                .yrStampIn(step >= 2 + order, 0.85f)
                                .heightIn(min = (96 * s).dp)
                                .yrPanel(s)
                                .padding(vertical = (14 * s).dp, horizontal = (8 * s).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((4 * s).dp, Alignment.CenterVertically),
                        ) {
                            YrText(fmt.number(value), 34f, s, colors.accentForeground, weight = FontWeight.ExtraBold, maxLines = 1, autoShrink = true)
                            YrText(fmt.string(label), 13f, s, colors.secondaryText, weight = FontWeight.SemiBold, maxLines = 1, autoShrink = true)
                            if (detail != null) YrText(detail, 11f, s, colors.secondaryText, weight = FontWeight.SemiBold, maxLines = 2)
                        }
                    }
                }
            }
        }
        Row(
            Modifier
                .padding(top = (14 * s).dp)
                .yrReveal(step >= 6, (14 * s).dp)
                .fillMaxWidth()
                .yrPanel(s)
                .padding((14 * s).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((14 * s).dp),
        ) {
            Box(Modifier.size((50 * s).dp).background(tier.color(), CircleShape), contentAlignment = Alignment.Center) {
                Icon(tier.icon, null, tint = Color.White, modifier = Modifier.size((26 * s).dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((4 * s).dp)) {
                YrText(fmt.string(tier.nameRes), 18f, s, colors.primaryText, weight = FontWeight.ExtraBold, align = TextAlign.Start)
                YrText(fmt.string(R.string.yr_treehole_exp, fmt.number(stats.exp)), 14f, s, colors.secondaryText, weight = FontWeight.SemiBold, align = TextAlign.Start)
                if (stats.helpful > 0) {
                    YrText(fmt.plural(R.plurals.yr_helpful_count, stats.helpful, stats.helpful), 13f, s, colors.secondaryText, weight = FontWeight.Medium, align = TextAlign.Start)
                }
            }
        }
        Flex(18f, s)
        YrFlavor(fmt.string(R.string.yr_treehole_flavor), s, Modifier.yrReveal(step >= 7, (10 * s).dp))
        Flex(0f, s)
    }
}

// MARK: - 金幣

@Composable
private fun CoinsPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val colors = NnoworkTheme.colors
    val s = p.s
    val data = p.data
    val themes = data.purchasedThemes
    val target = data.coinsEarned
    var displayed by remember { mutableIntStateOf(if (p.plays) 0 else target) }
    var step by remember { mutableIntStateOf(if (p.plays) 0 else 99) }
    var punch by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        if (!p.plays) return@LaunchedEffect
        step = 1
        delay(400)
        yearReviewCountUp(target, minOf(1.4, 0.5 + target * 0.01)) { displayed = it }
        if (target > 0) {
            haptics.stamp(full = false)
            punch++
        }
        repeat(minOf(themes.size, 4)) {
            delay(300)
            step = 2 + it
            haptics.tick()
        }
        delay(400)
        step = 6
    }
    PageColumn(s) {
        Flex(0f, s)
        YrHeader(fmt.string(R.string.yr_coins_header), s, Modifier.yrReveal(step >= 1, (10 * s).dp))
        Row(
            Modifier.padding(top = (8 * s).dp).yrPunch(punch),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((10 * s).dp),
        ) {
            Icon(Icons.Filled.MonetizationOn, null, tint = PixelColors.yellow, modifier = Modifier.size((56 * s).dp))
            YrText(fmt.number(displayed), 84f, s, colors.accentForeground, weight = FontWeight.Black, maxLines = 1, autoShrink = true)
        }
        Flex(20f, s)
        if (themes.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().yrPanel(s).padding((16 * s).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((12 * s).dp),
            ) {
                YrText(fmt.string(R.string.yr_coins_themes), 15f, s, colors.primaryText, weight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy((12 * s).dp), verticalAlignment = Alignment.Top) {
                    themes.take(4).forEachIndexed { index, item ->
                        Column(
                            Modifier.width((64 * s).dp).yrStampIn(step >= 2 + index, 0.6f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((6 * s).dp),
                        ) {
                            val shape = RoundedCornerShape((NnoworkRadius.field.value * s).dp)
                            Box(
                                Modifier.size((54 * s).dp).background(item.stampedCellColor, shape).border(1.5.dp, item.borderColor.copy(alpha = 0.8f), shape),
                                contentAlignment = Alignment.Center,
                            ) {
                                ThemeStampMark(theme = item, index = 0, modifier = Modifier.size((24 * s).dp))
                            }
                            YrText(item.title, 12f, s, colors.primaryText, weight = FontWeight.SemiBold, maxLines = 2)
                        }
                    }
                }
                if (themes.size > 4) YrText("+${themes.size - 4}", 13f, s, colors.secondaryText, weight = FontWeight.Bold)
            }
        }
        Flex(18f, s)
        YrFlavor(
            fmt.string(if (themes.isEmpty()) R.string.yr_coins_flavor_none else R.string.yr_coins_flavor_spent),
            s, Modifier.yrReveal(step >= 6, (10 * s).dp),
        )
        Flex(0f, s)
    }
}

// MARK: - 年度稱號

@Composable
private fun TitlePage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val s = p.s
    var punch by remember { mutableIntStateOf(0) }
    val step = rememberYearReviewSteps(p.plays, listOf(150L, 700L, 450L, 600L)) { value ->
        if (value == 2) haptics.tick()
        if (value == 3) {
            haptics.success()
            punch++
        }
    }
    PageColumn(s, horizontal = 22f) {
        Flex(0f, s)
        YrText(
            fmt.string(R.string.yr_title_header), 22f, s, YearReviewNight.text, weight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { heading() }.yrReveal(step >= 1, (10 * s).dp),
        )
        Flex(10f, s)
        YearReviewNightCow(s = s, animated = p.plays, paused = p.paused, crowned = true, cowScale = 0.85f, modifier = Modifier.yrReveal(step >= 1, (10 * s).dp))
        Flex(10f, s)
        YearReviewNightWindow(s = s, title = fmt.string(R.string.yr_title_earned), modifier = Modifier.yrStampIn(step >= 2, 0.7f)) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy((12 * s).dp)) {
                YrText(
                    fmt.annualTitle(p.data), 28f, s, YearReviewNight.title, weight = FontWeight.ExtraBold, maxLines = 3, autoShrink = true,
                    modifier = Modifier.fillMaxWidth().yrStampIn(step >= 3, 1.5f).yrPunch(punch),
                )
                YrText(
                    fmt.annualTitleDescription(p.data), 14f, s, YearReviewNight.textSoft, weight = FontWeight.Medium, lineSpacing = 3f,
                    modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (step >= 4) 1f else 0f },
                )
            }
        }
        Flex(0f, s)
    }
}

// MARK: - 總結卡

@Composable
private fun SummaryPage(p: PageArgs) {
    val fmt = rememberFormat()
    val haptics = rememberYearReviewHaptics()
    val s = p.s
    val data = p.data
    val rows = buildList {
        add(StatRow(Icons.Filled.Verified, YearReviewNight.iconTeal, fmt.string(R.string.yr_summary_total),
            fmt.plural(R.plurals.yr_summary_stamps_value, data.totalStamps, data.totalStamps)))
        data.adventureDays?.let { days ->
            add(StatRow(Icons.Filled.Flag, YearReviewNight.iconCoral, fmt.string(R.string.yr_summary_days), fmt.plural(R.plurals.yr_summary_days_value, days, days)))
        }
        data.reasonInsight?.let { insight ->
            insight.items.firstOrNull()?.let { first ->
                add(StatRow(
                    Icons.Filled.Textsms, YearReviewNight.iconGold,
                    fmt.string(if (insight.isKeywords) R.string.yr_summary_top_keyword else R.string.yr_summary_top_reason),
                    fmt.reasonText(first),
                ))
            }
        }
        if (!data.isLite) {
            data.dominantTimeSlot?.let { add(StatRow(Icons.Filled.Schedule, YearReviewNight.iconBlue, fmt.string(R.string.yr_summary_slot), fmt.slotTypeName(it))) }
            data.longestStreak?.takeIf { it.length >= 2 }?.let {
                add(StatRow(Icons.Filled.LocalFireDepartment, YearReviewNight.iconAmber, fmt.string(R.string.yr_summary_streak), fmt.string(R.string.yr_summary_streak_value, it.length)))
            }
        }
        if (data.completedCardCount > 0) {
            add(StatRow(Icons.Filled.GridView, YearReviewNight.iconPink, fmt.string(R.string.yr_cards_completed), fmt.string(R.string.yr_cards_count, data.completedCardCount)))
        }
        data.treeHole?.takeIf { it.exp > 0 }?.let { stats ->
            add(StatRow(AnswerTier.forExp(stats.exp).icon, YearReviewNight.iconGold, fmt.string(R.string.yr_summary_exp), fmt.number(stats.exp)))
        }
    }
    val step = rememberYearReviewSteps(p.plays, listOf(150L, 400L) + List(maxOf(0, rows.size - 1)) { 220L } + 500L) { value ->
        if (value >= 2 && value < 2 + rows.size) haptics.tick()
        if (value == 2 + rows.size) haptics.success()
    }
    val greeting = data.nickname?.let { fmt.string(R.string.yr_summary_greeting_named, it) } ?: fmt.string(R.string.yr_summary_greeting_anonymous)
    val title = fmt.annualTitle(data)
    // 先試一般版，放不下（列多、英日文長）就換緊湊版
    FitsVertically(
        modifier = Modifier.fillMaxSize().padding(horizontal = (22 * s).dp),
        normal = { SummaryLayout(data.year, greeting, rows, title, step, s, tight = false) },
        tight = { SummaryLayout(data.year, greeting, rows, title, step, s, tight = true) },
    )
}

@Composable
private fun SummaryLayout(year: Int, greeting: String, rows: List<StatRow>, title: String, step: Int, s: Float, tight: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        YrText(year.toString(), if (tight) 34f else 44f, s, YearReviewNight.title, weight = FontWeight.Black, modifier = Modifier.yrReveal(step >= 1, (-14 * s).dp))
        YrText(
            greeting, if (tight) 13f else 15f, s, YearReviewNight.text, weight = FontWeight.Bold, maxLines = if (tight) 1 else 2, autoShrink = tight,
            modifier = Modifier.yrReveal(step >= 1, (10 * s).dp),
        )
        Spacer(Modifier.height(((if (tight) 6 else 10) * s).dp))
        YearReviewNightWindow(s = s, title = LocalContext.current.getString(R.string.yr_summary_window), padding = if (tight) 12f else 16f) {
            Column(verticalArrangement = Arrangement.spacedBy(((if (tight) 4 else 7) * s).dp)) {
                rows.forEachIndexed { index, row ->
                    Row(
                        Modifier.yrReveal(step >= 2 + index, 0.dp, dx = (-20 * s).dp).semantics(mergeDescendants = true) { },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((10 * s).dp),
                    ) {
                        Icon(row.icon, null, tint = row.tint, modifier = Modifier.size(((if (tight) 13 else 15) * s).dp))
                        YrText(row.label, if (tight) 13f else 15f, s, YearReviewNight.textSoft, weight = FontWeight.Medium, maxLines = 1, autoShrink = true, align = TextAlign.Start)
                        Spacer(Modifier.width((8 * s).dp))
                        YrText(
                            row.value, if (tight) 14f else 16f, s, YearReviewNight.text, weight = FontWeight.Bold, maxLines = if (tight) 1 else 2,
                            autoShrink = tight, align = TextAlign.End, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(((if (tight) 8 else 12) * s).dp))
        val shape = RoundedCornerShape((NnoworkRadius.field.value * s).dp)
        Row(
            Modifier
                .yrStampIn(step >= 2 + rows.size, 0.7f)
                .fillMaxWidth()
                .background(YearReviewNight.bannerFill, shape)
                .border(1.5.dp, YearReviewNight.gold.copy(alpha = 0.7f), shape)
                .padding(horizontal = (16 * s).dp, vertical = ((if (tight) 7 else 10) * s).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((8 * s).dp, Alignment.CenterHorizontally),
        ) {
            Icon(Icons.Filled.WorkspacePremium, null, tint = YearReviewNight.gold, modifier = Modifier.size(((if (tight) 14 else 16) * s).dp))
            YrText(title, if (tight) 14f else 16f, s, YearReviewNight.title, weight = FontWeight.ExtraBold, maxLines = if (tight) 1 else 2, autoShrink = true)
        }
    }
}

/** iOS ViewThatFits(in: .vertical)：一般版放得下就用，否則用緊湊版；置中。 */
@Composable
private fun FitsVertically(modifier: Modifier, normal: @Composable () -> Unit, tight: @Composable () -> Unit) {
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val probe = subcompose("normal", normal).map { it.measure(loose.copy(maxHeight = Constraints.Infinity)) }
        val fits = probe.sumOf { it.height } <= constraints.maxHeight
        val placeables = if (fits) probe else subcompose("tight", tight).map { it.measure(loose) }
        val height = placeables.sumOf { it.height }
        layout(constraints.maxWidth, constraints.maxHeight) {
            var y = ((constraints.maxHeight - height) / 2).coerceAtLeast(0)
            placeables.forEach {
                it.placeRelative((constraints.maxWidth - it.width) / 2, y)
                y += it.height
            }
        }
    }
}
