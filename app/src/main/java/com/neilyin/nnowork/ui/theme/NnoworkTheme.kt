package com.neilyin.nnowork.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalNnoworkColors = staticCompositionLocalOf { CardTheme.RPG.nnoworkColors }
val LocalNnoworkTypeScale = staticCompositionLocalOf { CardTheme.RPG.typeScale }

/**
 * 語意 token 存取器（對應 MaterialTheme.colorScheme 的擴充）。
 * `NnoworkTheme { }` composable 會依目前 CardTheme 提供這些值。
 * 間距／圓角／尺寸是跨主題常數：見 NnoworkSpacing、NnoworkRadius、NnoworkSize。
 */
object NnoworkTheme {
    val colors: NnoworkColors
        @Composable @ReadOnlyComposable get() = LocalNnoworkColors.current

    val type: NnoworkTypeScale
        @Composable @ReadOnlyComposable get() = LocalNnoworkTypeScale.current
}
