package com.neilyin.nnowork.ui.theme

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

/**
 * 主題印章的唯一繪製入口。
 * 熊貓／樹懶使用去背 PNG，其餘主題沿用 Material icon；index 決定固定變體。
 */
@Composable
fun ThemeStampMark(
    theme: CardTheme,
    index: Int,
    modifier: Modifier = Modifier,
    tint: Color = theme.stampIconColor,
    contentDescription: String? = null,
) {
    val imageResource = theme.stampImageResource(index)
    if (imageResource != null) {
        Icon(
            painter = painterResource(imageResource),
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier,
        )
    } else {
        Icon(
            imageVector = theme.stampIcon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier,
        )
    }
}
