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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
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

const val UPDATE_NOTICE_VERSION = "android-3.7"

private data class UpdatePage(
    val icon: ImageVector,
    val accent: (NnoworkColors) -> Color,
    val eyebrow: String,
    val title: String,
    val body: String,
    val location: String? = null,
    /** 最後一頁：附上 3.0～3.7 的版本紀錄。 */
    val showHistory: Boolean = false,
)

private data class VersionEntry(val version: String, val items: List<String>)

/** 3.0～3.7 版本紀錄（只列 Android 也有的功能；新到舊）。 */
private val versionHistory = listOf(
    VersionEntry("3.7", listOf(
        "集滿第 3 張起成為「忍耐大師」，先看階段性總結",
        "離職結算改成 RPG 破關風格",
        "摺疊機、平板支援左右雙欄",
        "樹洞留言不再限制字數",
        "全新介面：柔和框線、卡片陰影、深色主題文字更清楚",
        "牛馬桌面小工具、使用說明書、App 內更新提示",
    )),
    VersionEntry("3.6", listOf(
        "換工作可以開新卡，舊卡收進「過往工作」",
        "過往工作可回顧期間、章數、最常見原因與蓋章時間軸",
        "樹洞貼文標示「二周目」「三周目」",
        "備份檔包含過往工作",
    )),
    VersionEntry("3.5", listOf(
        "職場解惑區：可以直接發問，回答累積解答者獎牌",
        "職業類別可多選篩選",
        "新主題：夏日海風、厭世熊貓、樹懶模式，買之前可預覽試蓋",
        "樹洞貼文與留言可檢舉、封鎖",
    )),
    VersionEntry("3.4", listOf(
        "Google／Email 帳號綁定，金幣與記錄可跨裝置",
        "資料備份：JSON 匯出與匯入",
        "樹洞貼文分享卡",
        "薪水計時器手動計時、新增 14 個行業別",
    )),
    VersionEntry("3.3", listOf("薪水計時器：上班時看著薪水一秒一秒累積")),
    VersionEntry("3.2", listOf("今日心聲卡片改版、新手教學更新")),
    VersionEntry("3.1", listOf("取暖樹洞新增留言與「我的樹洞」")),
    VersionEntry("3.0", listOf("Android 版全面重構，與 iOS 版同步")),
)

private val updatePages = listOf(
    UpdatePage(
        icon = Icons.Filled.Campaign,
        accent = { it.highlight },
        eyebrow = "V3.7 更新說明",
        title = "Android 版追上 iOS 了",
        body = "這次一口氣把 3.6 到 3.7 的功能帶到 Android，並重新整理了整個介面。\n\n接下來幾頁帶你快速看一遍；最後一頁有 3.0～3.7 的完整版本紀錄。",
    ),
    UpdatePage(
        icon = Icons.Filled.Work,
        accent = { it.info },
        eyebrow = "換工作也能留下回憶",
        title = "開新卡，舊卡收進「過往工作」",
        body = "選「我要離職」看完收據，就能直接開始新工作。主題、金幣、常用原因都會保留。\n\n過往工作可以回顧期間、章數、最常見原因、自由預備金與蓋章時間軸。封存需要綁定帳號，資料存在雲端。",
        location = "離職紀錄 → 過往工作",
    ),
    UpdatePage(
        icon = Icons.Filled.EmojiEvents,
        accent = { it.warning },
        eyebrow = "離職結算升級",
        title = "成為「忍耐大師」",
        body = "集滿第 3 張起，會先看到一份階段性總結，再決定要繼續忍耐還是勇敢離職。\n\n結算畫面也改成 RPG 破關風格：星空、跳躍的像素小牛、打字機對話框與冒險成果。",
    ),
    UpdatePage(
        icon = Icons.Filled.Devices,
        accent = { it.teal },
        eyebrow = "摺疊機與平板",
        title = "大螢幕自動變雙欄",
        body = "打開摺疊機內螢幕或使用平板時，離職紀錄、離職計畫、設定、取暖樹洞會變成左右兩欄。半折時內容會避開摺痕。",
    ),
    UpdatePage(
        icon = Icons.Filled.Forum,
        accent = { it.success },
        eyebrow = "取暖樹洞",
        title = "留言不再限制字數",
        body = "想說的可以完整說完。樹洞貼文也會標示「二周目」「三周目」，看得出大家已經換過幾份工作。",
    ),
    UpdatePage(
        icon = Icons.Filled.Palette,
        accent = { it.highlight },
        eyebrow = "全新介面",
        title = "更柔和、更好讀",
        body = "框線更柔和，卡片改用陰影分出層次，深色主題的次要文字更清楚。整個 App 的配色仍會跟著你選的主題一起變。",
    ),
    UpdatePage(
        icon = Icons.Filled.Widgets,
        accent = { it.warning },
        eyebrow = "Android 限定",
        title = "牛馬桌面小工具",
        body = "把牛馬放到桌面，不用打開 App 就能看到今天又賺了多少、離下班還有多久。",
        location = "長按桌面 → 小工具 → 離職集點卡",
    ),
    UpdatePage(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        accent = { it.info },
        eyebrow = "更多貼心小功能",
        title = "說明書與更新提示",
        body = "不知道某個功能怎麼用？設定裡新增了使用說明書。\n\n有新版本時，App 上方也會出現提示，點一下就能更新。",
        location = "設定 → 使用說明書",
    ),
    UpdatePage(
        icon = Icons.Filled.AutoAwesome,
        accent = { it.highlight },
        eyebrow = "年底見",
        title = "年度回顧 12/10 開放",
        body = "今年你忍了幾次、最常因為什麼想離職？每年 12/10～1/31，App 會為你整理一份專屬的年度回顧。",
    ),
    UpdatePage(
        icon = Icons.Filled.RocketLaunch,
        accent = { it.success },
        eyebrow = "版本紀錄",
        title = "3.0～3.7 更新了什麼",
        body = "謝謝大家喜歡離職集點卡，我們會持續進步與努力。",
        showHistory = true,
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
            if (page.showHistory) {
                Spacer(Modifier.height(NnoworkSpacing.md))
                VersionHistory()
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

@Composable
private fun VersionHistory() {
    val colors = NnoworkTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.roomy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
    ) {
        versionHistory.forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
                Text(
                    "V${entry.version}",
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, colors.highlight),
                    modifier = Modifier.semantics { heading() },
                )
                entry.items.forEach { item ->
                    Row {
                        Text("・", style = pixelTextStyle(NnoworkTheme.type.small, color = colors.mutedText))
                        Text(item, style = pixelTextStyle(NnoworkTheme.type.small, color = colors.mutedText))
                    }
                }
            }
        }
    }
}

@Preview(name = "V3.7 更新說明", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun UpdateNoticePreview() {
    UpdateNoticeScreen(onFinished = {})
}
