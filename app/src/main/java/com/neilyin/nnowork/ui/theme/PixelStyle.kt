package com.neilyin.nnowork.ui.theme

import android.graphics.Paint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * 文字樣式。預設一律系統 Sans（標題、按鈕字也是 Sans 粗體），中英文同一套字形。
 * 等寬只在呼叫點明確指定 [monospace] = true：數字（格數、金幣、計時、統計）、集點格、
 * 徽章與品牌標題（NnoworkPageTitle、首頁「離職集點卡」標題），延續像素品牌但不擴散到整頁。
 */
fun pixelTextStyle(
    fontSize: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    monospace: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
    fontSize = fontSize,
    fontWeight = weight,
    color = color,
)

/** 細框。預設線寬／圓角來自 NnoworkLine／NnoworkRadius。 */
fun Modifier.pixelBorder(
    color: Color,
    width: Dp = NnoworkLine.control,
    shape: Shape = RoundedCornerShape(NnoworkRadius.field),
): Modifier =
    this.border(width, color, shape)

/**
 * 柔和投影（iOS shadow radius 10、y 3）。Compose 1.7 沒有 dropShadow，
 * 用 framework Paint.setShadowLayer 畫在內容後方；API 28 以下硬體加速不支援時會自然退化成無陰影。
 */
fun Modifier.softShadow(
    shape: Shape,
    color: Color,
    radius: Dp = NnoworkShadow.radius,
    offsetY: Dp = NnoworkShadow.offsetY,
): Modifier = if (color.alpha == 0f) this else drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.TRANSPARENT
            setShadowLayer(radius.toPx(), 0f, offsetY.toPx(), color.toArgb())
        }
        // 填色本身透明，只留下陰影。
        paint.color = color.copy(alpha = 0.004f).toArgb()
        when (outline) {
            is Outline.Rounded -> {
                val r = outline.roundRect
                canvas.nativeCanvas.drawRoundRect(
                    r.left, r.top, r.right, r.bottom,
                    r.topLeftCornerRadius.x, r.topLeftCornerRadius.y, paint,
                )
            }
            else -> {
                val b = outline.bounds
                canvas.nativeCanvas.drawRect(b.left, b.top, b.right, b.bottom, paint)
            }
        }
    }
}

/** 依填色亮度決定的陰影色：淺卡極淡、深卡較重、半透明不加。 */
fun cardShadowColor(background: Color): Color = when {
    background.alpha < 0.95f -> Color.Transparent
    background.luminance() > 0.6f -> Color.Black.copy(alpha = NnoworkShadow.lightAlpha)
    else -> Color.Black.copy(alpha = NnoworkShadow.darkAlpha)
}

/** 填色本身夠清楚、不需要外框（不透明且不是接近白的淺色；iOS pixelStandsAlone）。 */
fun Color.standsAlone(): Boolean = alpha > 0.95f && luminance() < 0.6f

/**
 * 卡片：hairline 1dp 外框（45% 透明）＋依底色亮度決定的柔和投影（iOS PixelCardModifier）。
 * [elevation] = 0 時不加陰影；[borderAlpha] 給主視覺（集點卡）用較實的框。
 */
@Composable
fun Modifier.pixelCard(
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = NnoworkLine.hairline,
    shape: Shape = RoundedCornerShape(NnoworkRadius.card),
    elevation: Dp = NnoworkElevation.card,
    borderAlpha: Float = NnoworkCardBorderAlpha,
): Modifier {
    val resolvedBackground = backgroundColor.takeOrElse { NnoworkTheme.colors.surface }
    val resolvedBorder = borderColor.takeOrElse { NnoworkTheme.colors.cardBorder }
    val shadow = if (elevation > 0.dp) cardShadowColor(resolvedBackground) else Color.Transparent
    return this
        .softShadow(shape, shadow)
        .clip(shape)
        .background(resolvedBackground, shape)
        .border(borderWidth, resolvedBorder.copy(alpha = resolvedBorder.alpha * borderAlpha), shape)
}

/** 集點卡格子。主視覺保留像素感：2dp 框、主題圓角（最小 small 6）。 */
@Composable
fun Modifier.pixelCell(
    isStamped: Boolean,
    stampedColor: Color = NnoworkTheme.colors.celebrateYellow,
    emptyColor: Color = NnoworkTheme.colors.elevatedSurface,
    borderColor: Color = NnoworkTheme.colors.cardBorder,
    cornerRadius: Dp = NnoworkRadius.small,
): Modifier {
    val shape = RoundedCornerShape(maxOf(cornerRadius, NnoworkRadius.small))
    return this
        .clip(shape)
        .background(if (isStamped) stampedColor else emptyColor)
        .border(2.dp, borderColor, shape)
}

/** 依底色挑出可讀的內容色（≥ 4.5:1 優先用主題文字色）。 */
@Composable
fun contentColorFor(background: Color): Color {
    val colors = NnoworkTheme.colors
    val solid = if (background.alpha < 1f) background.compositeOver(colors.surface) else background
    return readableOn(solid, colors.primaryText, colors.onAccent, PixelColors.black, Color.White)
}

/**
 * 讓 [content] 裡讀 NnoworkTheme.colors 的文字 token（primaryText／onElevated／secondaryText…）
 * 全部變成 [contentColor]。按鈕、彩色徽章用：呼叫點寫的是「主要文字色」，實際上應該是「這塊底上的字色」。
 */
@Composable
fun ProvideContentColor(contentColor: Color, content: @Composable () -> Unit) {
    val colors = NnoworkTheme.colors
    CompositionLocalProvider(
        LocalNnoworkColors provides colors.copy(
            primaryText = contentColor,
            secondaryText = contentColor.copy(alpha = 0.85f),
            mutedText = contentColor,
            onAppBG = contentColor,
            onSurface = contentColor,
            onElevated = contentColor,
            onStrong = contentColor,
            onAccent = contentColor,
            buttonText = contentColor,
            disabledText = contentColor,
            accentForeground = contentColor,
        ),
        LocalContentColor provides contentColor,
        content = content,
    )
}

/**
 * 徽章：實心不加框、淺色才加細框（iOS PixelBadgeModifier）。內容字色自動依底色決定。
 */
@Composable
fun PixelBadge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    content: @Composable () -> Unit,
) {
    val resolvedBackground = backgroundColor.takeOrElse { NnoworkTheme.colors.danger }
    val resolvedBorder = borderColor.takeOrElse { NnoworkTheme.colors.cardBorder }
    val shape = RoundedCornerShape(NnoworkRadius.small)
    val outlined = !resolvedBackground.standsAlone()
    Box(
        modifier = modifier
            .clip(shape)
            .background(resolvedBackground, shape)
            .then(if (outlined) Modifier.border(NnoworkLine.hairline, resolvedBorder, shape) else Modifier)
            .padding(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.xxs),
        contentAlignment = Alignment.Center,
    ) { ProvideContentColor(contentColorFor(resolvedBackground)) { content() } }
}

/** 按鈕的語意角色。 */
enum class PixelButtonVariant {
    /** 主要動作：accent 實心、onAccent 字、不加框。 */
    Primary,
    /** 次要動作／未選取 chip：tonal（淺色主題 accentSoft、深色主題 elevated）。 */
    Tonal,
    /** 白底或頁面底色的次要鈕：hairline 外框。 */
    Outlined,
    /** 刪除、重置等破壞性動作。 */
    Danger,
    /** 大師等級、慶祝用的紫。 */
    Celebrate,
    /** 呼叫點指定的特殊色（主題品牌色、班別色…）。 */
    Custom,
}

/**
 * 由舊呼叫點傳入的 8-bit 底色推斷按鈕角色：
 * 藍／綠／黃／橘這些原色按鈕一律收斂成主題 accent（8-bit 原色只留給集點格與慶祝畫面）。
 */
@Composable
private fun inferVariant(background: Color): PixelButtonVariant {
    val c = NnoworkTheme.colors
    val scheme = MaterialTheme.colorScheme
    return when (background) {
        Color.Unspecified, c.buttonBG, c.success, c.highlight, c.info, c.accent, c.primary,
        c.warning, c.teal, c.treeHole.questionAccent, scheme.primary -> PixelButtonVariant.Primary
        c.disabledBG -> PixelButtonVariant.Tonal
        c.elevatedSurface, c.surface, c.appBG, scheme.surface, scheme.surfaceVariant,
        scheme.background -> PixelButtonVariant.Outlined
        c.danger, scheme.error -> PixelButtonVariant.Danger
        c.special -> PixelButtonVariant.Celebrate
        else -> PixelButtonVariant.Custom
    }
}

/**
 * 按鈕：對應 iOS reui PixelButtonStyle。
 * 實心鈕不加框；淺色／半透明才加 hairline。按下縮到 0.97 並略降透明度（取代往右下位移）。
 * 內容（Text／Icon）讀 NnoworkTheme.colors 的文字 token 時，會自動拿到這顆按鈕底色上的可讀字色。
 */
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.sm),
    shape: Shape = RoundedCornerShape(NnoworkRadius.button),
    variant: PixelButtonVariant? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = NnoworkTheme.colors
    val role = variant ?: inferVariant(backgroundColor)
    val fill = when (role) {
        PixelButtonVariant.Primary -> colors.buttonBG
        PixelButtonVariant.Tonal -> colors.disabledBG
        PixelButtonVariant.Outlined -> backgroundColor.takeOrElse { colors.elevatedSurface }
        PixelButtonVariant.Danger -> colors.danger
        PixelButtonVariant.Celebrate -> colors.special
        PixelButtonVariant.Custom -> backgroundColor
    }
    val contentColor = when (role) {
        PixelButtonVariant.Primary -> colors.buttonText
        PixelButtonVariant.Tonal, PixelButtonVariant.Outlined -> colors.primaryText
        else -> contentColorFor(fill)
    }
    val outlined = when (role) {
        PixelButtonVariant.Outlined -> true
        PixelButtonVariant.Tonal -> !colors.disabledBG.standsAlone() && colors.disabledBG.luminance() > 0.9f
        PixelButtonVariant.Custom -> !fill.standsAlone()
        else -> false
    }
    val resolvedBorder = borderColor.takeOrElse { colors.border }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "pixelButtonPress",
    )

    ProvideContentColor(contentColor) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = NnoworkSize.controlHeight)
            // 透明度圖層必須包住完整按鈕；若放在 background 之後，
            // Compose 在 disabled -> enabled 時可能移除圖層卻沒有重畫容器色。
            .alpha(if (enabled) 1f else 0.45f)
            .graphicsLayer {
                val s = 1f - 0.03f * pressProgress
                scaleX = s
                scaleY = s
                alpha = 1f - 0.15f * pressProgress
            }
            .clip(shape)
            .background(fill, shape)
            .then(if (outlined) Modifier.border(NnoworkLine.hairline, resolvedBorder, shape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
    }
}
