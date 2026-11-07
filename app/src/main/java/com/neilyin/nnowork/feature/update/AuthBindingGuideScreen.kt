package com.neilyin.nnowork.feature.update

import androidx.activity.compose.BackHandler
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle

@Composable
fun AuthBindingGuideScreen(
    onOpenSettings: () -> Unit,
    onLater: () -> Unit,
) {
    BackHandler(onBack = onLater)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.CloudSync, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(NnoworkSpacing.lg))
        Text("讓重要資料有回家的路", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, MaterialTheme.colorScheme.onBackground), textAlign = TextAlign.Center)
        Spacer(Modifier.height(NnoworkSpacing.cozy))
        Text(
            "綁定 Email 或 Google 帳號後，樹洞貼文與留言能在換機時重新找回。這完全是選用功能，你也可以繼續匿名使用。",
            style = pixelTextStyle(NnoworkTheme.type.body, color = MaterialTheme.colorScheme.onBackground),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.roomy),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
        ) {
            Icon(Icons.Filled.Security, null, tint = NnoworkTheme.colors.success)
            Text("密碼由 Supabase Auth 管理，App 不會保存密碼。登入 session 會以 Android Keystore 加密。", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.mutedText))
        }
        Spacer(Modifier.height(28.dp))
        PixelButton(onClick = onOpenSettings, backgroundColor = NnoworkTheme.colors.highlight, modifier = Modifier.fillMaxWidth()) {
            Text("前往設定帳號", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
        Spacer(Modifier.height(NnoworkSpacing.sm))
        PixelButton(onClick = onLater, backgroundColor = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Text("稍後再說", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, MaterialTheme.colorScheme.onSurface))
        }
    }
}
