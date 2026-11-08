package com.neilyin.nnowork.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * 語意色 token。欄位名與色值對齊 iOS AppThemeManager（PixelStyleModifiers.swift:490-728，
 * reui 精修版）。Phase B「沉穩原生」：
 * - 淺色全 App 主題（usesAppWideLightPalette）surface／elevatedSurface 用白（樹懶用米白）。
 * - 深色主題（RPG／系統錯誤／度假／夜空）內容卡是深一階的 surface，不再強制淺底。
 * - 8-bit 原色（PixelColors.yellow/green/blue…）只留給集點格、印章與慶祝畫面（[celebrate*]）。
 * - 所有文字與其底色對比 ≥ 4.5:1，由 NnoworkTokensTest 鎖定。
 */
@Immutable
data class NnoworkColors(
    // ── 底色與面 ──────────────────────────────────────────────
    /** 頁面底色。 */
    val appBG: Color,
    /** 頁面底色漸層的下緣（只有夜空是漸層，其餘 = appBG）。 */
    val appBGBottom: Color,
    /** 卡片／列表項底色。 */
    val surface: Color,
    /** Sheet、對話框、浮動卡片等比 surface 更突出的面。淺色主題 = 白。 */
    val elevatedSurface: Color,
    /** 對話框底色（= elevatedSurface）。 */
    val dialogBG: Color,

    // ── 文字 ──────────────────────────────────────────────────
    /** 主要文字（appBG／surface／elevatedSurface 上皆 ≥ 4.5:1）。 */
    val primaryText: Color,
    /** 次要文字（appBG／surface 上皆 ≥ 4.5:1）。 */
    val secondaryText: Color,
    /** 頁面底色上的文字（= primaryText）。 */
    val onAppBG: Color,
    /** surface 上的文字（= primaryText；M3 onSurface／onSurfaceVariant）。 */
    val onSurface: Color,
    /** 介於主要與次要之間的輔助文字（欄位標籤、說明）。 */
    val mutedText: Color,
    /** 對話框／sheet（elevatedSurface）上的文字。原 Phase A 的 textOnDark：對話框不再是深色，所以 = primaryText。 */
    val onElevated: Color,
    /** 飽和填色（紫色大師徽章、班別色塊等）上的白字。 */
    val onStrong: Color,

    // ── 線條 ──────────────────────────────────────────────────
    val divider: Color,
    /** 控制項外框（輸入框、次要按鈕）。 */
    val border: Color,
    /** 卡片 hairline 外框原色；pixelCard 會再乘 45% 透明度。 */
    val cardBorder: Color,

    // ── 強調 ──────────────────────────────────────────────────
    val accent: Color,
    /** 強調色當文字／icon 用（淺色主題 = primaryText，亮粉亮橘只當填色）。 */
    val accentForeground: Color,
    /** 選取狀態、徽章、導覽 indicator 的低彩度底。 */
    val accentSoft: Color,
    val onAccent: Color,
    val primary: Color,
    val onPrimary: Color,

    // ── 按鈕 ──────────────────────────────────────────────────
    val buttonBG: Color,
    val buttonText: Color,
    /** 次要（tonal）按鈕／未選取 chip 的底色。 */
    val disabledBG: Color,
    val disabledText: Color,

    // ── 導覽 ──────────────────────────────────────────────────
    val navBarBG: Color,
    val navBarText: Color,
    val tabBarBG: Color,
    val tabSelected: Color,
    val tabUnselected: Color,
    val tabIndicator: Color,

    // ── 輸入框 ────────────────────────────────────────────────
    val inputBG: Color,
    val inputText: Color,
    val inputBorder: Color,

    // ── 狀態／功能色（已依主題深淺調成可當文字的版本）────────────
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    /** 選取／確認的強調（淺色主題 = accent，深色主題 = 8-bit 黃）。 */
    val highlight: Color,
    /** 大師／特別等級的紫。 */
    val special: Color,
    val teal: Color,
    /** 班別顏色解析失敗時的備援色。 */
    val shiftFallback: Color,
    /** 對話框、全螢幕遮罩。 */
    val scrim: Color,

    // ── 樹洞職稱徽章（iOS identityBadge*）────────────────────────
    val identityBadgeText: Color,
    val identityBadgeBG: Color,
    val identityBadgeBorder: Color,

    // ── 慶祝畫面專用的 8-bit 原色（不隨主題變）───────────────────
    val celebrateYellow: Color,
    val celebrateOrange: Color,
    val celebratePurple: Color,
    val celebrateGreen: Color,
    val celebrateBlue: Color,

    val treeHole: NnoworkTreeHoleColors,
)

/** 樹洞模組的提問／錯誤／提示／職級色。Phase B 改為跟主題走。 */
@Immutable
data class NnoworkTreeHoleColors(
    val questionAccent: Color,
    val questionAccentDark: Color,
    val questionSoft: Color,
    val questionTitle: Color,
    val questionBody: Color,
    val errorSoft: Color,
    val errorBorder: Color,
    val errorText: Color,
    val noticeSoft: Color,
    val noticeStroke: Color,
    val noticeText: Color,
    val tierRookie: Color,
    val tierPasserby: Color,
    val tierSenior: Color,
    val tierStrategist: Color,
    val tierMaster: Color,
)

/** WCAG 2.x 對比值（相對亮度公式）。 */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

/** 在 [background] 上可讀性最高的前景色（從候選中挑對比最大者）。 */
fun readableOn(background: Color, candidates: List<Color>): Color =
    candidates.maxBy { contrastRatio(it, background) }

fun readableOn(background: Color, a: Color, b: Color, c: Color? = null, d: Color? = null): Color =
    readableOn(background, listOfNotNull(a, b, c, d))

/** 以不透明色表示「前景 alpha 疊在底色上」的結果，方便對比計算。 */
private fun Color.over(background: Color, alpha: Float): Color =
    copy(alpha = alpha).compositeOver(background)

/** 主題的原始色值（iOS AppThemeManager），其餘欄位由這組推導。 */
private class ThemeBase(
    val light: Boolean,
    val appBG: Color,
    val surface: Color,
    val elevated: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val divider: Color,
    val border: Color,
    val cardBorder: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val danger: Color,
    val navBarBG: Color,
    val appBGBottom: Color = appBG,
)

private val DarkSurface = Color(red = 0.16f, green = 0.19f, blue = 0.24f)
/** iOS rgb(0.62,0.64,0.68) 在 darkGray 頁底只有 4.3:1；提亮到在頁底與 accentSoft 上都達 AA。 */
private val DarkSecondary = colorHex("#ADB2BA")
private val White = Color(0xFFFFFFFF)

private fun darkBase(accent: Color, cardBorder: Color) = ThemeBase(
    light = false,
    appBG = PixelColors.darkGray,
    surface = DarkSurface,
    elevated = colorHex("#323A48"),
    primaryText = White,
    secondaryText = DarkSecondary,
    divider = White.over(DarkSurface, 0.10f),
    border = colorHex("#5A6475"),
    cardBorder = cardBorder,
    accent = accent,
    // 12%：再高的話 secondaryText 在 accentSoft（選取卡、計時卡）上會掉到 AA 以下。
    accentSoft = accent.over(DarkSurface, 0.12f),
    onAccent = PixelColors.black,
    /** 8-bit 紅在深底當文字只有 3.5:1，改用淺一階的紅。 */
    danger = colorHex("#F28B82"),
    navBarBG = PixelColors.darkGray,
)

private val CardTheme.base: ThemeBase
    get() = when (this) {
        CardTheme.RPG -> darkBase(PixelColors.yellow, PixelColors.rpgComponentBorderSoft)
        CardTheme.CYBERPUNK -> darkBase(PixelColors.green, PixelColors.green)
        CardTheme.VACATION -> darkBase(PixelColors.turquoise, PixelColors.orange)
        CardTheme.SAKURA -> ThemeBase(
            light = true,
            appBG = PixelColors.sakuraBackground,
            surface = White,
            elevated = White,
            primaryText = PixelColors.sakuraText,
            secondaryText = PixelColors.sakuraSecondaryText,
            divider = PixelColors.sakuraDivider,
            border = PixelColors.sakuraControlBorderSoft,
            cardBorder = PixelColors.sakuraControlBorderSoft,
            accent = PixelColors.sakuraAccent,
            accentSoft = colorHex("#FBE3EC"),
            onAccent = White,
            danger = colorHex("#A12F49"),
            navBarBG = White,
        )
        CardTheme.LAZY_CAT -> ThemeBase(
            light = true,
            appBG = PixelColors.catBackground,
            surface = PixelColors.catSurface,
            elevated = White,
            primaryText = PixelColors.catText,
            secondaryText = PixelColors.catSecondaryText,
            divider = PixelColors.catDivider,
            border = PixelColors.catControlBorderSoft,
            cardBorder = PixelColors.catControlBorderSoft,
            accent = PixelColors.catAccent,
            accentSoft = colorHex("#FDE8CF"),
            onAccent = White,
            danger = colorHex("#9E3F35"),
            navBarBG = PixelColors.catSurface,
        )
        CardTheme.NOTION -> ThemeBase(
            light = true,
            appBG = PixelColors.notionBackground,
            surface = PixelColors.notionSurface,
            elevated = White,
            primaryText = PixelColors.notionText,
            secondaryText = PixelColors.notionSecondary,
            divider = PixelColors.notionBorder,
            border = PixelColors.notionControlBorderSoft,
            cardBorder = PixelColors.notionControlBorderSoft,
            accent = PixelColors.notionAccent,
            accentSoft = PixelColors.notionAccentSoft,
            onAccent = White,
            danger = colorHex("#B42318"),
            navBarBG = PixelColors.notionBackground,
        )
        CardTheme.SUMMER -> ThemeBase(
            light = true,
            appBG = PixelColors.summerBackground,
            surface = PixelColors.summerSurface,
            elevated = White,
            primaryText = PixelColors.summerText,
            secondaryText = PixelColors.summerSecondary,
            divider = PixelColors.summerDivider,
            border = PixelColors.summerControlBorder,
            cardBorder = PixelColors.summerControlBorder,
            accent = PixelColors.summerAccent,
            accentSoft = PixelColors.summerAccentSoft,
            onAccent = PixelColors.summerOnAccent,
            danger = PixelColors.summerDanger,
            navBarBG = PixelColors.summerSurface,
        )
        CardTheme.PANDA -> ThemeBase(
            light = true,
            appBG = PixelColors.pandaBackground,
            surface = PixelColors.pandaSurface,
            elevated = White,
            primaryText = PixelColors.pandaText,
            secondaryText = PixelColors.pandaSecondary,
            divider = PixelColors.pandaDivider,
            border = PixelColors.pandaControlBorder,
            cardBorder = PixelColors.pandaControlBorder,
            accent = PixelColors.pandaAccent,
            accentSoft = PixelColors.pandaAccentSoft,
            onAccent = White,
            danger = PixelColors.pandaDanger,
            navBarBG = PixelColors.pandaSurface,
        )
        CardTheme.SLOTH -> ThemeBase(
            light = true,
            appBG = PixelColors.slothBackground,
            surface = PixelColors.slothSurface,
            // 米白：純白在奶油底上太刺（iOS 同）。
            elevated = PixelColors.slothSurface,
            primaryText = PixelColors.slothText,
            secondaryText = PixelColors.slothSecondary,
            divider = PixelColors.slothDivider,
            border = PixelColors.slothControlBorder,
            cardBorder = PixelColors.slothControlBorder,
            accent = PixelColors.slothAccent,
            accentSoft = PixelColors.slothAccentSoft,
            onAccent = PixelColors.slothOnAccent,
            danger = PixelColors.slothDanger,
            navBarBG = PixelColors.slothSurface,
        )
        CardTheme.NIGHT -> ThemeBase(
            light = false,
            appBG = PixelColors.nightSkyTop,
            appBGBottom = PixelColors.nightSkyBottom,
            surface = PixelColors.nightSurface,
            elevated = PixelColors.nightElevated,
            primaryText = PixelColors.nightStarWhite,
            secondaryText = PixelColors.nightTextSoft,
            divider = PixelColors.nightStarWhite.over(PixelColors.nightSurface, 0.10f),
            border = PixelColors.nightStarWhite.over(PixelColors.nightSurface, 0.28f),
            cardBorder = PixelColors.nightStarWhite.over(PixelColors.nightSurface, 0.30f),
            accent = PixelColors.nightGold,
            accentSoft = PixelColors.nightBanner,
            onAccent = PixelColors.nightSkyTop,
            danger = PixelColors.nightCoral,
            navBarBG = PixelColors.nightSkyTop,
        )
    }

/** 每個主題的完整語意色。 */
val CardTheme.nnoworkColors: NnoworkColors
    get() {
        val b = base
        // 深色主題的 tonal 鈕要比 elevatedSurface（對話框底）再亮一階，否則在對話框裡看不出按鈕範圍。
        val secondaryButtonBG = if (b.light) b.accentSoft else White.over(b.elevated, 0.08f)
        val secondaryButtonText = b.primaryText
        val success = if (b.light) colorHex("#1E7A4C") else PixelColors.green
        val warning = if (b.light) colorHex("#9A5B00") else PixelColors.orange
        val info = if (b.light) colorHex("#1F6FA8") else PixelColors.blue
        val special = if (b.light) colorHex("#7B4A9A") else colorHex("#C79BDA")
        val teal = if (b.light) colorHex("#0F766E") else PixelColors.turquoise
        val highlight = if (b.light) b.accent else b.accent.takeIf { this == CardTheme.NIGHT } ?: PixelColors.yellow
        val accentForeground = if (b.light) b.primaryText else b.accent
        return NnoworkColors(
            appBG = b.appBG,
            appBGBottom = b.appBGBottom,
            surface = b.surface,
            elevatedSurface = b.elevated,
            dialogBG = b.elevated,
            primaryText = b.primaryText,
            secondaryText = b.secondaryText,
            onAppBG = b.primaryText,
            onSurface = b.primaryText,
            mutedText = b.primaryText.over(b.surface, 0.82f),
            onElevated = b.primaryText,
            onStrong = White,
            divider = b.divider,
            border = b.border,
            cardBorder = b.cardBorder,
            accent = b.accent,
            accentForeground = accentForeground,
            accentSoft = b.accentSoft,
            onAccent = b.onAccent,
            primary = b.accent,
            onPrimary = b.onAccent,
            buttonBG = b.accent,
            buttonText = b.onAccent,
            disabledBG = secondaryButtonBG,
            disabledText = secondaryButtonText,
            navBarBG = b.navBarBG,
            navBarText = b.primaryText,
            tabBarBG = if (b.light) White else if (this == CardTheme.NIGHT) PixelColors.nightSurface else PixelColors.darkGray,
            tabSelected = if (b.light) accentForeground else if (this == CardTheme.NIGHT) b.accent else White,
            tabUnselected = b.secondaryText,
            tabIndicator = b.accentSoft,
            inputBG = if (b.light) White else b.elevated,
            inputText = b.primaryText,
            inputBorder = b.border,
            success = success,
            warning = warning,
            danger = b.danger,
            info = info,
            highlight = highlight,
            special = special,
            teal = teal,
            shiftFallback = colorHex("#4A90D9"),
            scrim = PixelColors.black.copy(alpha = 0.56f),
            identityBadgeText = if (b.light) b.primaryText else highlight,
            identityBadgeBG = if (b.light) b.accentSoft else highlight.over(b.surface, 0.15f),
            identityBadgeBorder = if (b.light) b.accent else highlight.over(b.surface, 0.55f),
            celebrateYellow = PixelColors.yellow,
            celebrateOrange = PixelColors.orange,
            celebratePurple = PixelColors.purple,
            celebrateGreen = PixelColors.green,
            celebrateBlue = PixelColors.blue,
            treeHole = NnoworkTreeHoleColors(
                questionAccent = b.accent,
                questionAccentDark = accentForeground,
                questionSoft = b.accentSoft,
                questionTitle = b.primaryText,
                questionBody = b.secondaryText,
                errorSoft = b.danger.over(b.surface, 0.10f),
                errorBorder = b.danger,
                errorText = if (b.light) b.danger else b.primaryText,
                noticeSoft = warning.over(b.surface, 0.14f),
                noticeStroke = warning,
                noticeText = if (b.light) colorHex("#6B4000") else b.primaryText,
                tierRookie = if (b.light) colorHex("#455A64") else colorHex("#B0BEC5"),
                tierPasserby = if (b.light) colorHex("#2E7D32") else colorHex("#81C784"),
                tierSenior = if (b.light) colorHex("#0D5BA7") else colorHex("#90CAF9"),
                tierStrategist = if (b.light) colorHex("#6A1B9A") else colorHex("#CE93D8"),
                tierMaster = if (b.light) colorHex("#7A5000") else PixelColors.yellow,
            ),
        )
    }
