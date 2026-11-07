package com.neilyin.nnowork.feature.history

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
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
            title = { Text("重新開始？", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
            text = { Text("會清除集點紀錄與離職計畫，金幣和已解鎖主題會保留。", style = pixelTextStyle(14.sp, color = PixelColors.white)) },
            confirmButton = {
                PixelButton(onClick = onStartNewCompany, backgroundColor = PixelColors.red) {
                    Text("確認重新開始", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white))
                }
            },
            dismissButton = {
                PixelButton(onClick = { confirmRestart = false }, backgroundColor = PixelColors.gray) {
                    Text("取消", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
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
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Icon(Icons.Filled.Celebration, "完成離職集點", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(82.dp))
        Text("恭喜你！", style = pixelTextStyle(38.sp, FontWeight.Bold, MaterialTheme.colorScheme.onPrimary))
        Text("祝你奔向自由！", style = pixelTextStyle(25.sp, FontWeight.Bold, MaterialTheme.colorScheme.onPrimary))

        Text(
            "再見了，${companyName.ifBlank { "公司" }}！",
            style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.black),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(3.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FreedomStat(Icons.Filled.GridView, completedCards.toString(), "完成卡片", Modifier.weight(1f))
            FreedomStat(Icons.Filled.CheckCircle, totalStamps.toString(), "總蓋章數", Modifier.weight(1f))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("願你找到更好的歸屬", style = pixelTextStyle(17.sp, FontWeight.Bold, MaterialTheme.colorScheme.onPrimary))
            Text("人生苦短，不要委屈自己", style = pixelTextStyle(14.sp, color = MaterialTheme.colorScheme.onPrimary))
        }

        Spacer(Modifier.height(4.dp))
        PixelButton(onClick = onViewHistory, backgroundColor = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = PixelColors.black)
            Spacer(Modifier.size(8.dp))
            Text("查看離職紀錄", style = pixelTextStyle(17.sp, FontWeight.Bold, PixelColors.black))
        }
        PixelButton(onClick = { confirmRestart = true }, backgroundColor = PixelColors.green, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.RestartAlt, null, tint = PixelColors.black)
            Spacer(Modifier.size(8.dp))
            Text("重新集點（新公司）", style = pixelTextStyle(17.sp, FontWeight.Bold, PixelColors.black))
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
        modifier = modifier.pixelCard().padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = PixelColors.black, modifier = Modifier.size(30.dp))
        Text(value, style = pixelTextStyle(30.sp, FontWeight.Bold, PixelColors.black))
        Text(label, style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.gray))
    }
}
