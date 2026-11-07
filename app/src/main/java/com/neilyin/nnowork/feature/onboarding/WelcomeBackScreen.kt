package com.neilyin.nnowork.feature.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.NnoworkCenteredPage
import com.neilyin.nnowork.ui.theme.NnoworkElevation
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle

@Composable
fun WelcomeBackScreen(
    onReturningUser: () -> Unit,
    onNewUser: () -> Unit,
) {
    NnoworkCenteredPage {
        Spacer(Modifier.height(NnoworkSpacing.md))
        WelcomeIcon(Icons.Filled.CloudDownload, "資料找回")
        Spacer(Modifier.height(NnoworkSpacing.lg))
        Text("在開始之前", style = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onBackground))
        Spacer(Modifier.height(12.dp))
        Text(
            "你是第一次使用這個 App，\n還是重新安裝、想找回之前的資料？",
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(44.dp))
        PixelButton(onClick = onReturningUser, backgroundColor = PixelColors.yellow, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CloudDownload, null, tint = PixelColors.black)
            Spacer(Modifier.size(8.dp))
            Text("我是老用戶（找回資料）", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.black))
        }
        Spacer(Modifier.height(14.dp))
        PixelButton(onClick = onNewUser, backgroundColor = MaterialTheme.colorScheme.surface, borderColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PersonAdd, null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(8.dp))
            Text("我是新用戶", style = pixelTextStyle(16.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurface))
        }
    }
}

@Composable
fun ReturningUserScreen(
    processing: Boolean,
    message: String?,
    isError: Boolean,
    onImport: (Uri) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    var pendingUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingUri = uri
    }

    pendingUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { if (!processing) pendingUri = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("匯入這份備份？", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
            text = { Text("會用備份內容還原集點紀錄、離職計畫、金幣與薪水設定。", style = pixelTextStyle(14.sp, color = PixelColors.white)) },
            confirmButton = {
                PixelButton(onClick = { onImport(uri); pendingUri = null }, enabled = !processing, backgroundColor = PixelColors.blue) {
                    Text("確認匯入", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white))
                }
            },
            dismissButton = {
                PixelButton(onClick = { pendingUri = null }, enabled = !processing, backgroundColor = PixelColors.gray) {
                    Text("取消", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
                }
            },
        )
    }

    NnoworkCenteredPage {
        WelcomeIcon(Icons.Filled.CloudDownload, "找回你的資料")
        Spacer(Modifier.height(NnoworkSpacing.lg))
        Text("找回你的資料", style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground))
        Spacer(Modifier.height(12.dp))
        Text(
            "Android 系統備份若已還原，App 會自動接續；也可以選擇之前匯出的 NNOWORK JSON 備份。",
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PixelButton(onClick = { launcher.launch(arrayOf("application/json", "text/plain")) }, enabled = !processing, backgroundColor = PixelColors.blue, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.FileUpload, null, tint = PixelColors.white)
            Spacer(Modifier.size(8.dp))
            Text(if (processing) "匯入中..." else "選擇備份檔", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.white))
        }
        message?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, style = pixelTextStyle(13.sp, FontWeight.Bold, if (isError) PixelColors.red else PixelColors.green), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(18.dp))
        PixelButton(onClick = onSkip, enabled = !processing, backgroundColor = PixelColors.gray, modifier = Modifier.fillMaxWidth()) {
            Text("略過，以新用戶開始", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
        }
        Spacer(Modifier.height(10.dp))
        PixelButton(onClick = onBack, enabled = !processing, backgroundColor = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            Text("返回", style = pixelTextStyle(14.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurface))
        }
    }
}

@Composable
private fun WelcomeIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
) {
    Box(
        modifier = Modifier
            .size(86.dp)
            .pixelCard(
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(NnoworkRadius.hero),
                elevation = NnoworkElevation.hero,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
    }
}
