package com.neilyin.nnowork.feature.onboarding

import com.neilyin.nnowork.ui.theme.NnoworkDialogDefaults
import android.net.Uri
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkTheme
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
import com.neilyin.nnowork.ui.theme.PixelButton
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
        Spacer(Modifier.height(NnoworkSpacing.sm))
        Text(
            "你是第一次使用這個 App，\n還是重新安裝、想找回之前的資料？",
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(44.dp))
        PixelButton(onClick = onReturningUser, backgroundColor = NnoworkTheme.colors.highlight, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.CloudDownload, null, tint = NnoworkTheme.colors.primaryText)
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text("我是老用戶（找回資料）", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
        Spacer(Modifier.height(NnoworkSpacing.cozy))
        PixelButton(onClick = onNewUser, backgroundColor = MaterialTheme.colorScheme.surface, borderColor = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PersonAdd, null, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text("我是新用戶", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, MaterialTheme.colorScheme.onSurface))
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
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = { Text("匯入這份備份？", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
            text = { Text("會用備份內容還原集點紀錄、離職計畫、金幣與薪水設定。", style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated)) },
            confirmButton = {
                PixelButton(onClick = { onImport(uri); pendingUri = null }, enabled = !processing, backgroundColor = NnoworkTheme.colors.buttonBG) {
                    Text("確認匯入", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                }
            },
            dismissButton = {
                PixelButton(onClick = { pendingUri = null }, enabled = !processing, backgroundColor = NnoworkTheme.colors.disabledBG) {
                    Text("取消", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            },
        )
    }

    NnoworkCenteredPage {
        WelcomeIcon(Icons.Filled.CloudDownload, "找回你的資料")
        Spacer(Modifier.height(NnoworkSpacing.lg))
        Text("找回你的資料", style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onBackground))
        Spacer(Modifier.height(NnoworkSpacing.sm))
        Text(
            "Android 系統備份若已還原，App 會自動接續；也可以選擇之前匯出的 NNOWORK JSON 備份。",
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(NnoworkSpacing.xl))
        PixelButton(onClick = { launcher.launch(arrayOf("application/json", "text/plain")) }, enabled = !processing, backgroundColor = NnoworkTheme.colors.buttonBG, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.FileUpload, null, tint = NnoworkTheme.colors.onElevated)
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text(if (processing) "匯入中..." else "選擇備份檔", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.onElevated))
        }
        message?.let {
            Spacer(Modifier.height(NnoworkSpacing.cozy))
            Text(it, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, if (isError) NnoworkTheme.colors.danger else NnoworkTheme.colors.success), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(NnoworkSpacing.roomy))
        PixelButton(onClick = onSkip, enabled = !processing, backgroundColor = NnoworkTheme.colors.disabledBG, modifier = Modifier.fillMaxWidth()) {
            Text("略過，以新用戶開始", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
        Spacer(Modifier.height(NnoworkSpacing.snug))
        PixelButton(onClick = onBack, enabled = !processing, backgroundColor = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            Text("返回", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, MaterialTheme.colorScheme.onSurface))
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
            modifier = Modifier.size(NnoworkSize.minTouchTarget),
        )
    }
}
