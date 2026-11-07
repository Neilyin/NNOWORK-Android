package com.neilyin.nnowork.ui.adaptive

import com.neilyin.nnowork.util.findActivity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * 視窗裡的直向分隔（摺痕／hinge）。座標以「視窗左緣」為 0 的 dp 表示。
 *
 * 對應 iOS DuoVerticalFold；由 androidx.window 的 FoldingFeature 換算：
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
 * 目前視窗的摺痕：讀 androidx.window 的 [FoldingFeature]，換算規則在 [foldInfoFromFeature]。
 * 沒有 Activity（例如 Preview）、沒有摺痕、或摺痕不分隔內容時回 [FoldInfo.None]，雙欄走比例分配。
 * 各畫面透過 [LocalNnoworkWindowInfo].fold 與 [NnoworkTwoPane] 取用。
 */
@Composable
fun rememberFoldInfo(): FoldInfo {
    val activity = LocalContext.current.findActivity() ?: return FoldInfo.None
    val density = LocalDensity.current.density
    val layoutInfoFlow = remember(activity) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
    }
    val layoutInfo by layoutInfoFlow.collectAsState(initial = null)
    val feature = layoutInfo?.displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.firstOrNull() ?: return FoldInfo.None
    return foldInfoFromFeature(
        orientation = if (feature.orientation == FoldingFeature.Orientation.VERTICAL) {
            FoldOrientation.Vertical
        } else {
            FoldOrientation.Horizontal
        },
        isSeparating = feature.isSeparating,
        occludes = feature.occlusionType == FoldingFeature.OcclusionType.FULL,
        featureLeftPx = feature.bounds.left,
        featureWidthPx = feature.bounds.width(),
        density = density,
    )
}


/** FoldingFeature.orientation 的對應（不直接依賴 androidx.window，方便單元測試）。 */
enum class FoldOrientation { Vertical, Horizontal }

/**
 * FoldingFeature → [FoldInfo] 的換算（純函式，單位 px，由呼叫端帶入 density 與視窗左緣）。
 *
 * - 只有直向（左右分欄用）的摺痕會被採用；水平摺痕（tabletop）一律不分欄。
 * - 只在「會分隔內容」時才依 bounds 分欄：`isSeparating`（book posture 半摺、雙螢幕）
 *   或 hinge 會遮擋畫面（`occlusionType == FULL`）。全開、不遮擋的摺痕回 [FoldInfo.None]，
 *   雙欄退回比例分配。
 *
 * @param featureLeftPx FoldingFeature.bounds.left（相對於視窗）。
 * @param featureWidthPx FoldingFeature.bounds.width()。
 */
fun foldInfoFromFeature(
    orientation: FoldOrientation,
    isSeparating: Boolean,
    occludes: Boolean,
    featureLeftPx: Int,
    featureWidthPx: Int,
    density: Float,
): FoldInfo {
    if (orientation != FoldOrientation.Vertical) return FoldInfo.None
    if (!isSeparating && !occludes) return FoldInfo.None
    if (density <= 0f) return FoldInfo.None
    return FoldInfo(
        hasVerticalFold = true,
        start = (featureLeftPx / density).dp,
        width = (featureWidthPx.coerceAtLeast(0) / density).dp,
        isSeparating = true,
    )
}

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
