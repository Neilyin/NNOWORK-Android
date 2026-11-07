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

private val PixelTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

private val NnoworkShapes = Shapes(
    extraSmall = RoundedCornerShape(NnoworkRadius.compact),
    small = RoundedCornerShape(NnoworkRadius.control),
    medium = RoundedCornerShape(NnoworkRadius.card),
    large = RoundedCornerShape(NnoworkRadius.hero),
    extraLarge = RoundedCornerShape(NnoworkRadius.hero),
)

/**
 * 全 App 語意色。內容卡固定維持淺底，讓既有像素風黑字與功能色按鈕保有對比；
 * chrome 則是主題化深色，供底部導覽、對話框與 Widget 使用。
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
    }

@Composable
fun NnoworkTheme(
    cardTheme: CardTheme = CardTheme.RPG,
    content: @Composable () -> Unit,
) {
    val palette = cardTheme.palette
    val tokens = cardTheme.nnoworkColors
    val view = LocalView.current
    val colors = if (cardTheme.isLightTheme) {
        lightColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            secondary = tokens.accent,
            onSecondary = tokens.onAccent,
            tertiary = tokens.accentSoft,
            onTertiary = tokens.onSurface,
            background = tokens.appBG,
            onBackground = tokens.onAppBG,
            surface = tokens.dialogBG,
            onSurface = tokens.navBarText,
            surfaceVariant = tokens.surface,
            onSurfaceVariant = tokens.onSurface,
            outline = tokens.cardBorder,
            error = tokens.danger,
            onError = tokens.textOnDark,
        )
    } else {
        darkColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            secondary = tokens.accent,
            onSecondary = tokens.onAccent,
            tertiary = tokens.accentSoft,
            onTertiary = tokens.onSurface,
            background = tokens.appBG,
            onBackground = tokens.onAppBG,
            surface = tokens.dialogBG,
            onSurface = tokens.navBarText,
            surfaceVariant = tokens.surface,
            onSurfaceVariant = tokens.onSurface,
            outline = tokens.cardBorder,
            error = tokens.danger,
            onError = tokens.textOnDark,
        )
    }
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = palette.page.luminance() > 0.5f
                isAppearanceLightNavigationBars = palette.chrome.luminance() > 0.5f
            }
        }
    }
    CompositionLocalProvider(
        LocalNnoworkColors provides tokens,
        LocalNnoworkTypeScale provides cardTheme.typeScale,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = PixelTypography,
            shapes = NnoworkShapes,
            content = content,
        )
    }
}
