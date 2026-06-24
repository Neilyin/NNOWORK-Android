package com.neilyin.nnowork.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.PixelBadge
import com.neilyin.nnowork.ui.theme.pixelTextStyle

@Composable
fun StampInputDialog(
    theme: CardTheme,
    commonReasons: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, addToCommon: Boolean) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    var addToCommon by remember { mutableStateOf(false) }
    val trimmed = reason.trim()
    val canConfirm = trimmed.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PixelColors.darkGray)
                .statusBarsPadding()
                .padding(24.dp),
        ) {
            // 取消按鈕
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)) {
                Text("取消", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.white))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(theme.sheetIcon, contentDescription = null, tint = PixelColors.white, modifier = Modifier.size(60.dp))
                Spacer(Modifier.height(12.dp))
                Text(theme.sheetTitle, style = pixelTextStyle(24.sp, FontWeight.Bold, PixelColors.white))
                Spacer(Modifier.height(24.dp))

                Text(
                    "說說你的理由吧",
                    style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.white),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(PixelColors.white)
                        .border(3.dp, PixelColors.black, RectangleShape)
                        .padding(16.dp),
                ) {
                    BasicTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        textStyle = pixelTextStyle(18.sp, FontWeight.Medium, PixelColors.black),
                        cursorBrush = SolidColor(PixelColors.black),
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (reason.isEmpty()) {
                        Text(
                            "例如：主管又亂改需求...",
                            style = pixelTextStyle(14.sp, color = PixelColors.gray),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 常用原因
                if (commonReasons.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        commonReasons.forEach { r ->
                            val selected = reason == r
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selected) PixelColors.green.copy(alpha = 0.8f)
                                        else PixelColors.gray.copy(alpha = 0.3f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                TextButton(
                                    onClick = { reason = r },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                ) {
                                    Text(
                                        r,
                                        style = pixelTextStyle(
                                            14.sp,
                                            FontWeight.Medium,
                                            if (selected) PixelColors.black else PixelColors.white,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // 加入常用原因
                if (trimmed.isNotEmpty() && !commonReasons.contains(trimmed)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = addToCommon,
                            onCheckedChange = { addToCommon = it },
                            colors = CheckboxDefaults.colors(checkedColor = PixelColors.green),
                        )
                        Text("加入常用原因", style = pixelTextStyle(14.sp, FontWeight.Medium, PixelColors.white))
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PixelBadge(backgroundColor = PixelColors.gray) {
                        Text("${reason.length} 字", style = pixelTextStyle(14.sp, FontWeight.Medium, PixelColors.white))
                    }
                }

                Spacer(Modifier.height(24.dp))

                PixelButton(
                    onClick = { if (canConfirm) onConfirm(trimmed, addToCommon) },
                    backgroundColor = if (canConfirm) theme.accentColor else PixelColors.gray,
                    enabled = canConfirm,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = PixelColors.black, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("蓋章！", style = pixelTextStyle(22.sp, FontWeight.Bold, PixelColors.black))
                    }
                }
            }
        }
    }
}
