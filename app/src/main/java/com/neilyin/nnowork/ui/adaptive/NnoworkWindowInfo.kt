package com.neilyin.nnowork.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 輕量 window size class（不依賴 androidx.window／material3-adaptive，離線快取沒有這些套件）。
 * 門檻照 Material 3：寬 compact < 600dp ≤ medium < 840dp ≤ expanded；
 * 高 compact < 480dp ≤ medium < 900dp ≤ expanded。
 *
 * 各畫面只透過 [LocalNnoworkWindowInfo] 取得視窗資訊與版面決策，不自己讀 LocalConfiguration，
 * 之後換成 androidx.window 的 WindowMetrics 時只要改 [ProvideNnoworkWindowInfo]。
 */
enum class NnoworkWidthClass {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        val MediumMinWidth: Dp = 600.dp
        val ExpandedMinWidth: Dp = 840.dp

        fun fromWidth(width: Dp): NnoworkWidthClass = when {
            width < MediumMinWidth -> Compact
            width < ExpandedMinWidth -> Medium
            else -> Expanded
        }
    }
}

enum class NnoworkHeightClass {
    Compact,
    Medium,
    Expanded,
    ;

    companion object {
        val MediumMinHeight: Dp = 480.dp
        val ExpandedMinHeight: Dp = 900.dp

        fun fromHeight(height: Dp): NnoworkHeightClass = when {
            height < MediumMinHeight -> Compact
            height < ExpandedMinHeight -> Medium
            else -> Expanded
        }
    }
}

/** 主導覽型態：compact 用底部 NavigationBar，medium／expanded 用左側 NavigationRail。 */
enum class NnoworkNavigationType { BottomBar, Rail }

/** 首頁版面（對齊 iOS HomeView usesWideLayout／wideContent）。 */
enum class HomeLayout {
    /** 一般手機直向：原本版面。 */
    Phone,
    /** 寬螢幕但比較高（摺疊機展開直向）：上下堆疊、限寬置中。 */
    WideStacked,
    /** 寬螢幕橫向或高度不足：左集點卡、右資訊欄。 */
    WideSideBySide,
}

@Immutable
data class NnoworkWindowInfo(
    val width: Dp,
    val height: Dp,
    val fold: FoldInfo = FoldInfo.None,
) {
    val widthClass: NnoworkWidthClass get() = NnoworkWidthClass.fromWidth(width)
    val heightClass: NnoworkHeightClass get() = NnoworkHeightClass.fromHeight(height)
    val isLandscape: Boolean get() = width > height

    /** 對應 iOS horizontalSizeClass == .regular。 */
    val isWide: Boolean get() = widthClass != NnoworkWidthClass.Compact

    val navigationType: NnoworkNavigationType get() = navigationTypeFor(widthClass)
    val homeLayout: HomeLayout get() = homeLayoutFor(widthClass, heightClass, isLandscape)

    /** 紀錄、樹洞：左清單／右詳情（iOS NavigationSplitView，只在 regular width）。 */
    val usesListDetail: Boolean get() = usesListDetailFor(widthClass)

    /**
     * 樹洞：左動態／右貼文只在 expanded；medium（約 600–840dp）左欄會窄到篩選列換行，
     * 改單欄動態＋全寬詳情。
     */
    val usesFeedDetailSplit: Boolean get() = usesFeedDetailSplitFor(widthClass)

    /** 設定：左右兩欄各自捲動（iOS SettingsView regular 分支）。 */
    val usesTwoColumnSettings: Boolean get() = usesTwoColumnSettingsFor(widthClass)

    companion object {
        /** 預設＝一般手機直向，讓 Preview 與單元測試外觀不變。 */
        val PhoneDefault = NnoworkWindowInfo(width = 411.dp, height = 891.dp)
    }
}

fun navigationTypeFor(widthClass: NnoworkWidthClass): NnoworkNavigationType =
    if (widthClass == NnoworkWidthClass.Compact) NnoworkNavigationType.BottomBar else NnoworkNavigationType.Rail

/**
 * iOS：`horizontalSizeClass == .regular || verticalSizeClass == .compact` 走寬版；
 * 寬版裡「寬 > 高」左右兩欄，否則上下堆疊。
 */
fun homeLayoutFor(
    widthClass: NnoworkWidthClass,
    heightClass: NnoworkHeightClass,
    isLandscape: Boolean,
): HomeLayout {
    val wide = widthClass != NnoworkWidthClass.Compact || heightClass == NnoworkHeightClass.Compact
    return when {
        !wide -> HomeLayout.Phone
        isLandscape -> HomeLayout.WideSideBySide
        else -> HomeLayout.WideStacked
    }
}

fun usesListDetailFor(widthClass: NnoworkWidthClass): Boolean = widthClass != NnoworkWidthClass.Compact

fun usesFeedDetailSplitFor(widthClass: NnoworkWidthClass): Boolean = widthClass == NnoworkWidthClass.Expanded

fun usesTwoColumnSettingsFor(widthClass: NnoworkWidthClass): Boolean = widthClass != NnoworkWidthClass.Compact

/** 大螢幕版面的尺寸 token（對齊 iOS DuoLayout）。 */
object NnoworkAdaptive {
    /** 單欄內容最大閱讀寬度（iOS DuoLayout.readableMaxWidth 640）。 */
    val readableMaxWidth: Dp = 640.dp
    /** 蓋章輸入等表單的最大寬度。 */
    val formMaxWidth: Dp = 600.dp
    /** Bottom sheet 最大寬度（Material 預設 640，明確寫出來）。 */
    val sheetMaxWidth: Dp = 640.dp
    /** 雙欄任一欄的最小寬度；不夠時退回比例分配（iOS foldSplit minColumnWidth 120，這裡放寬給內容）。 */
    val minPaneWidth: Dp = 240.dp
    /** 首頁上下堆疊版面的最大寬度（iOS wideStackedColumn 560）。 */
    val homeStackedMaxWidth: Dp = 560.dp
    /** 首頁左右兩欄時右欄的最大寬度（iOS 440）。 */
    val homeSecondaryMaxWidth: Dp = 440.dp
}

/** 寬螢幕單欄內容置中時的左右內距：至少 [minPadding]，內容寬不超過 [maxContentWidth]。 */
fun readableSidePadding(containerWidth: Dp, maxContentWidth: Dp, minPadding: Dp): Dp {
    val centered = (containerWidth - maxContentWidth) / 2
    return if (centered > minPadding) centered else minPadding
}

val LocalNnoworkWindowInfo = staticCompositionLocalOf { NnoworkWindowInfo.PhoneDefault }

/**
 * 在 App 根部提供視窗資訊。寬高取自 LocalConfiguration（targetSdk 35+ 已含 system bars，等於視窗大小），
 * 摺痕取自 [rememberFoldInfo]。
 */
@Composable
fun ProvideNnoworkWindowInfo(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val fold = rememberFoldInfo()
    val info = remember(configuration.screenWidthDp, configuration.screenHeightDp, fold) {
        NnoworkWindowInfo(
            width = configuration.screenWidthDp.dp,
            height = configuration.screenHeightDp.dp,
            fold = fold,
        )
    }
    CompositionLocalProvider(LocalNnoworkWindowInfo provides info, content = content)
}
