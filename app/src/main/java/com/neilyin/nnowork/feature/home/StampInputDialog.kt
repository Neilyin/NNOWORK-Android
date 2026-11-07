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
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NnoworkTheme.colors.elevatedSurface)
                .statusBarsPadding(),
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
