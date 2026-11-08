package com.neilyin.nnowork.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

/**
 * M3 Typography：全部角色都用系統 Sans（中文走 Noto Sans CJK），中英文同一套字形。
 * 等寬只透過 pixelTextStyle(monospace = true) 明確指定（數字、集點格、徽章、品牌標題）。
 * 字級與 NnoworkTypeScale 同一組 8 級：32/28/24/20/16/14/13/11。
 */
private val NnoworkM3Typography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

private val NnoworkShapes = Shapes(
    extraSmall = RoundedCornerShape(NnoworkRadius.small),
    small = RoundedCornerShape(NnoworkRadius.field),
    medium = RoundedCornerShape(NnoworkRadius.card),
    large = RoundedCornerShape(NnoworkRadius.hero),
    extraLarge = RoundedCornerShape(NnoworkRadius.sheet),
)

/**
 * 主題的品牌色票（集點卡主視覺、Widget、印章動作鈕用）。
 * 全 App 介面色請用 [NnoworkColors]（NnoworkTheme.colors），不要再從這裡取。
 */
@Immutable
data class NnoworkPalette(
    val page: Color,
    val onPage: Color,
    val chrome: Color,
    val onChrome: Color,
    val content: Color,
    val contentAlt: Color,
    val onContent: Color,
    val border: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
)

val CardTheme.palette: NnoworkPalette
    get() = when (this) {
        CardTheme.RPG -> NnoworkPalette(
            page = PixelColors.rpgBackground,
            onPage = PixelColors.white,
            chrome = colorHex("#17232E"),
            onChrome = PixelColors.white,
            content = colorHex("#F5F9FC"),
            contentAlt = colorHex("#D9ECF8"),
            onContent = PixelColors.black,
            border = colorHex("#10202D"),
            primary = PixelColors.blue,
            onPrimary = PixelColors.black,
            accent = PixelColors.yellow,
            onAccent = PixelColors.black,
        )
        CardTheme.CYBERPUNK -> NnoworkPalette(
            page = PixelColors.cyberpunkBackground,
            onPage = colorHex("#DFFFF0"),
            chrome = colorHex("#07140D"),
            onChrome = colorHex("#E9FFF1"),
            content = colorHex("#ECFFF3"),
            contentAlt = colorHex("#CFF6DD"),
            onContent = colorHex("#07140D"),
            border = PixelColors.green,
            primary = PixelColors.green,
            onPrimary = PixelColors.black,
            accent = PixelColors.yellow,
            onAccent = PixelColors.black,
        )
        CardTheme.VACATION -> NnoworkPalette(
            page = PixelColors.vacationBackground,
            onPage = PixelColors.black,
            chrome = colorHex("#174C55"),
            onChrome = PixelColors.white,
            content = colorHex("#FFF8EA"),
            contentAlt = colorHex("#DDF7F1"),
            onContent = PixelColors.black,
            border = colorHex("#8A4F08"),
            primary = PixelColors.orange,
            onPrimary = PixelColors.black,
            accent = PixelColors.turquoise,
            onAccent = PixelColors.black,
        )
        CardTheme.SAKURA -> NnoworkPalette(
            page = PixelColors.sakuraBackground,
            onPage = PixelColors.sakuraText,
            chrome = colorHex("#6D3145"),
            onChrome = PixelColors.white,
            content = colorHex("#FFFDFE"),
            contentAlt = colorHex("#FFE4EE"),
            onContent = PixelColors.sakuraText,
            border = colorHex("#A23C5D"),
            primary = PixelColors.sakuraPrimary,
            onPrimary = PixelColors.sakuraText,
            accent = PixelColors.sakuraAccent,
            onAccent = PixelColors.white,
        )
        CardTheme.LAZY_CAT -> NnoworkPalette(
            page = PixelColors.catBackground,
            onPage = PixelColors.catText,
            chrome = colorHex("#5B3A22"),
            onChrome = PixelColors.white,
            content = colorHex("#FFFDF7"),
            contentAlt = colorHex("#E5F3E9"),
            onContent = colorHex("#3F2A19"),
            border = PixelColors.catText,
            primary = PixelColors.catPrimary,
            onPrimary = colorHex("#3F2A19"),
            accent = PixelColors.catAccent,
            onAccent = PixelColors.white,
        )
        CardTheme.NOTION -> NnoworkPalette(
            page = PixelColors.notionBackground,
            onPage = PixelColors.notionText,
            chrome = colorHex("#242424"),
            onChrome = PixelColors.white,
            content = PixelColors.notionSurface,
            contentAlt = colorHex("#ECEBE8"),
            onContent = PixelColors.notionText,
            border = PixelColors.notionText,
            primary = PixelColors.notionText,
            onPrimary = PixelColors.white,
            accent = colorHex("#57534E"),
            onAccent = PixelColors.white,
        )
        CardTheme.SUMMER -> NnoworkPalette(
            page = PixelColors.summerBackground,
            onPage = PixelColors.summerText,
            chrome = PixelColors.summerPrimary,
            onChrome = PixelColors.white,
            content = PixelColors.summerSurface,
            contentAlt = colorHex("#D6F1F7"),
            onContent = PixelColors.summerText,
            border = colorHex("#A8DAE4"),
            primary = PixelColors.summerPrimary,
            onPrimary = PixelColors.white,
            accent = PixelColors.summerAccent,
            onAccent = colorHex("#04323C"),
        )
        CardTheme.PANDA -> NnoworkPalette(
            page = PixelColors.pandaBackground,
            onPage = PixelColors.pandaText,
            chrome = PixelColors.pandaPrimary,
            onChrome = PixelColors.white,
            content = PixelColors.pandaSurface,
            contentAlt = colorHex("#E8E8E5"),
            onContent = PixelColors.pandaText,
            border = colorHex("#C9C9C4"),
            primary = PixelColors.pandaPrimary,
            onPrimary = PixelColors.white,
            accent = PixelColors.pandaAccent,
            onAccent = PixelColors.white,
        )
        CardTheme.SLOTH -> NnoworkPalette(
            page = PixelColors.slothBackground,
            onPage = PixelColors.slothText,
            chrome = PixelColors.slothPrimary,
            onChrome = PixelColors.white,
            content = PixelColors.slothSurface,
            contentAlt = colorHex("#E7EEDF"),
            onContent = PixelColors.slothText,
            border = colorHex("#CFC0A5"),
            primary = PixelColors.slothPrimary,
            onPrimary = PixelColors.white,
            accent = PixelColors.slothAccent,
            onAccent = colorHex("#2C2417"),
        )
        CardTheme.NIGHT -> NnoworkPalette(
            page = PixelColors.nightSkyTop,
            onPage = PixelColors.nightStarWhite,
            chrome = PixelColors.nightSurface,
            onChrome = PixelColors.nightStarWhite,
            content = PixelColors.nightSurface,
            contentAlt = PixelColors.nightBanner,
            onContent = PixelColors.nightStarWhite,
            border = PixelColors.nightCardBorder,
            primary = PixelColors.nightGold,
            onPrimary = PixelColors.nightSkyTop,
            accent = PixelColors.nightGold,
            onAccent = PixelColors.nightSkyTop,
        )
    }

/**
 * 由 NnoworkColors 產生 M3 colorScheme。方向 B 的角色映射：
 * - surface／surfaceContainer*／surfaceVariant 對應 surface／elevatedSurface（淺色主題 = 白），
 *   所以 AlertDialog、ModalBottomSheet 預設就是淺底，不會再出現「淺頁＋深對話框」。
 * - 深色導覽色只給 NavigationBar（tabBarBG），不再塞進 surface。
 * - secondaryContainer = accentSoft：M3 NavigationBar 的 indicator 預設取這個角色。
 */
internal fun NnoworkColors.toColorScheme(light: Boolean) = if (light) {
    lightColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = accentSoft, onPrimaryContainer = accentForeground,
        secondary = accent, onSecondary = onAccent,
        secondaryContainer = accentSoft, onSecondaryContainer = accentForeground,
        tertiary = accentSoft, onTertiary = primaryText,
        tertiaryContainer = accentSoft, onTertiaryContainer = primaryText,
        background = appBG, onBackground = primaryText,
        surface = surface, onSurface = primaryText,
        surfaceVariant = surface, onSurfaceVariant = primaryText,
        surfaceTint = Color.Transparent,
        surfaceDim = appBG, surfaceBright = elevatedSurface,
        surfaceContainerLowest = elevatedSurface, surfaceContainerLow = elevatedSurface,
        surfaceContainer = elevatedSurface, surfaceContainerHigh = elevatedSurface,
        surfaceContainerHighest = elevatedSurface,
        outline = cardBorder, outlineVariant = divider,
        error = danger, onError = onStrong,
        scrim = scrim,
    )
} else {
    darkColorScheme(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = accentSoft, onPrimaryContainer = accentForeground,
        secondary = accent, onSecondary = onAccent,
        secondaryContainer = accentSoft, onSecondaryContainer = accentForeground,
        tertiary = accentSoft, onTertiary = primaryText,
        tertiaryContainer = accentSoft, onTertiaryContainer = primaryText,
        background = appBG, onBackground = primaryText,
        surface = surface, onSurface = primaryText,
        surfaceVariant = surface, onSurfaceVariant = primaryText,
        surfaceTint = Color.Transparent,
        surfaceDim = appBG, surfaceBright = elevatedSurface,
        surfaceContainerLowest = surface, surfaceContainerLow = surface,
        surfaceContainer = elevatedSurface, surfaceContainerHigh = elevatedSurface,
        surfaceContainerHighest = elevatedSurface,
        outline = cardBorder, outlineVariant = divider,
        error = danger, onError = readableOn(danger, PixelColors.black, onStrong),
        scrim = scrim,
    )
}

@Composable
fun NnoworkTheme(
    cardTheme: CardTheme = CardTheme.RPG,
    /** 預覽其他主題（主題商店 sheet）時傳 false，避免改到系統列圖示深淺。 */
    applySystemBars: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tokens = cardTheme.nnoworkColors
    val view = LocalView.current
    val colors = tokens.toColorScheme(light = cardTheme.usesAppWideLightPalette)
    if (applySystemBars && !view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = tokens.appBG.luminance() > 0.5f
                isAppearanceLightNavigationBars = tokens.tabBarBG.luminance() > 0.5f
            }
        }
    }
    CompositionLocalProvider(
        LocalNnoworkColors provides tokens,
        LocalNnoworkTypeScale provides cardTheme.typeScale,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = NnoworkM3Typography,
            shapes = NnoworkShapes,
            content = content,
        )
    }
}
