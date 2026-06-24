package com.neilyin.nnowork.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun Modifier.pixelBorder(color: Color, width: Dp = 3.dp, shape: Shape = RectangleShape): Modifier =
    this.border(width, color, shape)

/** 像素卡片：底色 + 方角邊框。 */
fun Modifier.pixelCard(
    backgroundColor: Color = PixelColors.white,
    borderColor: Color = PixelColors.black,
    borderWidth: Dp = 3.dp,
): Modifier = this
    .background(backgroundColor)
    .border(borderWidth, borderColor, RectangleShape)

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
    borderColor: Color = PixelColors.black,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .border(2.dp, borderColor, RectangleShape)
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
    borderColor: Color = PixelColors.black,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "pixelButtonScale",
    )
    val offsetPx = if (pressed) 2 else 0

    Box(
        modifier = modifier
            .scale(scale)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(IntOffset(offsetPx, offsetPx))
                }
            }
            .background(backgroundColor)
            .border(3.dp, borderColor, RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// 便利：常用字級
val pixel12 = 12.sp
val pixel14 = 14.sp
val pixel16 = 16.sp
val pixel18 = 18.sp
val pixel20 = 20.sp
val pixel22 = 22.sp
val pixel24 = 24.sp
