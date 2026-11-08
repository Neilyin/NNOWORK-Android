package com.neilyin.nnowork.feature.home

import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.background
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import com.neilyin.nnowork.ui.adaptive.NnoworkAdaptive
import com.neilyin.nnowork.ui.adaptive.readableWidth
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkTextField
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelBadge
import com.neilyin.nnowork.ui.theme.pixelTextStyle

@Composable
fun StampInputDialog(
    theme: CardTheme,
    commonReasons: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, addToCommon: Boolean) -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    var addToCommon by rememberSaveable { mutableStateOf(false) }
    val trimmed = reason.trim()
    val canConfirm = trimmed.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        // 全螢幕表單：對話框視窗鋪滿整個螢幕（含狀態列），內容自己用 insets 讓開狀態列、手勢列與鍵盤。
        // 預設的浮動視窗排在狀態列下方，上方會露出背景遮罩的深灰帶，狀態列圖示深淺也不受控。
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val dialogView = LocalView.current
        val surface = NnoworkTheme.colors.elevatedSurface
        val lightBars = surface.luminance() > 0.5f
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.let { window ->
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                )
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION")
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.setGravity(android.view.Gravity.TOP)
                // 表單本身已鋪滿不透明底色；不要背景遮罩，避免系統重排視窗（例如鍵盤出現）時露出深灰帶。
                window.setDimAmount(0f)
                @Suppress("DEPRECATION")
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                WindowCompat.getInsetsController(window, dialogView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
        // 大螢幕：表單（含取消鈕）限寬置中，不全寬拉伸；手機寬度小於上限，外觀不變。
        Box(
            modifier = Modifier
                .readableWidth(NnoworkAdaptive.formMaxWidth)
                .fillMaxHeight()
                .padding(NnoworkSpacing.lg),
        ) {
            // 取消按鈕
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)) {
                Text("取消", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Medium, NnoworkTheme.colors.accentForeground))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(theme.sheetIcon, contentDescription = null, tint = NnoworkTheme.colors.accent, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(NnoworkSpacing.sm))
                Text(theme.sheetTitle, style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                Spacer(Modifier.height(NnoworkSpacing.lg))

                Text(
                    "說說你的理由吧",
                    style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.onElevated),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(NnoworkSpacing.sm))

                NnoworkTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "離職理由",
                    placeholder = "例如：主管又亂改需求…",
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                )

                Spacer(Modifier.height(NnoworkSpacing.sm))

                // 常用原因
                if (commonReasons.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
                    ) {
                        commonReasons.forEach { r ->
                            val selected = reason == r
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(NnoworkRadius.sheet))
                                    .background(
                                        if (selected) NnoworkTheme.colors.accent
                                        else NnoworkTheme.colors.disabledBG
                                    )
                                    .padding(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.tight),
                            ) {
                                TextButton(
                                    onClick = { reason = r },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                ) {
                                    Text(
                                        r,
                                        style = pixelTextStyle(
                                            NnoworkTheme.type.body,
                                            FontWeight.Medium,
                                            if (selected) NnoworkTheme.colors.onAccent else NnoworkTheme.colors.primaryText,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(NnoworkSpacing.sm))
                }

                // 加入常用原因
                if (trimmed.isNotEmpty() && !commonReasons.contains(trimmed)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = addToCommon,
                            onCheckedChange = { addToCommon = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NnoworkTheme.colors.accent,
                                checkmarkColor = NnoworkTheme.colors.onAccent,
                                uncheckedColor = NnoworkTheme.colors.border,
                            ),
                        )
                        Text("加入常用原因", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.onElevated))
                    }
                }

                Spacer(Modifier.height(NnoworkSpacing.xs))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PixelBadge(backgroundColor = NnoworkTheme.colors.disabledBG) {
                        Text("${reason.length} 字", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.onElevated))
                    }
                }

                Spacer(Modifier.height(NnoworkSpacing.lg))

                PixelButton(
                    onClick = { if (canConfirm) onConfirm(trimmed, addToCommon) },
                    // 與首頁「我要蓋章」同為主要按鈕（accent），避免進入表單後按鈕突然換色。
                    backgroundColor = if (canConfirm) NnoworkTheme.colors.buttonBG else NnoworkTheme.colors.disabledBG,
                    enabled = canConfirm,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val actionContentColor = LocalContentColor.current
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = actionContentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(NnoworkSpacing.xs))
                        Text("蓋章！", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, actionContentColor))
                    }
                }
            }
        }
        }
    }
}
