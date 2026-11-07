package com.neilyin.nnowork.ui.theme

import androidx.compose.foundation.background
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

    // 以下是 feature 內重複最多、但不在 4/8/12… 等差級距上的值（Phase A 保留以維持外觀）。
    // Phase 3 要改：評估收斂成 4/8/12/16/24/32 六級，並統一頁面水平內距（目前 24/20/16 並存）。
    /** 6dp */
    val tight = 6.dp
    /** 10dp */
    val snug = 10.dp
    /** 14dp */
    val cozy = 14.dp
    /** 18dp，目前多數分頁的區塊間距。 */
    val roomy = 18.dp
    /** 20dp，目前計畫／紀錄／設定的頁面水平內距。 */
    val page = 20.dp
}

/**
 * 圓角。對齊 iOS PixelRadius 的角色名（card/button/field/small/hero）。
 * Phase 3 要改：iOS 是 card 12／button 10／field 8／small 6／hero 20；
 * Android 目前 button=8、small=4（compact）。
 */
object NnoworkRadius {
    val compact = 4.dp
    val control = 8.dp
    val card = 12.dp
    val hero = 20.dp

    val small = compact
    val button = control
    val field = control

    // Phase A 保留的版面專屬圓角（ThemeShop 預覽、蓋章輸入、首頁完成對話框）。
    val chip = 10.dp
    val tile = 14.dp
    val sheet = 16.dp
}

/** 線寬。Phase 3 要改：iOS 是 hairline 1／control 1.5／emphasis 3，且實心按鈕不加框。 */
object NnoworkLine {
    val hairline = 1.dp
    val control = 2.dp
    val emphasis = 3.dp
}

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
}

object NnoworkMotion {
    const val fast = 150
    const val normal = 220
    const val emphasis = 300
}

/**
 * 淺色內容面的標準輸入框。標籤永遠可見，避免只靠 placeholder 傳達欄位用途。
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
        shape = RoundedCornerShape(NnoworkRadius.control),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            disabledContainerColor = colors.surface.copy(alpha = 0.72f),
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            disabledTextColor = colors.onSurface.copy(alpha = 0.55f),
            cursorColor = colors.primary,
            focusedBorderColor = colors.cardBorder,
            unfocusedBorderColor = colors.cardBorder.copy(alpha = 0.72f),
            focusedLabelColor = colors.onSurface,
            unfocusedLabelColor = colors.onSurface.copy(alpha = 0.78f),
            focusedPlaceholderColor = colors.onSurface.copy(alpha = 0.55f),
            unfocusedPlaceholderColor = colors.onSurface.copy(alpha = 0.55f),
            errorBorderColor = colors.danger,
            errorLabelColor = colors.danger,
            errorCursorColor = colors.danger,
        ),
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
