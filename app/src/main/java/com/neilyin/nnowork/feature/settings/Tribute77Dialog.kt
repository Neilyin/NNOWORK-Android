package com.neilyin.nnowork.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.R
import com.neilyin.nnowork.ui.common.FullScreenDialog
import com.neilyin.nnowork.ui.theme.NnoworkDialogDefaults
import com.neilyin.nnowork.ui.theme.NnoworkElevation
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelButtonVariant
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle

/**
 * 設定頁署名彩蛋：紀念 77（為 Android 版打下基底的夥伴）。
 * 整頁遮罩上的置中卡片；內容較長，小螢幕可捲動。
 */
@Composable
internal fun Tribute77Dialog(onDismiss: () -> Unit) {
    FullScreenDialog(
        onDismissRequest = onDismiss,
        background = SolidColor(NnoworkTheme.colors.scrim),
        lightSystemBars = false,
    ) {
        // 外層置中、內層可捲：內容比螢幕短時卡片垂直置中，比螢幕長（小螢幕／大字級）時可以捲動。
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(NnoworkSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .pixelCard(
                        backgroundColor = NnoworkDialogDefaults.containerColor,
                        shape = NnoworkDialogDefaults.shape,
                        elevation = NnoworkElevation.hero,
                    )
                    .padding(horizontal = NnoworkSpacing.lg, vertical = NnoworkSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("😇", style = pixelTextStyle(NnoworkTheme.type.hero))
                Spacer(Modifier.height(NnoworkSpacing.sm))
                Text(
                    stringResource(R.string.tribute_77_title),
                    style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(NnoworkSpacing.md))
                Box(
                    Modifier
                        .width(48.dp)
                        .height(2.dp)
                        .background(NnoworkTheme.colors.highlight),
                )
                Spacer(Modifier.height(NnoworkSpacing.lg))

                Column(
                    verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    listOf(
                        R.string.tribute_77_p1,
                        R.string.tribute_77_p2,
                        R.string.tribute_77_p3,
                        R.string.tribute_77_p4,
                    ).forEach { res ->
                        Text(
                            stringResource(res),
                            style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(NnoworkSpacing.lg))
                Text(
                    stringResource(R.string.tribute_77_signoff),
                    style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.highlight)
                        .copy(fontStyle = FontStyle.Italic),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(NnoworkSpacing.xs))
                Text("🕊️  ✨  🕊️", style = pixelTextStyle(NnoworkTheme.type.body))
                Spacer(Modifier.height(NnoworkSpacing.lg))

                PixelButton(
                    onClick = onDismiss,
                    variant = PixelButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.tribute_77_action),
                        style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold),
                    )
                }
            }
          }
        }
    }
}
