package com.neilyin.nnowork.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.R

/**
 * 集點卡主題。id 與商品 ID 已寫入 iOS／Supabase 正式資料，不可重新編號或改名。
 * iOS 使用 SF Symbols，這裡映射到 Material Icons。
 */
enum class CardTheme(val id: Int) {
    RPG(1),
    CYBERPUNK(2),
    VACATION(3),
    SAKURA(4),
    LAZY_CAT(5),
    NOTION(6),
    SUMMER(7),
    PANDA(8),
    SLOTH(9),
    /** 第 10 個付費主題：夜空金（色值取自 iOS YearReviewNightStyle）。 */
    NIGHT(10);

    @get:StringRes
    val titleRes: Int
        get() = when (this) {
            RPG -> R.string.theme_rpg_title
            CYBERPUNK -> R.string.theme_cyberpunk_title
            VACATION -> R.string.theme_vacation_title
            SAKURA -> R.string.theme_sakura_title
            LAZY_CAT -> R.string.theme_lazy_cat_title
            NOTION -> R.string.theme_notion_title
            SUMMER -> R.string.theme_summer_title
            PANDA -> R.string.theme_panda_title
            SLOTH -> R.string.theme_sloth_title
            NIGHT -> R.string.theme_night_title
        }

    @get:StringRes
    val descriptionRes: Int
        get() = when (this) {
            RPG -> R.string.theme_rpg_description
            CYBERPUNK -> R.string.theme_cyberpunk_description
            VACATION -> R.string.theme_vacation_description
            SAKURA -> R.string.theme_sakura_description
            LAZY_CAT -> R.string.theme_lazy_cat_description
            NOTION -> R.string.theme_notion_description
            SUMMER -> R.string.theme_summer_description
            PANDA -> R.string.theme_panda_description
            SLOTH -> R.string.theme_sloth_description
            NIGHT -> R.string.theme_night_description
        }

    val title: String
        get() = when (this) {
            RPG -> "經典 RPG"
            CYBERPUNK -> "系統錯誤"
            VACATION -> "度假模式"
            SAKURA -> "春日花香"
            LAZY_CAT -> "慵懶寵物"
            NOTION -> "極簡筆記"
            SUMMER -> "夏日海風"
            PANDA -> "厭世熊貓"
            SLOTH -> "樹懶模式"
            NIGHT -> "夜空金"
        }

    val description: String
        get() = when (this) {
            RPG -> "把職場當作一場冒險遊戲，\n用像素劍斬斷社畜命運！"
            CYBERPUNK -> "覺醒的駭客，\n打破這個名為公司的矩陣。"
            VACATION -> "心已經飛去海島退休了，\n只差肉體還在這裡。"
            SAKURA -> "讓每一個不想上班的日子，\n都開出一朵櫻花。"
            LAZY_CAT -> "就算天塌下來，\n也要先睡個午覺。"
            NOTION -> "如同一張空白的 Notion 頁面，\n用純粹的黑白極簡，\n記錄離職的每一步。"
            SUMMER -> "把辦公室的冷氣，\n想像成海邊的風。"
            PANDA -> "黑眼圈是與生俱來的，\n不是加班加出來的。"
            SLOTH -> "再快也是這樣，\n再慢也是這樣。\n那就慢慢來吧。"
            NIGHT -> "下班後的夜空最安靜，\n星星替你數著，\n離自由還有幾步。"
        }

    val primaryColor: Color
        get() = when (this) {
            RPG -> PixelColors.blue
            CYBERPUNK -> PixelColors.green
            VACATION -> PixelColors.orange
            SAKURA -> PixelColors.sakuraPrimary
            LAZY_CAT -> PixelColors.catPrimary
            NOTION -> PixelColors.notionText
            SUMMER -> PixelColors.summerPrimary
            PANDA -> PixelColors.pandaPrimary
            SLOTH -> PixelColors.slothPrimary
            NIGHT -> PixelColors.nightGold
        }

    val backgroundColor: Color
        get() = when (this) {
            RPG -> PixelColors.rpgBackground
            CYBERPUNK -> PixelColors.cyberpunkBackground
            VACATION -> PixelColors.vacationBackground
            SAKURA -> PixelColors.sakuraBackground
            LAZY_CAT -> PixelColors.catBackground
            NOTION -> PixelColors.notionBackground
            SUMMER -> PixelColors.summerBackground
            PANDA -> PixelColors.pandaBackground
            SLOTH -> PixelColors.slothBackground
            NIGHT -> PixelColors.nightSkyTop
        }

    val accentColor: Color
        get() = when (this) {
            RPG -> PixelColors.yellow
            CYBERPUNK -> PixelColors.green
            VACATION -> PixelColors.turquoise
            SAKURA -> PixelColors.sakuraAccent
            LAZY_CAT -> PixelColors.catAccent
            NOTION -> PixelColors.notionAccent
            SUMMER -> PixelColors.summerAccent
            PANDA -> PixelColors.pandaAccent
            SLOTH -> PixelColors.slothAccent
            NIGHT -> PixelColors.nightGold
        }

    val stampIcon: ImageVector
        get() = when (this) {
            RPG -> Icons.Filled.Shield
            CYBERPUNK -> Icons.Filled.BugReport
            VACATION -> Icons.Filled.WbSunny
            SAKURA -> Icons.Filled.LocalFlorist
            LAZY_CAT -> Icons.Filled.Pets
            NOTION -> Icons.Filled.Check
            SUMMER -> Icons.Filled.WbSunny
            PANDA -> Icons.Filled.LocalFlorist
            SLOTH -> Icons.Filled.Hotel
            NIGHT -> Icons.Filled.Star
        }

    val borderColor: Color
        get() = when (this) {
            RPG -> PixelColors.white
            CYBERPUNK -> PixelColors.green
            VACATION -> PixelColors.orange
            SAKURA -> PixelColors.sakuraBorder
            LAZY_CAT -> PixelColors.catBorder
            NOTION -> PixelColors.notionBorder
            SUMMER -> PixelColors.summerBorder
            PANDA -> PixelColors.pandaBorder
            SLOTH -> PixelColors.slothBorder
            NIGHT -> PixelColors.nightCardBorder
        }

    val titleColor: Color
        get() = when (this) {
            RPG -> PixelColors.white
            CYBERPUNK -> PixelColors.white
            VACATION -> PixelColors.black
            SAKURA -> PixelColors.sakuraText
            LAZY_CAT -> PixelColors.catText
            NOTION -> PixelColors.notionText
            SUMMER -> PixelColors.summerText
            PANDA -> PixelColors.pandaText
            SLOTH -> PixelColors.slothText
            NIGHT -> PixelColors.nightStarWhite
        }

    val buttonActionIcon: ImageVector
        get() = when (this) {
            LAZY_CAT -> Icons.Filled.Pets
            NOTION -> Icons.Filled.Check
            NIGHT -> Icons.Filled.Star
            else -> Icons.Filled.ThumbUp
        }

    /** 首頁 CTA 與蓋章輸入頁共用，避免同一動作在兩個畫面出現不同配色。 */
    val stampActionBackgroundColor: Color get() = primaryColor
    val stampActionContentColor: Color get() = palette.onPrimary

    val sheetIcon: ImageVector
        get() = when (this) {
            LAZY_CAT -> Icons.Filled.Hotel
            NOTION -> Icons.Filled.Description
            SUMMER -> Icons.Filled.WbSunny
            PANDA -> Icons.Filled.LocalFlorist
            SLOTH -> Icons.Filled.Hotel
            NIGHT -> Icons.Filled.NightsStay
            else -> Icons.Filled.Edit
        }

    val sheetTitle: String
        get() = when (this) {
            LAZY_CAT -> "今天又想睡覺了？"
            SLOTH -> "今天又想耍廢了？"
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
            SUMMER -> 14.dp
            PANDA -> 18.dp
            SLOTH -> 20.dp
            // iOS reui：rpg／cyberpunk／vacation 由直角改為 PixelRadius.small。
            else -> NnoworkRadius.small
        }

    val stampedCellColor: Color
        get() = when (this) {
            SAKURA -> PixelColors.sakuraStamp
            LAZY_CAT -> PixelColors.catStamp
            NOTION -> PixelColors.notionStamp
            SUMMER -> PixelColors.summerStamp
            PANDA -> PixelColors.pandaStamp
            SLOTH -> PixelColors.slothStamp
            NIGHT -> PixelColors.nightGold
            else -> PixelColors.yellow
        }

    val emptyCellColor: Color
        get() = when (this) {
            SAKURA -> colorHex("#FFDFEB")
            LAZY_CAT -> colorHex("#FFECCC")
            NOTION -> PixelColors.notionBackground
            SUMMER -> PixelColors.summerEmptyCell
            PANDA -> PixelColors.pandaEmptyCell
            SLOTH -> PixelColors.slothEmptyCell
            NIGHT -> PixelColors.nightEmptyCell
            // 深色卡片主題：空格用卡片底色的柔和一階，不再是刺眼的純白（同夜空的作法）。
            RPG -> colorHex("#2D3E50")
            CYBERPUNK -> colorHex("#122017")
            // 度假的卡片是蜜桃色：空格用更淺一階的蜜桃。
            VACATION -> colorHex("#FDE8C6")
        }

    /** 空格上的數字色（對空格底色 ≥ 4.5:1，由 NnoworkPaletteTest 鎖定）。 */
    val emptyCellTextColor: Color
        get() = when (this) {
            RPG -> colorHex("#E6EDF4")
            CYBERPUNK -> colorHex("#A9F0C4")
            VACATION -> PixelColors.black
            else -> palette.onContent
        }

    val requiresCoins: Boolean
        get() = id >= SAKURA.id

    val coinCost: Int
        get() = if (requiresCoins) 15 else 0

    /** 與 iOS/Supabase 銷售統計共用的穩定商品 ID。 */
    val shopProductId: String
        get() = when (this) {
            RPG -> "theme.rpg"
            CYBERPUNK -> "theme.cyberpunk"
            VACATION -> "theme.vacation"
            SAKURA -> "theme.sakura"
            LAZY_CAT -> "theme.lazy_cat"
            NOTION -> "theme.notion"
            SUMMER -> "theme.summer"
            PANDA -> "theme.panda"
            SLOTH -> "theme.sloth"
            NIGHT -> "theme.night"
        }

    /** 熊貓／樹懶沒有 Material 對應圖示，依格子序號固定輪替去背 PNG。 */
    @DrawableRes
    fun stampImageResource(index: Int): Int? {
        val variant = Math.floorMod(index, 3)
        return when (this) {
            PANDA -> when (variant) {
                0 -> R.drawable.stamp_panda_1
                1 -> R.drawable.stamp_panda_2
                else -> R.drawable.stamp_panda_3
            }
            SLOTH -> when (variant) {
                0 -> R.drawable.stamp_sloth_1
                1 -> R.drawable.stamp_sloth_2
                else -> R.drawable.stamp_sloth_3
            }
            else -> null
        }
    }

    val stampIconColor: Color
        get() = when (this) {
            // RPG/Cyberpunk/Vacation 的已蓋章底色相近，使用各自 accent 會失去對比；
            // 亮色印章格統一用深色 icon，Notion 黑底則反轉為白色。
            NOTION -> PixelColors.white
            PANDA -> PixelColors.pandaPrimary
            SLOTH -> PixelColors.slothPrimary
            SUMMER -> PixelColors.summerText
            NIGHT -> PixelColors.nightSkyTop
            else -> PixelColors.black
        }

    val emptyCellBorderOpacity: Float
        get() = if (this == NOTION) 1.0f else 0.3f

    val stampedCellBorderOpacity: Float
        get() = if (this == NOTION) 0.0f else 0.8f

    val cardSurfaceColor: Color
        get() = when (this) {
            NOTION -> PixelColors.notionSurface
            SUMMER -> PixelColors.summerSurface
            PANDA -> PixelColors.pandaSurface
            SLOTH -> PixelColors.slothSurface
            NIGHT -> PixelColors.nightSurface
            else -> backgroundColor
        }

    val isLightTheme: Boolean
        get() = this != RPG && this != CYBERPUNK && this != NIGHT

    /**
     * 會把整個 App 的 chrome 與內容切成淺色語意色的主題（對齊 iOS usesAppWideLightPalette）。
     * vacation 只有集點卡本身是淺色，全 App 維持深色。
     */
    val usesAppWideLightPalette: Boolean
        get() = when (this) {
            SAKURA, LAZY_CAT, NOTION, SUMMER, PANDA, SLOTH -> true
            else -> false
        }

    companion object {
        fun fromId(id: Int): CardTheme = entries.firstOrNull { it.id == id } ?: RPG
    }
}

fun CardTheme.isUnlocked(unlockedStyles: Collection<Int>): Boolean =
    !requiresCoins || id in unlockedStyles

/** 只有未解鎖的付費風格需要安全退回 RPG；免費風格一律可套用到整個 App。 */
fun CardTheme.appWideTheme(unlockedStyles: Collection<Int>): CardTheme =
    if (isUnlocked(unlockedStyles)) this else CardTheme.RPG

/** 固定傾斜角，避免畫面重組後印章抖動。 */
fun stampTiltDegrees(index: Int): Float {
    val steps = floatArrayOf(-11f, 7f, -4f, 12f, -8f, 3f, -13f, 9f, -6f, 5f)
    return steps[Math.floorMod(index, steps.size)]
}
