package com.neilyin.nnowork.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 視窗裡的直向分隔（摺痕／hinge）。座標以「視窗左緣」為 0 的 dp 表示。
 *
 * 對應 iOS DuoVerticalFold；之後接 androidx.window 的 FoldingFeature 時：
 * `bounds.left / density` → [start]、`bounds.width() / density` → [width]、
 * `isSeparating` → [isSeparating]，`orientation == VERTICAL` 才填。
 */
@Immutable
data class FoldInfo(
    /** 有沒有直向分隔（全開時多數裝置仍會回報 hinge，但寬度為 0、不分隔）。 */
    val hasVerticalFold: Boolean,
    val start: Dp = 0.dp,
    val width: Dp = 0.dp,
    /** true＝半摺或實體 hinge，內容不該跨過分隔。 */
    val isSeparating: Boolean = false,
) {
    val center: Dp get() = start + width / 2

    companion object {
        val None = FoldInfo(hasVerticalFold = false)
    }
}

/**
 * 這期的摺痕來源：一律回傳「沒有摺痕」，雙欄走比例分配。
 *
 * 接上 FoldingFeature 只要改這個函式（加 androidx.window 依賴後）：
 * ```
 * val activity = LocalContext.current.findActivity()
 * val layoutInfo by remember(activity) { WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity) }
 *     .collectAsState(initial = null)
 * val feature = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()
 *     ?.firstOrNull { it.orientation == FoldingFeature.Orientation.VERTICAL }
 * return feature?.let { FoldInfo(true, (it.bounds.left / density).dp, (it.bounds.width() / density).dp, it.isSeparating) } ?: FoldInfo.None
 * ```
 * 各畫面透過 [LocalNnoworkWindowInfo].fold 與 [NnoworkTwoPane] 取用，不需修改。
 */
@Composable
fun rememberFoldInfo(): FoldInfo = FoldInfo.None

/** 雙欄的切分結果：左欄寬度與兩欄間距。 */
@Immutable
data class PaneSplit(val leading: Dp, val gap: Dp)

/**
 * 決定雙欄分界（iOS DuoLayout.foldSplit）：
 * 有直向摺痕且落在容器內 → 左欄寬到「摺痕中線減半個間距」，間距至少涵蓋摺痕本身；
 * 沒有摺痕、或任一欄會窄於 [minPaneWidth] → 依 [ratio] 分配。
 *
 * @param containerStart 容器左緣在視窗中的 x（摺痕座標以視窗為準，左側有 NavigationRail 時不為 0）。
 */
fun twoPaneSplit(
    containerWidth: Dp,
    containerStart: Dp,
    spacing: Dp,
    fold: FoldInfo,
    ratio: Float = 0.5f,
    minPaneWidth: Dp = NnoworkAdaptive.minPaneWidth,
): PaneSplit {
    val fallback = PaneSplit(leading = (containerWidth - spacing) * ratio.coerceIn(0.1f, 0.9f), gap = spacing)
    if (!fold.hasVerticalFold || containerWidth <= 0.dp) return fallback
    val gap = if (fold.width > spacing) fold.width else spacing
    val leading = fold.center - containerStart - gap / 2
    val trailing = containerWidth - leading - gap
    if (leading < minPaneWidth || trailing < minPaneWidth) return fallback
    return PaneSplit(leading = leading, gap = gap)
}
