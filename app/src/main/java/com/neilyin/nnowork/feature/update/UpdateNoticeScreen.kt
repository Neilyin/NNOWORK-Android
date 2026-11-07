package com.neilyin.nnowork.feature.update

import com.neilyin.nnowork.ui.adaptive.readableWidth
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.neilyin.nnowork.ui.theme.contentColorFor
import androidx.activity.compose.BackHandler
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkColors
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle

const val UPDATE_NOTICE_VERSION = "android-2.0-theme-351"

private data class UpdatePage(
    val icon: ImageVector,
    val accent: (NnoworkColors) -> Color,
    val eyebrow: String,
    val title: String,
    val body: String,
    val location: String? = null,
)

private val updatePages = listOf(
    UpdatePage(
        icon = Icons.Filled.Campaign,
        accent = { it.highlight },
        eyebrow = "給 Android 使用者的一封信",
        title = "我們想先向你道歉",
        body = "Android 版本原負責工程師因身體因素需長期住院。事發突然，原始碼未能完成交接。上一版為了讓更新繼續，我們參照 iOS 版本重構了 Android App。\n\n如果更新後曾造成你的資料遺失，我們真的很抱歉，這不是我們想看到的結果。\n\n所以我們做了以下的改變來維持你們的資料安全。",
    ),
    UpdatePage(
        icon = Icons.Filled.Shield,
        accent = { it.success },
        eyebrow = "資料安全",
        title = "把重要記錄留在自己手上",
        body = "這個版本加入版本化 JSON 備份。你可以隨時把集點記錄、離職計畫與 App 設定匯出成檔案，妥善保存在自己的裝置或雲端空間。",
        location = "設定 → 資料備份與移轉 → 匯出備份",
    ),
    UpdatePage(
        icon = Icons.Filled.ImportExport,
        accent = { it.info },
        eyebrow = "匯入與還原",
        title = "換裝置也能把資料帶回來",
        body = "選擇先前匯出的 JSON 檔即可還原。App 會先檢查檔案內容，並在取代現有資料前再次請你確認，降低誤操作風險。",
        location = "設定 → 資料備份與移轉 → 匯入備份",
    ),
    UpdatePage(
        icon = Icons.Filled.Favorite,
        accent = { it.danger },
        eyebrow = "再次向你說聲抱歉",
        title = "送你 15 枚離職金幣",
        body = "為了表達歉意，我們準備了 15 枚離職金幣，你可以到主題商店兌換一個自己喜歡的主題。\n\n這次的更新把主題全面優化，整個 App 的配色都會一起調整。\n\n謝謝大家喜歡離職集點卡，我們會持續進步與努力。",
    ),
    UpdatePage(
        icon = Icons.Filled.Share,
        accent = { it.warning },
        eyebrow = "分享新功能",
        title = "取暖樹洞現在也可以\n分享給朋友了",
        body = "把有共鳴的心聲分享出去，讓朋友也能一起取暖。分享卡不會包含暱稱與公司名稱。",
    ),
    UpdatePage(
        icon = Icons.Filled.Palette,
        accent = { it.teal },
        eyebrow = "主題商店",
        title = "新增三款主題",
        body = "夏日海風的水藍、厭世熊貓的黑白灰、樹懶模式的大地色。熊貓與樹懶還有三種專屬印章，會依格子固定輪替。",
        location = "設定 → 主題商店",
    ),
    UpdatePage(
        icon = Icons.Filled.Visibility,
        accent = { it.info },
        eyebrow = "購買前先看看",
        title = "主題現在可以互動預覽",
        body = "點選尚未解鎖的主題，就能先看完整配色、卡片與印章，也可以試著蓋幾個章。預覽不會改掉目前使用中的主題。",
        location = "設定 → 主題商店 → 選擇未解鎖主題",
    ),
    UpdatePage(
        icon = Icons.Filled.RocketLaunch,
        accent = { it.success },
        eyebrow = "準備好了嗎？",
        title = "進入離職集點卡",
        body = "我們一起規劃職涯，一起線上取暖！",
    ),
)

@Composable
fun UpdateNoticeScreen(onFinished: () -> Unit) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val page = updatePages[pageIndex]
    val isLastPage = pageIndex == updatePages.lastIndex
    BackHandler(onBack = onFinished)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .readableWidth()
            .padding(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "本次更新  ${pageIndex + 1}/${updatePages.size}",
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onFinished) {
                Text(
                    "關閉",
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "第 ${pageIndex + 1} 頁，共 ${updatePages.size} 頁" },
            horizontalArrangement = Arrangement.Center,
        ) {
            updatePages.indices.forEach { index ->
                Box(
                    Modifier
                        .padding(horizontal = NnoworkSpacing.xxs)
                        .size(if (index == pageIndex) 12.dp else 8.dp)
                        .background(
                            color = if (index == pageIndex) page.accent(NnoworkTheme.colors) else NnoworkTheme.colors.secondaryText,
                            shape = CircleShape,
                        ),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .verticalScroll(rememberScrollState())
                .padding(vertical = NnoworkSpacing.page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(NnoworkRadius.hero))
                    .background(page.accent(NnoworkTheme.colors)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = contentColorFor(page.accent(NnoworkTheme.colors)),
                    modifier = Modifier.size(52.dp),
                )
            }
            Spacer(Modifier.height(NnoworkSpacing.lg))
            Text(
                page.eyebrow,
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, page.accent(NnoworkTheme.colors)),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(NnoworkSpacing.xs))
            Text(
                page.title,
                style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(NnoworkSpacing.roomy))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelCard()
                    .padding(NnoworkSpacing.roomy),
            ) {
                Text(
                    page.body,
                    style = pixelTextStyle(NnoworkTheme.type.bodyLarge, color = NnoworkTheme.colors.mutedText).copy(lineHeight = NnoworkTheme.type.bodyLarge * 1.5625f),
                )
                page.location?.let { location ->
                    Spacer(Modifier.height(NnoworkSpacing.md))
                    Text("在哪裡用", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
                    Spacer(Modifier.height(5.dp))
                    Text(location, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
        ) {
            if (pageIndex > 0) {
                PixelButton(
                    onClick = { pageIndex -= 1 },
                    backgroundColor = NnoworkTheme.colors.disabledBG,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("上一頁", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            }
            PixelButton(
                onClick = {
                    if (isLastPage) onFinished() else pageIndex += 1
                },
                backgroundColor = if (isLastPage) NnoworkTheme.colors.success else page.accent(NnoworkTheme.colors),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (isLastPage) "開始使用" else "下一頁",
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
        }
    }
}

@Preview(name = "更新與道歉啟事", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun UpdateNoticePreview() {
    UpdateNoticeScreen(onFinished = {})
}
