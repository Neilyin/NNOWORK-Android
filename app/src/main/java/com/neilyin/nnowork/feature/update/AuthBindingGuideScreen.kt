package com.neilyin.nnowork.feature.update

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
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
        Spacer(Modifier.height(24.dp))
        Text("讓重要資料有回家的路", style = pixelTextStyle(25.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground), textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(
            "綁定 Email 或 Google 帳號後，樹洞貼文與留言能在換機時重新找回。這完全是選用功能，你也可以繼續匿名使用。",
            style = pixelTextStyle(15.sp, color = MaterialTheme.colorScheme.onBackground),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth().pixelCard().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Security, null, tint = PixelColors.green)
            Text("密碼由 Supabase Auth 管理，App 不會保存密碼。登入 session 會以 Android Keystore 加密。", style = pixelTextStyle(13.sp, color = PixelColors.darkGray))
        }
        Spacer(Modifier.height(28.dp))
        PixelButton(onClick = onOpenSettings, backgroundColor = PixelColors.yellow, modifier = Modifier.fillMaxWidth()) {
            Text("前往設定帳號", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.black))
        }
        Spacer(Modifier.height(12.dp))
        PixelButton(onClick = onLater, backgroundColor = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Text("稍後再說", style = pixelTextStyle(15.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurface))
        }
    }
}
