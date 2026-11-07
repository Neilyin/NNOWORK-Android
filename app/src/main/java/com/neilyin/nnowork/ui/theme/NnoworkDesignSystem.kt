package com.neilyin.nnowork.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * NNOWORK 的共用視覺節奏。畫面應優先使用這些 token，避免各自長出相近但不同的數值。
 */
object NnoworkSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp

    // 以下是 feature 內重複最多、但不在 4/8/12… 等差級距上的值（保留以維持各畫面節奏）。
    /** 6dp */
    val tight = 6.dp
    /** 10dp */
    val snug = 10.dp
    /** 14dp */
    val cozy = 14.dp
    /** 18dp，目前多數分頁的區塊間距。 */
    val roomy = 18.dp
    /** 20dp：全 App 分頁的水平內距。 */
    val page = 20.dp
}

/** 圓角，數值對齊 iOS PixelRadius（reui）。 */
object NnoworkRadius {
    /** 小標籤、徽章、集點格。 */
    val small = 6.dp
    /** 輸入框。 */
    val field = 8.dp
    /** 按鈕。 */
    val button = 10.dp
    /** 卡片、區塊。 */
    val card = 12.dp
    /** 首頁集點卡等主視覺、對話框。 */
    val hero = 20.dp

    // 舊名稱：保留給既有呼叫點，值已收斂到上面五級。
    val compact = small
    val control = field
    val chip = button
    val tile = card
    /** Bottom sheet 上緣。 */
    val sheet = hero
}

/** 線寬，對齊 iOS PixelLine：卡片 hairline、控制項 1.5、粗框只給集點卡主視覺。 */
object NnoworkLine {
    val hairline = 1.dp
    val control = 1.5.dp
    val emphasis = 3.dp
}

/** 卡片 hairline 外框的透明度（iOS PixelCardModifier 0.45）。 */
const val NnoworkCardBorderAlpha = 0.45f

/** 柔和投影參數（iOS：radius 10、y 3；淺卡 6% 黑、深卡 28% 黑）。 */
object NnoworkShadow {
    val radius = 10.dp
    val offsetY = 3.dp
    const val lightAlpha = 0.06f
    const val darkAlpha = 0.28f
}

/** 舊的 Material 陰影高度；> 0 代表「要柔和投影」，實際陰影見 [NnoworkShadow]。 */
object NnoworkElevation {
    val control = 3.dp
    val card = 4.dp
    val hero = 7.dp
}

object NnoworkSize {
    val minTouchTarget = 48.dp
    /** 按鈕與輸入框的最小高度。 */
    val controlHeight = 48.dp
    val icon = 24.dp
    val compactIcon = 18.dp
    /** 多行輸入框（例如留言）的最大高度，超過在框內捲動。約 6 行內文。 */
    val multilineFieldMaxHeight = 160.dp
    val scrollbarWidth = 3.dp
    val scrollbarMinThumb = 16.dp
    /** 時間軸連接線等細分隔線的寬度。 */
    val timelineConnector = 2.dp
}

object NnoworkMotion {
    const val fast = 150
    const val normal = 220
    const val emphasis = 300
}

/**
 * 標準輸入框（有浮動標籤）。底色／文字／框線走 input* token，圓角 field 8。
 */
@Composable
fun NnoworkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = NnoworkTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        placeholder = placeholder?.let {
            { Text(it, style = MaterialTheme.typography.bodyLarge) }
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(NnoworkRadius.field),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.inputBG,
            unfocusedContainerColor = colors.inputBG,
            disabledContainerColor = colors.inputBG.copy(alpha = 0.72f),
            focusedTextColor = colors.inputText,
            unfocusedTextColor = colors.inputText,
            disabledTextColor = colors.inputText.copy(alpha = 0.55f),
            cursorColor = colors.accentForeground,
            focusedBorderColor = colors.accentForeground,
            unfocusedBorderColor = colors.inputBorder,
            focusedLabelColor = colors.accentForeground,
            unfocusedLabelColor = colors.secondaryText,
            focusedPlaceholderColor = colors.secondaryText,
            unfocusedPlaceholderColor = colors.secondaryText,
            errorBorderColor = colors.danger,
            errorLabelColor = colors.danger,
            errorCursorColor = colors.danger,
        ),
    )
}

/**
 * 無浮動標籤的精簡輸入框（取代原本 6 個寫死白底＋3dp 黑框的 BasicTextField）。
 * 外觀與 [NnoworkTextField] 同一套：input* token、field 圓角、1.5dp 框、聚焦時框轉 accentForeground。
 * [minHeight] 給多行備註用；單行時維持 controlHeight。
 */
@Composable
fun NnoworkField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    minHeight: Dp = NnoworkSize.controlHeight,
    /**
     * 多行時框的最大高度；內容超過就在框內捲動，右側顯示捲軸（例如不限字數的留言）。
     * 未指定時框會跟著內容一直長高。
     */
    maxHeight: Dp = Dp.Unspecified,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    /** true 時外框改成 danger（例如字數超過）。 */
    isError: Boolean = false,
) {
    val colors = NnoworkTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(NnoworkRadius.field)
    val scrollable = !singleLine && maxHeight != Dp.Unspecified
    val scrollState = rememberScrollState()
    val scrollbarColor = colors.secondaryText.copy(alpha = 0.6f)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle.merge(TextStyle(color = colors.inputText)),
        cursorBrush = SolidColor(colors.accentForeground),
        keyboardOptions = keyboardOptions,
        interactionSource = interaction,
        modifier = modifier
            .heightIn(min = minHeight, max = if (scrollable) maxHeight else Dp.Infinity)
            .alpha(if (enabled) 1f else 0.6f)
            .clip(shape)
            .background(colors.inputBG, shape)
            .border(
                NnoworkLine.control,
                when {
                    isError -> colors.danger
                    focused -> colors.accentForeground
                    else -> colors.inputBorder
                },
                shape,
            )
            .then(if (scrollable) Modifier.fieldScrollbar(scrollState, scrollbarColor) else Modifier)
            .padding(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.sm),
        decorationBox = { inner ->
            Box(
                modifier = if (scrollable) Modifier.verticalScroll(scrollState) else Modifier,
                contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = textStyle.merge(TextStyle(color = colors.secondaryText)))
                }
                inner()
            }
        },
    )
}

/** 多行輸入框的捲軸：內容超出可見高度時，在右緣畫一條細捲軸，讓使用者看得出框內可以捲。 */
private fun Modifier.fieldScrollbar(state: ScrollState, color: Color): Modifier = drawWithContent {
    drawContent()
    if (state.maxValue <= 0) return@drawWithContent
    val viewport = size.height
    val total = viewport + state.maxValue
    val thumbHeight = (viewport * viewport / total).coerceAtLeast(NnoworkSize.scrollbarMinThumb.toPx())
    val inset = NnoworkSpacing.xxs.toPx()
    val top = inset + (viewport - thumbHeight - inset * 2) * state.value / state.maxValue
    val width = NnoworkSize.scrollbarWidth.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - width - inset, top),
        size = Size(width, thumbHeight),
        cornerRadius = CornerRadius(width / 2, width / 2),
    )
}

/** 可點的「假輸入框」（時間選擇、下拉）共用的外觀，和 [NnoworkField] 一致。 */
@Composable
fun Modifier.fieldSurface(): Modifier {
    val colors = NnoworkTheme.colors
    val shape = RoundedCornerShape(NnoworkRadius.field)
    return this
        .clip(shape)
        .background(colors.inputBG, shape)
        .border(NnoworkLine.control, colors.inputBorder, shape)
}

/** Switch 統一配色：開啟時 accent 軌道＋onAccent 圓鈕。 */
@Composable
fun nnoworkSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = NnoworkTheme.colors.onAccent,
    checkedTrackColor = NnoworkTheme.colors.accent,
    checkedBorderColor = NnoworkTheme.colors.accent,
    uncheckedThumbColor = NnoworkTheme.colors.secondaryText,
    uncheckedTrackColor = NnoworkTheme.colors.disabledBG,
    uncheckedBorderColor = NnoworkTheme.colors.border,
)

/** Checkbox 統一配色。 */
@Composable
fun nnoworkCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = NnoworkTheme.colors.accent,
    checkmarkColor = NnoworkTheme.colors.onAccent,
    uncheckedColor = NnoworkTheme.colors.border,
)

/** 對話框統一外觀：elevatedSurface 底、hero 圓角、主要文字色。 */
object NnoworkDialogDefaults {
    val shape: Shape = RoundedCornerShape(NnoworkRadius.hero)
    val containerColor: Color
        @Composable @ReadOnlyComposable get() = NnoworkTheme.colors.dialogBG
    val contentColor: Color
        @Composable @ReadOnlyComposable get() = NnoworkTheme.colors.onElevated
}

/** Bottom sheet 統一外觀：elevatedSurface 底、上緣 sheet 圓角、柔和把手。 */
object NnoworkSheetDefaults {
    val shape: Shape = RoundedCornerShape(topStart = NnoworkRadius.sheet, topEnd = NnoworkRadius.sheet)
    val containerColor: Color
        @Composable @ReadOnlyComposable get() = NnoworkTheme.colors.elevatedSurface
    val scrimColor: Color
        @Composable @ReadOnlyComposable get() = NnoworkTheme.colors.scrim

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    fun DragHandle() {
        BottomSheetDefaults.DragHandle(color = NnoworkTheme.colors.secondaryText.copy(alpha = 0.5f))
    }
}

/** 分頁大標題：全 App 同一個字級（pageTitle 28）與字型（等寬粗體）。 */
@Composable
fun NnoworkPageTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NnoworkTheme.colors.primaryText,
) {
    Text(
        text,
        modifier = modifier,
        style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, color, monospace = true),
    )
}

/** 將任意色壓到足以作為柔和陰影的透明度。 */
internal fun Color.asPixelShadow(): Color = copy(alpha = if (alpha < 0.24f) alpha else 0.24f)

/**
 * 引導、資料找回等獨立頁面的安全容器：小螢幕可捲動，大螢幕限制閱讀寬度。
 */
@Composable
fun NnoworkCenteredPage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NnoworkSpacing.lg, vertical = NnoworkSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
