package com.neilyin.nnowork.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.pixelTextStyle

/** 尚未移植完成的分頁暫用畫面。 */
@Composable
fun PlaceholderScreen(title: String, subtitle: String = "施工中…即將完成") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚧", style = pixelTextStyle(48.sp))
            Spacer(Modifier.height(12.dp))
            Text(title, style = pixelTextStyle(24.sp, FontWeight.Bold, PixelColors.white), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = pixelTextStyle(14.sp, color = PixelColors.gray), textAlign = TextAlign.Center)
        }
    }
}
