package com.neilyin.nnowork.feature.history

import androidx.activity.compose.BackHandler
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle

@Composable
fun FreedomScreen(
    companyName: String,
    completedCards: Int,
    totalStamps: Int,
    onViewHistory: () -> Unit,
    onStartNewCompany: () -> Unit,
) {
    var confirmRestart by remember { mutableStateOf(false) }
    BackHandler(onBack = onViewHistory)

    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("重新開始？", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark)) },
            text = { Text("會清除集點紀錄與離職計畫，金幣和已解鎖主題會保留。", style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.textOnDark)) },
            confirmButton = {
                PixelButton(onClick = onStartNewCompany, backgroundColor = NnoworkTheme.colors.danger) {
                    Text("確認重新開始", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
                }
            },
            dismissButton = {
                PixelButton(onClick = { confirmRestart = false }, backgroundColor = NnoworkTheme.colors.disabledBG) {
                    Text("取消", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NnoworkSpacing.lg, vertical = NnoworkSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.lg),
    ) {
        Icon(Icons.Filled.Celebration, "完成離職集點", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(82.dp))
        Text("恭喜你！", style = pixelTextStyle(NnoworkTheme.type.display, FontWeight.Bold, MaterialTheme.colorScheme.onPrimary))
        Text("祝你奔向自由！", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, MaterialTheme.colorScheme.onPrimary))

        Text(
            "再見了，${companyName.ifBlank { "公司" }}！",
            style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.primaryText),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(NnoworkLine.emphasis, MaterialTheme.colorScheme.outline, RectangleShape)
                .padding(horizontal = NnoworkSpacing.roomy, vertical = NnoworkSpacing.sm),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
            FreedomStat(Icons.Filled.GridView, completedCards.toString(), "完成卡片", Modifier.weight(1f))
            FreedomStat(Icons.Filled.CheckCircle, totalStamps.toString(), "總蓋章數", Modifier.weight(1f))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            Text("願你找到更好的歸屬", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, MaterialTheme.colorScheme.onPrimary))
            Text("人生苦短，不要委屈自己", style = pixelTextStyle(NnoworkTheme.type.body, color = MaterialTheme.colorScheme.onPrimary))
        }

        Spacer(Modifier.height(NnoworkSpacing.xxs))
        PixelButton(onClick = onViewHistory, backgroundColor = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = NnoworkTheme.colors.primaryText)
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text("查看離職紀錄", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
        PixelButton(onClick = { confirmRestart = true }, backgroundColor = NnoworkTheme.colors.success, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.RestartAlt, null, tint = NnoworkTheme.colors.primaryText)
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text("重新集點（新公司）", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
    }
}

@Composable
private fun FreedomStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.pixelCard().padding(vertical = NnoworkSpacing.roomy, horizontal = NnoworkSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight),
    ) {
        Icon(icon, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(30.dp))
        Text(value, style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
    }
}
