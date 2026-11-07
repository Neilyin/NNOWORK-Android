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
}

object NnoworkRadius {
    val compact = 4.dp
    val control = 8.dp
    val card = 12.dp
    val hero = 20.dp
}

object NnoworkElevation {
    val control = 3.dp
    val card = 4.dp
    val hero = 7.dp
}

object NnoworkSize {
    val minTouchTarget = 48.dp
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
    val colors = MaterialTheme.colorScheme
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
            focusedContainerColor = colors.surfaceVariant,
            unfocusedContainerColor = colors.surfaceVariant,
            disabledContainerColor = colors.surfaceVariant.copy(alpha = 0.72f),
            focusedTextColor = colors.onSurfaceVariant,
            unfocusedTextColor = colors.onSurfaceVariant,
            disabledTextColor = colors.onSurfaceVariant.copy(alpha = 0.55f),
            cursorColor = colors.primary,
            focusedBorderColor = colors.outline,
            unfocusedBorderColor = colors.outline.copy(alpha = 0.72f),
            focusedLabelColor = colors.onSurfaceVariant,
            unfocusedLabelColor = colors.onSurfaceVariant.copy(alpha = 0.78f),
            focusedPlaceholderColor = colors.onSurfaceVariant.copy(alpha = 0.55f),
            unfocusedPlaceholderColor = colors.onSurfaceVariant.copy(alpha = 0.55f),
            errorBorderColor = colors.error,
            errorLabelColor = colors.error,
            errorCursorColor = colors.error,
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
