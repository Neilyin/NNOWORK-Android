package com.neilyin.nnowork.feature.yearreview

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.colorHex
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** 版面基準（分享圖 360×640dp，density 3 輸出 1080×1920）。 */
object YearReviewLayout {
    const val BASE_WIDTH = 360f
    const val BASE_HEIGHT = 640f
    const val ASPECT = 9f / 16f
    const val TOP_INSET = 64f
    const val BOTTOM_INSET = 84f
}

/**
 * 「像素星空」風格（§22.10）：開場、年度稱號、總結卡固定深色，任何主題下都一樣。
 * 主要色票沿用 [PixelColors] 的夜空色；其餘裝飾色照規格。
 */
object YearReviewNight {
    val skyTop = PixelColors.nightSkyTop
    val skyBottom = PixelColors.nightSkyBottom
    val gold = PixelColors.nightGold
    val amber = PixelColors.nightAmber
    val beige = colorHex("#E2C39B")
    val greyBrown = colorHex("#524B46")
    val teal = colorHex("#458B74")
    val pink = colorHex("#7D586A")
    val starWhite = PixelColors.nightStarWhite
    val text = starWhite
    val textSoft = starWhite.copy(alpha = 0.72f)
    val title = gold
    val onGold = skyTop
    val windowFill = PixelColors.nightSurface
    val windowBorder = starWhite
    val bannerFill = PixelColors.nightBanner
    val iconGold = gold
    val iconAmber = amber
    val iconTeal = colorHex("#7CCBB0")
    val iconPink = colorHex("#E8A6C2")
    val iconBlue = colorHex("#8FC4F2")
    val iconCoral = PixelColors.nightCoral
}

/** 中間各頁跟著目前的卡片主題（迷你集點卡用）。 */
val LocalYearReviewCardTheme = staticCompositionLocalOf { CardTheme.RPG }

/** 減少動態效果：開發者選項／無障礙「移除動畫」。 */
@Composable
fun rememberYearReviewReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }
}

/** TalkBack（觸控探索）開著。 */
@Composable
fun rememberTalkBackEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    var enabled by remember { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}

/** 震動：蓋章（full／quick）、輕微 tick、成功。分享圖輸出時不呼叫。 */
class YearReviewHaptics(private val view: View) {
    fun stamp(full: Boolean = true) {
        view.performHapticFeedback(if (full) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun success() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
        )
    }
}

@Composable
fun rememberYearReviewHaptics(): YearReviewHaptics {
    val view = LocalView.current
    return remember(view) { YearReviewHaptics(view) }
}

/**
 * 依序推進登場步驟：[delays] 是每一步距離上一步的毫秒數。不播（減少動態效果、分享圖）時直接是 99（全部顯示）。
 */
@Composable
fun rememberYearReviewSteps(plays: Boolean, delays: List<Long>, onStep: (Int) -> Unit = {}): Int {
    var step by remember { mutableIntStateOf(if (plays) 0 else 99) }
    LaunchedEffect(Unit) {
        if (!plays) return@LaunchedEffect
        delays.forEachIndexed { index, delay ->
            kotlinx.coroutines.delay(delay)
            step = index + 1
            onStep(index + 1)
        }
    }
    return step
}

/** 數字從 0 往上跳（ease-out cubic）。 */
suspend fun yearReviewCountUp(target: Int, durationSeconds: Double, update: (Int) -> Unit) {
    if (target <= 0) {
        update(0)
        return
    }
    val start = System.nanoTime()
    while (true) {
        val t = ((System.nanoTime() - start) / 1e9 / durationSeconds).coerceAtMost(1.0)
        val eased = 1 - (1 - t) * (1 - t) * (1 - t)
        update(minOf(target, (target * eased).toInt()))
        if (t >= 1.0) break
        kotlinx.coroutines.delay(30)
    }
    update(target)
}

/** 逐步登場：還沒輪到時透明並往下偏移。 */
@Composable
fun Modifier.yrReveal(shown: Boolean, offset: Dp, dx: Dp = 0.dp): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 200f),
        label = "yrReveal",
    )
    return this.graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        translationY = (1 - progress) * offset.toPx()
        translationX = (1 - progress) * dx.toPx()
    }
}

/** 「蓋章式」彈一下：trigger 改變時 scale 先放大再彈回。 */
@Composable
fun Modifier.yrPunch(trigger: Int, strength: Float = 1f): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        scale.animateTo(1f + 0.25f * strength, tween(80))
        scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** 從 [startScale] 縮放淡入（scale 1.25→1 蓋下）。 */
@Composable
fun Modifier.yrStampIn(shown: Boolean, startScale: Float, dx: Dp = 0.dp): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 260f),
        label = "yrStampIn",
    )
    return this.graphicsLayer {
        alpha = progress.coerceIn(0f, 1f)
        val s = startScale + (1 - startScale) * progress
        scaleX = s
        scaleY = s
        translationX = (1 - progress) * dx.toPx()
    }
}

/** 持續累加的動畫秒數（暫停時停住，從背景回來最多跳 0.1 秒）。 */
@Composable
fun rememberYearReviewClock(running: Boolean): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            time.floatValue += ((now - last) / 1e9f).coerceAtMost(0.1f)
            last = now
        }
    }
    return time
}

// MARK: 主題頁共用元件

data class YearReviewTextStyle(val size: Float, val weight: FontWeight, val color: Color)

@Composable
fun YrText(
    text: String,
    size: Float,
    s: Float,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Center,
    maxLines: Int = Int.MAX_VALUE,
    lineSpacing: Float = 0f,
    autoShrink: Boolean = false,
) {
    val base = (size * s).sp
    var fontSize by remember(text, size, s) { mutableStateOf(base) }
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = weight,
            color = color,
            textAlign = align,
            lineHeight = if (lineSpacing > 0f) ((size + lineSpacing) * 1.2f * s).sp else androidx.compose.ui.unit.TextUnit.Unspecified,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = maxLines != 1 || !autoShrink,
        onTextLayout = { layout ->
            // minimumScaleFactor：放不下就縮小（最多縮到 0.5）
            if (autoShrink && (layout.hasVisualOverflow || layout.didOverflowWidth) && fontSize.value > base.value * 0.5f) {
                fontSize = (fontSize.value * 0.92f).sp
            }
        },
    )
}

@Composable
fun YrHeader(text: String, s: Float, modifier: Modifier = Modifier) {
    YrText(
        text, 22f, s, NnoworkTheme.colors.primaryText,
        modifier = modifier.semantics { heading() },
        weight = FontWeight.Bold,
    )
}

@Composable
fun YrFlavor(text: String, s: Float, modifier: Modifier = Modifier) {
    YrText(text, 15f, s, NnoworkTheme.colors.secondaryText, modifier = modifier, weight = FontWeight.Medium, lineSpacing = 3f)
}

/** 主題頁的卡片底：surface＋細框＋硬陰影（往下 4）。 */
@Composable
fun Modifier.yrPanel(s: Float): Modifier {
    val colors = NnoworkTheme.colors
    val radius = (NnoworkRadius.card.value * s).dp
    val shape = RoundedCornerShape(radius)
    val light = colors.appBG.luminance() > 0.5f
    val shadow = Color.Black.copy(alpha = if (light) 0.08f else 0.3f)
    return this
        .drawBehind {
            drawRoundRect(shadow, topLeft = Offset(0f, 4 * s * density), size = size, cornerRadius = CornerRadius(radius.toPx()))
        }
        .background(colors.surface, shape)
        .border(1.dp, colors.cardBorder.copy(alpha = 0.6f), shape)
}

/** 依字元陣列畫像素圖：每個字元一格，「.」透明。 */
@Composable
fun YrPixelSprite(rows: List<String>, palette: Map<Char, Color>, unit: Dp, modifier: Modifier = Modifier) {
    val columns = rows.maxOf { it.length }
    Canvas(modifier.size(unit * columns, unit * rows.size)) {
        val u = unit.toPx()
        rows.forEachIndexed { r, line ->
            line.forEachIndexed { c, ch ->
                val color = palette[ch] ?: return@forEachIndexed
                drawRect(color, topLeft = Offset(c * u, r * u), size = Size(u + 0.5f, u + 0.5f))
            }
        }
    }
}

/** 5×5 像素十字星。 */
fun DrawScope.drawPixelPlus(center: Offset, unit: Float, color: Color) {
    val arm = unit * 2.5f
    drawRect(color, Offset(center.x - arm, center.y - unit / 2), Size(arm * 2, unit))
    drawRect(color, Offset(center.x - unit / 2, center.y - arm), Size(unit, arm - unit / 2))
    drawRect(color, Offset(center.x - unit / 2, center.y + unit / 2), Size(unit, arm - unit / 2))
}

// MARK: 背景

/** 主題背景：主題底色＋一圈主題色光暈（位置跟著每頁主角走）＋淡淡的像素點陣。 */
@Composable
fun YearReviewThemeBackground(page: YearReviewPage, modifier: Modifier = Modifier) {
    val colors = NnoworkTheme.colors
    val light = colors.appBG.luminance() > 0.5f
    val glow = when (page) {
        YearReviewPage.ORIGIN -> Offset(0.2f, 0.3f)
        YearReviewPage.REASONS -> Offset(0.5f, 0.38f)
        YearReviewPage.TIME_OF_DAY -> Offset(0.75f, 0.25f)
        YearReviewPage.WEEKDAY -> Offset(0.25f, 0.3f)
        YearReviewPage.STREAK -> Offset(0.5f, 0.3f)
        YearReviewPage.CARDS -> Offset(0.7f, 0.4f)
        YearReviewPage.SAVINGS -> Offset(0.5f, 0.45f)
        YearReviewPage.TREE_HOLE -> Offset(0.3f, 0.35f)
        YearReviewPage.COINS -> Offset(0.5f, 0.3f)
        else -> Offset(0.5f, 0.33f)
    }
    val dot = colors.primaryText.copy(alpha = if (light) 0.07f else 0.06f)
    val accent = colors.accent.copy(alpha = if (light) 0.18f else 0.26f)
    Canvas(modifier.fillMaxSize().clearAndSetSemantics { }) {
        drawRect(colors.appBG)
        val center = Offset(glow.x * size.width, glow.y * size.height)
        val radius = 380.dp.toPx() * (size.width / 360.dp.toPx()).coerceAtLeast(0.6f)
        drawRect(Brush.radialGradient(listOf(accent, Color.Transparent), center = center, radius = radius))
        val step = 18.dp.toPx()
        val dotSize = 2.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawRect(dot, Offset(x, y), Size(dotSize, dotSize))
                x += step
            }
            y += step
        }
    }
}

private class NightArc(val x: Float, val y: Float, val radius: Float, val start: Float, val sweep: Float, val width: Float, val warm: Boolean, val opacity: Float)
private class NightBlock(val arc: Int, val angle: Float, val size: Float, val color: Color, val opacity: Float, val tilt: Float, val phase: Float, val outlined: Boolean = false)
private class NightParticle(val x: Float, val y: Float, val size: Float, val colorIndex: Int, val phase: Float, val speed: Float, val twinkles: Boolean)
private class NightConfetti(val x: Float, val size: Float, val speed: Float, val offset: Float, val sway: Float, val colorIndex: Int)

private object NightScene {
    val orbs = listOf(
        floatArrayOf(0.14f, 0.20f, 0.48f, 0.12f, 0f) to YearReviewNight.amber,
        floatArrayOf(0.92f, 0.58f, 0.52f, 0.08f, 1.8f) to YearReviewNight.gold,
        floatArrayOf(0.30f, 0.96f, 0.45f, 0.09f, 3.1f) to YearReviewNight.amber,
    )
    val arcs = listOf(
        NightArc(1.22f, 0.24f, 0.45f, 120f, 130f, 1.5f, false, 0.14f),
        NightArc(-0.22f, 0.60f, 0.48f, -70f, 140f, 2f, true, 0.32f),
        NightArc(0.50f, 0.40f, 0.98f, 20f, 140f, 1f, false, 0.09f),
        NightArc(1.18f, 0.98f, 0.50f, 190f, 80f, 1.5f, true, 0.26f),
        NightArc(-0.10f, 0.06f, 0.40f, -10f, 110f, 1.5f, false, 0.11f),
    )
    val blocks = listOf(
        NightBlock(0, 140f, 18f, YearReviewNight.beige, 0.75f, 18f, 0.4f),
        NightBlock(1, -48f, 22f, YearReviewNight.greyBrown, 0.95f, -12f, 1.3f),
        NightBlock(1, 52f, 12f, YearReviewNight.gold, 0.6f, 30f, 2.2f),
        NightBlock(2, 116f, 24f, YearReviewNight.beige, 0.55f, -20f, 3.0f),
        NightBlock(2, 62f, 16f, YearReviewNight.greyBrown, 0.95f, 10f, 4.1f),
        NightBlock(3, 246f, 14f, YearReviewNight.gold, 0.7f, 40f, 5.0f, outlined = true),
        NightBlock(4, 80f, 16f, YearReviewNight.beige, 0.65f, -28f, 0.9f),
    )
    val particleColors = listOf(YearReviewNight.starWhite, YearReviewNight.teal, YearReviewNight.pink, YearReviewNight.gold)
    val particles: List<NightParticle> = Random(20261231).let { rng ->
        val weighted = listOf(0, 0, 0, 0, 0, 1, 1, 2, 2, 3)
        List(56) {
            NightParticle(
                rng.nextFloat(), rng.nextFloat(), listOf(2f, 2f, 3f, 3f, 4f)[rng.nextInt(5)],
                weighted[rng.nextInt(weighted.size)], rng.nextFloat() * 2 * PI.toFloat(),
                0.8f + rng.nextFloat() * 1.4f, rng.nextFloat() < 0.65f,
            )
        }
    }
    val sparkles = listOf(
        floatArrayOf(0.10f, 0.15f, 2.5f, 0f) to YearReviewNight.gold,
        floatArrayOf(0.90f, 0.36f, 2f, 1.6f) to YearReviewNight.starWhite,
        floatArrayOf(0.07f, 0.68f, 2f, 2.9f) to YearReviewNight.gold,
        floatArrayOf(0.93f, 0.82f, 2.5f, 4.2f) to YearReviewNight.gold,
    )
    val confettiColors = listOf(
        YearReviewNight.gold, YearReviewNight.amber, YearReviewNight.beige,
        YearReviewNight.teal, YearReviewNight.pink, YearReviewNight.starWhite,
    )
    val confetti: List<NightConfetti> = Random(2027).let { rng ->
        List(26) {
            NightConfetti(
                rng.nextFloat(), listOf(4f, 6f, 6f, 8f)[rng.nextInt(4)], 0.06f + rng.nextFloat() * 0.06f,
                rng.nextFloat(), rng.nextFloat() * 2 * PI.toFloat(), rng.nextInt(6),
            )
        }
    }

    fun point(cx: Float, cy: Float, r: Float, degrees: Float): Offset {
        val rad = degrees * PI.toFloat() / 180f
        return Offset(cx + r * cos(rad), cy + r * sin(rad))
    }

    /** live＝動畫中；false 時畫固定的一幀（分享圖、減少動態效果）。 */
    fun DrawScope.render(t: Float, live: Boolean, withConfetti: Boolean, dpPx: Float) {
        val w = size.width
        val h = size.height
        val u = w / (YearReviewLayout.BASE_WIDTH * dpPx) * dpPx
        drawRect(Brush.verticalGradient(listOf(YearReviewNight.skyTop, YearReviewNight.skyBottom)))

        orbs.forEach { (o, color) ->
            val c = Offset(o[0] * w, o[1] * h)
            val r = o[2] * w
            val pulse = if (live) 0.85f + 0.15f * sin(t * 0.5f + o[4]) else 1f
            drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = o[3] * pulse), color.copy(alpha = 0f)), center = c, radius = r),
                radius = r, center = c,
            )
        }

        val drifts = arcs.mapIndexed { index, _ -> if (live) 4f * sin(t * 0.12f + index * 1.7f) else 0f }
        arcs.forEachIndexed { index, arc ->
            val drift = drifts[index]
            val cx = arc.x * w
            val cy = arc.y * h
            val r = arc.radius * w
            val path = Path()
            for (step in 0..48) {
                val p = point(cx, cy, r, arc.start + drift + arc.sweep * step / 48f)
                if (step == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            val from = point(cx, cy, r, arc.start + drift)
            val to = point(cx, cy, r, arc.start + arc.sweep + drift)
            val stops = if (arc.warm) {
                listOf(YearReviewNight.gold.copy(alpha = 0f), YearReviewNight.gold.copy(alpha = arc.opacity),
                    YearReviewNight.amber.copy(alpha = arc.opacity * 0.7f), YearReviewNight.amber.copy(alpha = 0f))
            } else {
                listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = arc.opacity), Color.White.copy(alpha = arc.opacity), Color.White.copy(alpha = 0f))
            }
            drawPath(path, Brush.linearGradient(stops, start = from, end = to), style = Stroke(width = arc.width * u, cap = StrokeCap.Round))
        }

        blocks.forEach { block ->
            val arc = arcs[block.arc]
            val wobble = if (live) 5f * sin(t * 0.22f + block.phase) else 0f
            val p = point(arc.x * w, arc.y * h, arc.radius * w, block.angle + drifts[block.arc] + wobble)
            val tilt = block.tilt + if (live) 14f * sin(t * 0.3f + block.phase) else 0f
            val side = block.size * u
            translate(p.x, p.y) {
                rotate(tilt, pivot = Offset.Zero) {
                    val color = block.color.copy(alpha = block.opacity)
                    if (block.outlined) {
                        drawRoundRect(color, Offset(-side / 2, -side / 2), Size(side, side), CornerRadius(side * 0.28f), style = Stroke(1.5f * u))
                    } else {
                        drawRoundRect(color, Offset(-side / 2, -side / 2), Size(side, side), CornerRadius(side * 0.28f))
                    }
                }
            }
        }

        particles.forEach { particle ->
            val alpha = if (live && particle.twinkles) 0.25f + 0.75f * (0.5f + 0.5f * sin(t * particle.speed + particle.phase)) else 0.75f
            val side = maxOf(1f, kotlin.math.round(particle.size * u))
            val x = kotlin.math.round(particle.x * w)
            val y = kotlin.math.round(particle.y * h)
            val color = particleColors[particle.colorIndex]
            if (particle.colorIndex == 1 || particle.colorIndex == 2) {
                drawRect(color.copy(alpha = 0.25f * alpha), Offset(x - side * 0.75f, y - side * 0.75f), Size(side * 2.5f, side * 2.5f))
            }
            drawRect(color.copy(alpha = alpha), Offset(x, y), Size(side, side))
        }

        sparkles.forEach { (sp, color) ->
            val k = if (live) 0.55f + 0.45f * (0.5f + 0.5f * sin(t * 1.4f + sp[3])) else 1f
            drawPixelPlus(Offset(sp[0] * w, sp[1] * h), sp[2] * u * k, color.copy(alpha = 0.4f + 0.6f * k))
        }

        if (withConfetti) {
            confetti.forEach { piece ->
                val progress = (((if (live) t * piece.speed else 0f) + piece.offset) % 1f)
                val y = -20 * u + progress * (h + 40 * u)
                val x = piece.x * w + sin((if (live) t * 1.4f else 0f) + piece.sway) * 12 * u
                val side = kotlin.math.round(piece.size * u)
                drawRect(confettiColors[piece.colorIndex].copy(alpha = 0.8f), Offset(x, y), Size(side, side))
            }
        }
    }
}

/** 像素星空背景（稱號頁另有彩帶）。暫停時停住；不播時直接畫靜態的一幀。 */
@Composable
fun YearReviewNightBackground(page: YearReviewPage, animated: Boolean, paused: Boolean, modifier: Modifier = Modifier) {
    val clock = rememberYearReviewClock(running = animated && !paused)
    val confetti = page == YearReviewPage.TITLE
    Canvas(modifier.fillMaxSize().clearAndSetSemantics { }) {
        with(NightScene) { render(if (animated) clock.value else 0f, animated, confetti, density) }
    }
}

@Composable
fun YearReviewPageBackground(page: YearReviewPage, animated: Boolean, paused: Boolean, modifier: Modifier = Modifier) {
    if (page.isNight) {
        YearReviewNightBackground(page, animated, paused, modifier)
    } else {
        YearReviewThemeBackground(page, modifier)
    }
}

// MARK: 星空 RPG 視窗

/** 不透明深藍底、淺色雙框（外粗內細）的 RPG 視窗，標題金色；陰影只給外框。 */
@Composable
fun YearReviewNightWindow(
    s: Float,
    modifier: Modifier = Modifier,
    title: String? = null,
    padding: Float = 18f,
    content: @Composable () -> Unit,
) {
    val radius = NnoworkRadius.field
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(0f, 4.dp.toPx()), size = size, cornerRadius = CornerRadius(radius.toPx()))
            }
            .background(YearReviewNight.windowFill, shape)
            .border(3.dp, YearReviewNight.windowBorder.copy(alpha = 0.9f), shape)
            .drawBehind {
                val inset = 5.dp.toPx()
                drawRoundRect(
                    YearReviewNight.windowBorder.copy(alpha = 0.25f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius((radius - 4.dp).toPx()),
                    style = Stroke(1.dp.toPx()),
                )
            }
            .padding((padding * s).dp),
        verticalArrangement = Arrangement.spacedBy(((if (padding < 18f) 8f else 12f) * s).dp),
    ) {
        if (title != null) {
            Text(
                title,
                style = TextStyle(fontSize = (13 * s).sp, fontWeight = FontWeight.ExtraBold, color = YearReviewNight.title, letterSpacing = 1.sp),
            )
        }
        content()
    }
}

// MARK: 草地上的小牛

private val COW = listOf(
    ".YY........YY.",
    "..YKKKKKKKKY..",
    ".KWWWWWWBBBWK.",
    "KWWWWWWWWBBWWK",
    "KWWKKWWWWKKWWK",
    "KWWKKWWWWKKWWK",
    "KWRRWWWWWWRRWK",
    "KWPPPPPPPPPPWK",
    "KPPKPPPPPPKPPK",
    "KPPPPPPPPPPPPK",
    ".KPPPPPPPPPPK.",
    "..KKKKKKKKKK..",
)
private val COW_PALETTE = mapOf(
    'K' to Color(0.16f, 0.14f, 0.18f),
    'W' to Color.White,
    'B' to Color(0.16f, 0.14f, 0.18f),
    'Y' to Color(1.0f, 0.90f, 0.62f),
    'R' to Color(1.0f, 0.62f, 0.70f),
    'P' to Color(1.0f, 0.76f, 0.80f),
)
private val CROWN = listOf(
    "Y...Y...Y",
    "YY.YYY.YY",
    "YYYYYYYYY",
    "YRYYBYYRY",
    "YYYYYYYYY",
    "OOOOOOOOO",
)
private val CROWN_PALETTE = mapOf(
    'Y' to YearReviewNight.gold,
    'O' to Color(0.78f, 0.55f, 0.10f),
    'R' to PixelColors.red,
    'B' to PixelColors.blue,
)

/** 一個跳躍週期（iOS HoppingCow keyframes）在 t 秒時的 (y, scaleX, scaleY)。 */
private fun hopFrame(t: Float): Triple<Float, Float, Float> {
    val period = 1.23f
    val p = t % period
    fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f.coerceIn(0f, 1f)
    fun easeOut(f: Float) = 1 - (1 - f) * (1 - f)
    fun easeIn(f: Float) = f * f
    val y = when {
        p < 0.18f -> 0f
        p < 0.50f -> lerp(0f, -46f, easeOut((p - 0.18f) / 0.32f))
        p < 0.78f -> lerp(-46f, 0f, easeIn((p - 0.50f) / 0.28f))
        else -> 0f
    }
    fun track(values: FloatArray, times: FloatArray): Float {
        var start = 0f
        var from = 1f
        values.forEachIndexed { index, value ->
            val end = start + times[index]
            if (p < end) return lerp(from, value, (p - start) / times[index])
            start = end
            from = value
        }
        return 1f
    }
    val times = floatArrayOf(0.18f, 0.2f, 0.4f, 0.08f, 0.37f)
    val sy = track(floatArrayOf(0.82f, 1.12f, 1.0f, 0.88f, 1.0f), times)
    val sx = track(floatArrayOf(1.14f, 0.92f, 1.0f, 1.1f, 1.0f), times)
    return Triple(y, sx, sy)
}

/** 草地上的像素地面。 */
@Composable
private fun YrPixelGround(modifier: Modifier) {
    Canvas(modifier) {
        val unit = 6.dp.toPx()
        val grass = Color(0.36f, 0.72f, 0.36f)
        val grassDark = Color(0.24f, 0.55f, 0.28f)
        val dirt = Color(0.45f, 0.30f, 0.20f)
        drawRect(dirt, Offset(0f, unit * 2), Size(size.width, size.height - unit * 2))
        var x = 0f
        var i = 0
        while (x < size.width) {
            drawRect(if (i % 2 == 0) grass else grassDark, Offset(x, unit), Size(unit, unit * 1.5f))
            if (i % 3 == 0) drawRect(grass, Offset(x, 0f), Size(unit, unit))
            x += unit
            i++
        }
    }
}

/**
 * 站在草地上的小牛＋身後暖光＋金色十字星。開場的小牛會跳；稱號頁戴皇冠站好（皇冠約 0.85 秒落下）。
 */
@Composable
fun YearReviewNightCow(
    s: Float,
    animated: Boolean,
    paused: Boolean,
    modifier: Modifier = Modifier,
    crowned: Boolean = false,
    cowScale: Float = 0.8f,
) {
    val k = cowScale * s
    val px = 6f * k
    val groundHeight = 20f * s
    val footLift = groundHeight - 6f
    val headroom = if (crowned) 5 * px else 36f * k
    val width = 220f * s
    val height = headroom + 72f * k + footLift
    val clock = rememberYearReviewClock(running = animated && !paused)
    val crownProgress = remember { Animatable(if (crowned && animated) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (crowned && animated) {
            kotlinx.coroutines.delay(600)
            crownProgress.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 200f))
        }
    }
    Box(
        modifier
            .size(width.dp, height.dp)
            .clearAndSetSemantics { }
            .drawBehind {
                // 身後的暖色光暈：中心對齊小牛臉、壓扁成橢圓
                val r = 100f * s * density
                val cy = size.height - (footLift + 36f * k) * density
                val c = Offset(size.width / 2, cy)
                translate(0f, 0f) {
                    drawOval(
                        Brush.radialGradient(
                            listOf(YearReviewNight.gold.copy(alpha = 0.30f), YearReviewNight.amber.copy(alpha = 0.12f), YearReviewNight.amber.copy(alpha = 0f)),
                            center = c, radius = r,
                        ),
                        topLeft = Offset(c.x - r, c.y - r * 0.7f),
                        size = Size(r * 2, r * 1.4f),
                    )
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        YrPixelGround(Modifier.size((172f * s).dp, groundHeight.dp))
        Column(
            modifier = Modifier.padding(bottom = footLift.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-px).dp),
        ) {
            if (crowned) {
                YrPixelSprite(
                    CROWN, CROWN_PALETTE, px.dp,
                    Modifier.graphicsLayer {
                        val p = crownProgress.value
                        alpha = p.coerceIn(0f, 1f)
                        translationY = (1 - p) * -60f * s * density
                    },
                )
            }
            val hops = animated && !crowned
            YrPixelSprite(
                COW, COW_PALETTE, px.dp,
                Modifier.graphicsLayer {
                    if (hops) {
                        val (y, sx, sy) = hopFrame(clock.value)
                        translationY = y * k * density
                        scaleX = sx
                        scaleY = sy
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    }
                },
            )
        }
        // 四周的十字星
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            listOf(
                floatArrayOf(0.12f, 0.30f, 3f, 0f) to YearReviewNight.gold,
                floatArrayOf(0.88f, 0.18f, 2.5f, 1.2f) to YearReviewNight.gold,
                floatArrayOf(0.83f, 0.62f, 2f, 2.3f) to YearReviewNight.starWhite,
                floatArrayOf(0.20f, 0.68f, 2f, 3.4f) to YearReviewNight.amber,
            ).forEach { (spot, color) ->
                val kk = if (animated) 0.5f + 0.5f * (0.5f + 0.5f * sin(t * 2.2f + spot[3])) else 1f
                drawPixelPlus(Offset(spot[0] * size.width, spot[1] * size.height), spot[2] * s * density * kk, color.copy(alpha = 0.35f + 0.65f * kk))
            }
        }
    }
}

/** 一閃一閃的游標（開場操作提示）。 */
@Composable
fun YrBlinkingCursor(text: String, s: Float, color: Color, animated: Boolean) {
    val clock = rememberYearReviewClock(running = animated)
    Text(
        text,
        modifier = Modifier.graphicsLayer { alpha = if (!animated || (clock.value % 1f) < 0.55f) 1f else 0.15f },
        style = TextStyle(fontSize = (12 * s).sp, color = color),
    )
}

/** 載入面板的假進度（t 秒）→ (進度, 階段)。 */
fun yearReviewFakeProgress(elapsedSeconds: Double): Pair<Double, Int> {
    val t = maxOf(0.0, elapsedSeconds)
    return when {
        t < 1.5 -> 0.40 * t / 1.5 to 0
        t < 4.0 -> 0.40 + 0.35 * (t - 1.5) / 2.5 to 1
        else -> 0.75 + 0.20 * (1 - exp(-(t - 4.0) / 3.0)) to 2
    }
}
