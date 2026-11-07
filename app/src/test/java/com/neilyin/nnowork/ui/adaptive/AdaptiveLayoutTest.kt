package com.neilyin.nnowork.ui.adaptive

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun widthClassBoundaries() {
        assertEquals(NnoworkWidthClass.Compact, NnoworkWidthClass.fromWidth(0.dp))
        assertEquals(NnoworkWidthClass.Compact, NnoworkWidthClass.fromWidth(599.dp))
        assertEquals(NnoworkWidthClass.Compact, NnoworkWidthClass.fromWidth(599.9.dp))
        assertEquals(NnoworkWidthClass.Medium, NnoworkWidthClass.fromWidth(600.dp))
        assertEquals(NnoworkWidthClass.Medium, NnoworkWidthClass.fromWidth(839.dp))
        assertEquals(NnoworkWidthClass.Medium, NnoworkWidthClass.fromWidth(839.9.dp))
        assertEquals(NnoworkWidthClass.Expanded, NnoworkWidthClass.fromWidth(840.dp))
        assertEquals(NnoworkWidthClass.Expanded, NnoworkWidthClass.fromWidth(1280.dp))
    }

    @Test
    fun heightClassBoundaries() {
        assertEquals(NnoworkHeightClass.Compact, NnoworkHeightClass.fromHeight(479.dp))
        assertEquals(NnoworkHeightClass.Medium, NnoworkHeightClass.fromHeight(480.dp))
        assertEquals(NnoworkHeightClass.Medium, NnoworkHeightClass.fromHeight(899.dp))
        assertEquals(NnoworkHeightClass.Expanded, NnoworkHeightClass.fromHeight(900.dp))
    }

    @Test
    fun navigationTypeFollowsWidthClass() {
        assertEquals(NnoworkNavigationType.BottomBar, navigationTypeFor(NnoworkWidthClass.Compact))
        assertEquals(NnoworkNavigationType.Rail, navigationTypeFor(NnoworkWidthClass.Medium))
        assertEquals(NnoworkNavigationType.Rail, navigationTypeFor(NnoworkWidthClass.Expanded))
        assertEquals(NnoworkNavigationType.BottomBar, NnoworkWindowInfo(599.dp, 900.dp).navigationType)
        assertEquals(NnoworkNavigationType.Rail, NnoworkWindowInfo(600.dp, 900.dp).navigationType)
    }

    @Test
    fun phoneDefaultKeepsOriginalLayout() {
        val phone = NnoworkWindowInfo.PhoneDefault
        assertEquals(NnoworkNavigationType.BottomBar, phone.navigationType)
        assertEquals(HomeLayout.Phone, phone.homeLayout)
        assertFalse(phone.usesListDetail)
        assertFalse(phone.usesTwoColumnSettings)
    }

    @Test
    fun homeLayoutMatchesIos() {
        // 一般手機直向
        assertEquals(HomeLayout.Phone, homeLayoutFor(NnoworkWidthClass.Compact, NnoworkHeightClass.Medium, isLandscape = false))
        // 摺疊機展開直向（≈700×840dp）：上下堆疊
        assertEquals(HomeLayout.WideStacked, NnoworkWindowInfo(701.dp, 841.dp).homeLayout)
        // 摺疊機展開橫向（≈840×700dp）：左右兩欄
        assertEquals(HomeLayout.WideSideBySide, NnoworkWindowInfo(841.dp, 701.dp).homeLayout)
        // medium 橫向也兩欄
        assertEquals(HomeLayout.WideSideBySide, homeLayoutFor(NnoworkWidthClass.Medium, NnoworkHeightClass.Medium, isLandscape = true))
        // iOS verticalSizeClass == .compact（手機橫向，高度不足）也走兩欄
        assertEquals(HomeLayout.WideSideBySide, homeLayoutFor(NnoworkWidthClass.Compact, NnoworkHeightClass.Compact, isLandscape = true))
    }

    @Test
    fun listDetailAndSettingsColumnsOnlyOnWideWindows() {
        assertFalse(usesListDetailFor(NnoworkWidthClass.Compact))
        assertTrue(usesListDetailFor(NnoworkWidthClass.Medium))
        assertTrue(usesListDetailFor(NnoworkWidthClass.Expanded))
        assertFalse(usesTwoColumnSettingsFor(NnoworkWidthClass.Compact))
        assertTrue(usesTwoColumnSettingsFor(NnoworkWidthClass.Medium))
        assertTrue(usesTwoColumnSettingsFor(NnoworkWidthClass.Expanded))
        // 樹洞動態／貼文雙欄只在 expanded；medium 改單欄＋全寬詳情
        assertFalse(usesFeedDetailSplitFor(NnoworkWidthClass.Compact))
        assertFalse(usesFeedDetailSplitFor(NnoworkWidthClass.Medium))
        assertTrue(usesFeedDetailSplitFor(NnoworkWidthClass.Expanded))
        assertFalse(NnoworkWindowInfo(701.dp, 841.dp).usesFeedDetailSplit)
        assertTrue(NnoworkWindowInfo(841.dp, 701.dp).usesFeedDetailSplit)
        assertFalse(NnoworkWindowInfo(599.dp, 800.dp).usesListDetail)
        assertTrue(NnoworkWindowInfo(600.dp, 800.dp).usesListDetail)
    }

    @Test
    fun twoPaneSplitWithoutFoldUsesRatio() {
        val split = twoPaneSplit(containerWidth = 760.dp, containerStart = 80.dp, spacing = 0.dp, fold = FoldInfo.None)
        assertEquals(380f, split.leading.value, 0.01f)
        assertEquals(0f, split.gap.value, 0.01f)
        val withGap = twoPaneSplit(containerWidth = 740.dp, containerStart = 0.dp, spacing = 40.dp, fold = FoldInfo.None, ratio = 0.4f)
        assertEquals(280f, withGap.leading.value, 0.01f)
        assertEquals(40f, withGap.gap.value, 0.01f)
    }

    @Test
    fun twoPaneSplitAlignsToFoldAndFallsBackWhenTooNarrow() {
        // 視窗 840 寬、摺痕在 420 中線（寬 20），容器從 rail 右邊 80 開始。
        val fold = FoldInfo(hasVerticalFold = true, start = 410.dp, width = 20.dp, isSeparating = true)
        val split = twoPaneSplit(containerWidth = 760.dp, containerStart = 80.dp, spacing = 0.dp, fold = fold)
        assertEquals(20f, split.gap.value, 0.01f) // 間距至少涵蓋摺痕
        assertEquals(330f, split.leading.value, 0.01f) // 420 - 80 - 10
        // 摺痕太靠邊，任一欄會窄於最小寬度 → 退回比例
        val edgeFold = FoldInfo(hasVerticalFold = true, start = 100.dp, width = 0.dp)
        val fallback = twoPaneSplit(containerWidth = 760.dp, containerStart = 80.dp, spacing = 0.dp, fold = edgeFold)
        assertEquals(380f, fallback.leading.value, 0.01f)
    }

    @Test
    fun readableSidePaddingKeepsPhoneAndCentersWide() {
        assertEquals(20f, readableSidePadding(411.dp, 640.dp, 20.dp).value, 0.01f)
        assertEquals(20f, readableSidePadding(680.dp, 640.dp, 20.dp).value, 0.01f)
        assertEquals(60f, readableSidePadding(760.dp, 640.dp, 20.dp).value, 0.01f)
    }

    @Test
    fun verticalSeparatingHingeBecomesFold() {
        // book posture：直向摺痕、會分隔，bounds left 1092px、寬 56px，density 2.625
        val fold = foldInfoFromFeature(FoldOrientation.Vertical, isSeparating = true, occludes = false, featureLeftPx = 1092, featureWidthPx = 56, density = 2.625f)
        assertTrue(fold.hasVerticalFold)
        assertTrue(fold.isSeparating)
        assertEquals(416f, fold.start.value, 0.01f)
        assertEquals(21.33f, fold.width.value, 0.01f)
    }

    @Test
    fun occludingHingeSplitsEvenWhenFlat() {
        val fold = foldInfoFromFeature(FoldOrientation.Vertical, isSeparating = false, occludes = true, featureLeftPx = 1100, featureWidthPx = 84, density = 2.0f)
        assertTrue(fold.hasVerticalFold)
        assertEquals(550f, fold.start.value, 0.01f)
        assertEquals(42f, fold.width.value, 0.01f)
    }

    @Test
    fun flatNonOccludingFoldFallsBackToRatio() {
        val fold = foldInfoFromFeature(FoldOrientation.Vertical, isSeparating = false, occludes = false, featureLeftPx = 1104, featureWidthPx = 0, density = 2.625f)
        assertEquals(FoldInfo.None, fold)
        val split = twoPaneSplit(containerWidth = 760.dp, containerStart = 80.dp, spacing = 0.dp, fold = fold)
        assertEquals(380f, split.leading.value, 0.01f)
    }

    @Test
    fun horizontalHingeNeverSplitsColumns() {
        assertEquals(FoldInfo.None, foldInfoFromFeature(FoldOrientation.Horizontal, isSeparating = true, occludes = true, featureLeftPx = 0, featureWidthPx = 2208, density = 2.625f))
        assertEquals(FoldInfo.None, foldInfoFromFeature(FoldOrientation.Horizontal, isSeparating = false, occludes = false, featureLeftPx = 0, featureWidthPx = 2208, density = 2.625f))
    }

    @Test
    fun convertedFoldDrivesTwoPaneSplit() {
        // 840dp 視窗、摺痕在 410–430dp、容器從 rail 右側 80dp 開始
        val fold = foldInfoFromFeature(FoldOrientation.Vertical, isSeparating = true, occludes = false, featureLeftPx = 820, featureWidthPx = 40, density = 2f)
        val split = twoPaneSplit(containerWidth = 760.dp, containerStart = 80.dp, spacing = 0.dp, fold = fold)
        assertEquals(330f, split.leading.value, 0.01f)
        assertEquals(20f, split.gap.value, 0.01f)
    }
}
