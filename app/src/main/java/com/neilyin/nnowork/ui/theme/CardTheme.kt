package com.neilyin.nnowork.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 集點卡主題。對應 iOS 版 CardTheme（rawValue 1~6）。
 * iOS 使用 SF Symbols，這裡映射到 Material Icons。
 */
enum class CardTheme(val id: Int) {
    RPG(1),
    CYBERPUNK(2),
    VACATION(3),
    SAKURA(4),
    LAZY_CAT(5),
    NOTION(6);

    val title: String
        get() = when (this) {
            RPG -> "經典 RPG"
            CYBERPUNK -> "系統錯誤"
            VACATION -> "度假模式"
            SAKURA -> "春日花香"
            LAZY_CAT -> "慵懶寵物"
            NOTION -> "極簡筆記"
        }

    val description: String
        get() = when (this) {
            RPG -> "把職場當作一場冒險遊戲，\n用像素劍斬斷社畜命運！"
            CYBERPUNK -> "覺醒的駭客，\n打破這個名為公司的矩陣。"
            VACATION -> "心已經飛去海島退休了，\n只差肉體還在這裡。"
            SAKURA -> "讓每一個不想上班的日子，\n都開出一朵櫻花。"
            LAZY_CAT -> "就算天塌下來，\n也要先睡個午覺。"
            NOTION -> "如同一張空白的 Notion 頁面，\n用純粹的黑白極簡，\n記錄離職的每一步。"
        }

    val primaryColor: Color
        get() = when (this) {
            RPG -> PixelColors.blue
            CYBERPUNK -> PixelColors.green
            VACATION -> PixelColors.orange
            SAKURA -> PixelColors.sakuraPrimary
            LAZY_CAT -> PixelColors.catPrimary
            NOTION -> PixelColors.notionText
        }

    val backgroundColor: Color
        get() = when (this) {
            RPG -> PixelColors.rpgBackground
            CYBERPUNK -> PixelColors.cyberpunkBackground
            VACATION -> PixelColors.vacationBackground
            SAKURA -> PixelColors.sakuraBackground
            LAZY_CAT -> PixelColors.catBackground
            NOTION -> PixelColors.notionBackground
        }

    val accentColor: Color
        get() = when (this) {
            RPG -> PixelColors.yellow
            CYBERPUNK -> PixelColors.green
            VACATION -> PixelColors.turquoise
            SAKURA -> PixelColors.sakuraAccent
            LAZY_CAT -> PixelColors.catAccent
            NOTION -> PixelColors.notionAccent
        }

    val stampIcon: ImageVector
        get() = when (this) {
            RPG -> Icons.Filled.Shield
            CYBERPUNK -> Icons.Filled.BugReport
            VACATION -> Icons.Filled.WbSunny
            SAKURA -> Icons.Filled.LocalFlorist
            LAZY_CAT -> Icons.Filled.Pets
            NOTION -> Icons.Filled.Check
        }

    val borderColor: Color
        get() = when (this) {
            RPG -> PixelColors.white
            CYBERPUNK -> PixelColors.green
            VACATION -> PixelColors.orange
            SAKURA -> PixelColors.sakuraBorder
            LAZY_CAT -> PixelColors.catBorder
            NOTION -> PixelColors.notionBorder
        }

    val titleColor: Color
        get() = when (this) {
            RPG -> PixelColors.white
            CYBERPUNK -> PixelColors.white
            VACATION -> PixelColors.black
            SAKURA -> PixelColors.sakuraText
            LAZY_CAT -> PixelColors.catText
            NOTION -> PixelColors.notionText
        }

    val buttonActionIcon: ImageVector
        get() = when (this) {
            LAZY_CAT -> Icons.Filled.Pets
            NOTION -> Icons.Filled.Check
            else -> Icons.Filled.ThumbUp
        }

    val sheetIcon: ImageVector
        get() = when (this) {
            LAZY_CAT -> Icons.Filled.Hotel
            NOTION -> Icons.Filled.Description
            else -> Icons.Filled.Edit
        }

    val sheetTitle: String
        get() = when (this) {
            LAZY_CAT -> "今天又想睡覺了？"
            else -> "今天又想離職了？"
        }

    val emptyCellBackgroundIcon: ImageVector?
        get() = when (this) {
            LAZY_CAT -> Icons.Filled.Restaurant
            else -> null
        }

    val cellCornerRadius: Dp
        get() = when (this) {
            SAKURA -> 12.dp
            LAZY_CAT -> 16.dp
            NOTION -> 6.dp
            else -> 0.dp
        }

    val stampedCellColor: Color
        get() = when (this) {
            SAKURA -> PixelColors.sakuraStamp
            LAZY_CAT -> PixelColors.catStamp
            NOTION -> PixelColors.notionStamp
            else -> PixelColors.yellow
        }

    val emptyCellColor: Color
        get() = when (this) {
            SAKURA -> colorHex("#FFDFEB")
            LAZY_CAT -> colorHex("#FFECCC")
            NOTION -> PixelColors.notionBackground
            else -> PixelColors.white
        }

    val requiresCoins: Boolean
        get() = this == SAKURA || this == LAZY_CAT || this == NOTION

    val coinCost: Int
        get() = if (!requiresCoins) 0 else if (this == NOTION) 15 else 10

    val stampIconColor: Color
        get() = if (this == NOTION) PixelColors.white else accentColor

    val emptyCellBorderOpacity: Float
        get() = if (this == NOTION) 1.0f else 0.3f

    val stampedCellBorderOpacity: Float
        get() = if (this == NOTION) 0.0f else 0.8f

    val cardSurfaceColor: Color
        get() = if (this == NOTION) PixelColors.notionSurface else backgroundColor

    val isLightTheme: Boolean
        get() = this == VACATION || this == SAKURA || this == LAZY_CAT || this == NOTION

    companion object {
        fun fromId(id: Int): CardTheme = entries.firstOrNull { it.id == id } ?: RPG
    }
}
