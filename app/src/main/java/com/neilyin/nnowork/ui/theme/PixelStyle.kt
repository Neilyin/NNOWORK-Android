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
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme

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

/** 方角邊框（像素風格不用圓角）。 */
fun Modifier.pixelBorder(
    color: Color,
    width: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(NnoworkRadius.control),
): Modifier =
    this.border(width, color, shape)

/** 像素卡片：底色 + 方角邊框。 */
@Composable
fun Modifier.pixelCard(
    backgroundColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    borderWidth: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(NnoworkRadius.card),
    elevation: Dp = NnoworkElevation.card,
): Modifier {
    val resolvedBackground = backgroundColor.takeOrElse { MaterialTheme.colorScheme.surfaceVariant }
    val resolvedBorder = borderColor.takeOrElse { MaterialTheme.colorScheme.outline }
    return this
        .shadow(elevation, shape, clip = false)
        .clip(shape)
        .background(resolvedBackground, shape)
        .border(borderWidth, resolvedBorder, shape)
}

/** 集點卡格子。 */
fun Modifier.pixelCell(
    isStamped: Boolean,
    stampedColor: Color = PixelColors.yellow,
    emptyColor: Color = PixelColors.white,
    borderColor: Color = PixelColors.black,
    cornerRadius: Dp = 0.dp,
): Modifier {
    val shape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RectangleShape
    return this
        .clip(shape)
        .background(if (isStamped) stampedColor else emptyColor)
        .border(2.dp, borderColor, shape)
}

/**
 * 像素徽章：對應 iOS pixelBadge。
 */
@Composable
fun PixelBadge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelColors.red,
    borderColor: Color = Color.Unspecified,
    content: @Composable () -> Unit,
) {
    val resolvedBorder = borderColor.takeOrElse { MaterialTheme.colorScheme.outline }
    val shape = RoundedCornerShape(NnoworkRadius.compact)
    Box(
        modifier = modifier
            .shadow(2.dp, shape, clip = false)
            .clip(shape)
            .background(backgroundColor, shape)
            .border(2.dp, resolvedBorder, shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
    backgroundColor: Color = PixelColors.blue,
    borderColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
    shape: Shape = RoundedCornerShape(NnoworkRadius.control),
    content: @Composable RowScope.() -> Unit,
) {
    val resolvedBorder = borderColor.takeOrElse { MaterialTheme.colorScheme.outline }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(NnoworkMotion.fast),
        label = "pixelButtonScale",
    )

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = NnoworkSize.minTouchTarget)
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
            .background(backgroundColor, shape)
            .border(2.dp, resolvedBorder, shape)
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

// 便利：常用字級
val pixel12 = 12.sp
val pixel14 = 14.sp
val pixel16 = 16.sp
val pixel18 = 18.sp
val pixel20 = 20.sp
val pixel22 = 22.sp
val pixel24 = 24.sp
