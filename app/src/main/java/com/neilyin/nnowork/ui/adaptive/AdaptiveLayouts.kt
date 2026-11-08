package com.neilyin.nnowork.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkTheme

/**
 * 寬螢幕上把單欄內容限制在可讀寬度並置中（iOS duoReadableWidth）；窄螢幕寬度本來就小於上限，外觀不變。
 * 父層需給滿寬；這個 modifier 自己會 fillMaxWidth 再置中。
 */
fun Modifier.readableWidth(maxWidth: Dp = NnoworkAdaptive.readableMaxWidth): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)

/**
 * 給 LazyColumn 等需要「整片可捲、內容置中限寬」的清單：依容器寬度算左右內距後交給 [content]。
 * 用內距而不是縮窄清單，兩側空白處也能捲動。手機寬度時內距＝[minPadding]，與原本相同。
 */
@Composable
fun ReadableWidthContainer(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = NnoworkAdaptive.readableMaxWidth,
    minPadding: Dp,
    content: @Composable (horizontalPadding: Dp) -> Unit,
) {
    BoxWithConstraints(modifier) {
        content(readableSidePadding(maxWidth, maxContentWidth, minPadding))
    }
}

/**
 * 左右雙欄（iOS DuoFoldHStack／NavigationSplitView）。分界依 [twoPaneSplit]：
 * 有摺痕對準摺痕，沒有就依 [ratio]。[showDivider] 時在兩欄之間畫一條 hairline。
 */
@Composable
fun NnoworkTwoPane(
    modifier: Modifier = Modifier,
    ratio: Float = 0.5f,
    spacing: Dp = 0.dp,
    showDivider: Boolean = true,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    val fold = LocalNnoworkWindowInfo.current.fold
    val density = LocalDensity.current
    var containerStart by remember { mutableStateOf(0.dp) }
    BoxWithConstraints(
        modifier.onGloballyPositioned { coords ->
            containerStart = with(density) { coords.positionInWindow().x.toDp() }
        },
    ) {
        val gapForDivider = if (showDivider && spacing < NnoworkLine.hairline) NnoworkLine.hairline else spacing
        val split = twoPaneSplit(
            containerWidth = maxWidth,
            containerStart = containerStart,
            spacing = gapForDivider,
            fold = fold,
            ratio = ratio,
        )
        Row(Modifier.fillMaxHeight()) {
            Box(Modifier.width(split.leading).fillMaxHeight()) { first() }
            Box(Modifier.width(split.gap).fillMaxHeight(), contentAlignment = Alignment.Center) {
                if (showDivider) {
                    Box(
                        Modifier
                            .width(NnoworkLine.hairline)
                            .fillMaxHeight()
                            .background(NnoworkTheme.colors.divider),
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) { second() }
        }
    }
}
