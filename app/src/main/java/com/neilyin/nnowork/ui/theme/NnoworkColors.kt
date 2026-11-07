package com.neilyin.nnowork.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 語意色 token。欄位名對齊 iOS AppThemeManager（PixelStyleModifiers.swift），
 * 讓兩端未來可以一對一換值。
 *
 * Phase A（token 化）：每個值都等於「現在畫面實際呈現的顏色」，外觀不變。
 * 標記 `Phase 3 要改` 的值是已知有對比或底色錯置問題、但這一期刻意不修的欄位。
 */
@Immutable
data class NnoworkColors(
    // ── 底色與面 ──────────────────────────────────────────────
    /** 頁面底色（= palette.page）。 */
    val appBG: Color,
    /** 卡片／內容面（= palette.content）。 */
    val surface: Color,
    /** 輸入框、白色浮層面。Phase 3 要改：iOS 淺色主題是 elevatedSurface = white，深色主題是深一階。 */
    val elevatedSurface: Color,
    /** 對話框底色（= palette.chrome 深色）。Phase 3 要改：淺色主題的對話框不該是深色（P3）。 */
    val dialogBG: Color,

    // ── 文字 ──────────────────────────────────────────────────
    /** 內容卡上的主要文字。Phase 3 要改：目前是固定的 8-bit 黑，應跟主題 onContent。 */
    val primaryText: Color,
    /** 次要文字。Phase 3 要改：#95A5A6 對淺底只有 2.42:1，未達 AA。 */
    val secondaryText: Color,
    /** 頁面底色上的文字（= palette.onPage）。 */
    val onAppBG: Color,
    /** 主題化內容面（surface）上的文字，= palette.onContent；M3 的 onSurfaceVariant 對應。 */
    val onSurface: Color,
    /** 比 secondaryText 更深的輔助文字（說明、欄位標籤）。 */
    val mutedText: Color,
    /** 深色 chrome／對話框／彩色按鈕上的文字（固定 8-bit 白）。Phase 3 要改：白字配藍/綠/橘按鈕對比不足。 */
    val textOnDark: Color,

    // ── 線條 ──────────────────────────────────────────────────
    val divider: Color,
    /** 粗體硬框（固定 8-bit 黑）。Phase 3 要改：疊在深色 chrome 上對比 1.03:1。 */
    val border: Color,
    /** 主題化卡片框（= palette.border）。 */
    val cardBorder: Color,

    // ── 強調 ──────────────────────────────────────────────────
    val accent: Color,
    /** 強調色當文字／icon 用。Phase 3 要改：目前直接等於 accent，亮色強調在淺底不夠對比。 */
    val accentForeground: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val primary: Color,
    val onPrimary: Color,

    // ── 按鈕 ──────────────────────────────────────────────────
    /** 預設按鈕底色。Phase 3 要改：iOS 是 accent；Android 目前固定 8-bit 藍。 */
    val buttonBG: Color,
    val buttonText: Color,
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

    // ── 狀態／功能色（原 8-bit 原色收編）────────────────────────
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    /** 選取／確認用的黃（原 PixelColors.yellow）。 */
    val highlight: Color,
    /** 大師／特別等級的紫（原 PixelColors.purple）。 */
    val special: Color,
    val teal: Color,
    /** 班別顏色解析失敗時的備援色（原 PlanScreen 內的 0xFF4A90D9）。 */
    val shiftFallback: Color,

    val treeHole: NnoworkTreeHoleColors,
)

/**
 * 樹洞模組自帶的藍／黃／紅色系（原本硬編碼在 treehole 套件）。
 * Phase 3 要改：這組顏色不會跟主題走，在 Notion／Sakura 下會出現亮藍提問卡。
 */
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

private val TreeHoleLegacyColors = NnoworkTreeHoleColors(
    questionAccent = Color(0xFF6BB8FA),
    questionAccentDark = Color(0xFF0B5FA5),
    questionSoft = Color(0xFFE5F3FF),
    questionTitle = Color(0xFF063B63),
    questionBody = Color(0xFF24516F),
    errorSoft = Color(0xFFFFE7E7),
    errorBorder = Color(0xFF9A1F1F),
    errorText = Color(0xFF711515),
    noticeSoft = Color(0xFFFFF1B8),
    noticeStroke = Color(0xFF7A5000),
    noticeText = Color(0xFF624000),
    tierRookie = Color(0xFF455A64),
    tierPasserby = Color(0xFF2E7D32),
    tierSenior = Color(0xFF0D5BA7),
    tierStrategist = Color(0xFF6A1B9A),
    tierMaster = Color(0xFF7A5000),
)

/** 每個主題的完整語意色。Phase A 中，與主題無關的欄位在 9 個主題相同。 */
val CardTheme.nnoworkColors: NnoworkColors
    get() {
        val p = palette
        return NnoworkColors(
            appBG = p.page,
            onAppBG = p.onPage,
            surface = p.content,
            elevatedSurface = PixelColors.white,
            dialogBG = p.chrome,
            onSurface = p.onContent,
            primaryText = PixelColors.black,
            secondaryText = PixelColors.gray,
            mutedText = PixelColors.darkGray,
            textOnDark = PixelColors.white,
            divider = PixelColors.gray.copy(alpha = 0.3f),
            border = PixelColors.black,
            cardBorder = p.border,
            accent = p.accent,
            accentForeground = p.accent,
            accentSoft = p.contentAlt,
            onAccent = p.onAccent,
            primary = p.primary,
            onPrimary = p.onPrimary,
            buttonBG = PixelColors.blue,
            buttonText = PixelColors.black,
            disabledBG = PixelColors.gray,
            disabledText = PixelColors.black,
            navBarBG = p.chrome,
            navBarText = p.onChrome,
            tabBarBG = p.chrome,
            tabSelected = p.onPrimary,
            tabUnselected = p.onChrome.copy(alpha = 0.68f),
            tabIndicator = p.primary,
            inputBG = PixelColors.white,
            inputText = PixelColors.black,
            inputBorder = PixelColors.black,
            success = PixelColors.green,
            warning = PixelColors.orange,
            danger = PixelColors.red,
            info = PixelColors.blue,
            highlight = PixelColors.yellow,
            special = PixelColors.purple,
            teal = PixelColors.turquoise,
            shiftFallback = Color(0xFF4A90D9),
            treeHole = TreeHoleLegacyColors,
        )
    }
