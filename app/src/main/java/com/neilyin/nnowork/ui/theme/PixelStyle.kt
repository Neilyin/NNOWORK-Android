package com.neilyin.nnowork.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * 像素／等寬字體（對應 iOS .monospaced design）。
 */
fun pixelTextStyle(
    fontSize: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = fontSize,
    fontWeight = weight,
    color = color,
)

/** 像素邊框。預設線寬／圓角來自 NnoworkLine／NnoworkRadius。 */
fun Modifier.pixelBorder(
    color: Color,
    width: Dp = NnoworkLine.control,
    shape: Shape = RoundedCornerShape(NnoworkRadius.field),
): Modifier =
    this.border(width, color, shape)

/** 像素卡片：底色 + 方角邊框。 */
@Composable
fun Modifier.pixelCard(
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = NnoworkLine.control,
    shape: Shape = RoundedCornerShape(NnoworkRadius.card),
    elevation: Dp = NnoworkElevation.card,
): Modifier {
    val resolvedBackground = backgroundColor.takeOrElse { NnoworkTheme.colors.surface }
    val resolvedBorder = borderColor.takeOrElse { NnoworkTheme.colors.cardBorder }
    return this
        .shadow(elevation, shape, clip = false)
        .clip(shape)
        .background(resolvedBackground, shape)
        .border(borderWidth, resolvedBorder, shape)
}

/** 集點卡格子。預設色為集點格刻意使用的原色（highlight／elevatedSurface／border）。 */
@Composable
fun Modifier.pixelCell(
    isStamped: Boolean,
    stampedColor: Color = NnoworkTheme.colors.highlight,
    emptyColor: Color = NnoworkTheme.colors.elevatedSurface,
    borderColor: Color = NnoworkTheme.colors.border,
    cornerRadius: Dp = 0.dp,
): Modifier {
    val shape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RectangleShape
    return this
        .clip(shape)
        .background(if (isStamped) stampedColor else emptyColor)
        .border(NnoworkLine.control, borderColor, shape)
}

/**
 * 像素徽章：對應 iOS pixelBadge。
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
    Box(
        modifier = modifier
            .shadow(2.dp, shape, clip = false)
            .clip(shape)
            .background(resolvedBackground, shape)
            .border(NnoworkLine.control, resolvedBorder, shape)
            .padding(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.xxs),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * 像素按鈕：對應 iOS PixelButtonStyle。
 * 按下時往右下位移 2pt 並縮放 0.98。
 */
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = NnoworkSpacing.page, vertical = 13.dp),
    shape: Shape = RoundedCornerShape(NnoworkRadius.button),
    content: @Composable RowScope.() -> Unit,
) {
    val resolvedBackground = backgroundColor.takeOrElse { NnoworkTheme.colors.buttonBG }
    val resolvedBorder = borderColor.takeOrElse { NnoworkTheme.colors.cardBorder }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(NnoworkMotion.fast),
        label = "pixelButtonScale",
    )

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = NnoworkSize.controlHeight)
            // 透明度圖層必須包住完整按鈕；若放在 background 之後，
            // Compose 在 disabled -> enabled 時可能移除圖層卻沒有重畫容器色。
            .alpha(if (enabled) 1f else 0.55f)
            .graphicsLayer {
                val pressedScale = 1f - (0.015f * pressProgress)
                scaleX = pressedScale
                scaleY = pressedScale
                translationX = 2.dp.toPx() * pressProgress
                translationY = 2.dp.toPx() * pressProgress
            }
            .shadow(
                elevation = if (pressed) 1.dp else NnoworkElevation.control,
                shape = shape,
                clip = false,
            )
            .clip(shape)
            .background(resolvedBackground, shape)
            .border(NnoworkLine.control, resolvedBorder, shape)
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
