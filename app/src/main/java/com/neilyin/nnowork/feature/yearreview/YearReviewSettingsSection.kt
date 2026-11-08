package com.neilyin.nnowork.feature.yearreview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neilyin.nnowork.BuildConfig
import com.neilyin.nnowork.R
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.nnoworkSwitchColors
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle

/**
 * 設定頁最上方的年度回顧入口（§22.13）：開放期間（或「提前開放」）才出現。
 * 任一測試開關開著時標「測試中」。點了先記邀請卡看過再打開回顧。
 * 測試開關卡只在 [YearReviewPrefs.TEST_TOOLS_ENABLED] 且 DEBUG build 時出現（文字只有繁中）。
 */
@Composable
fun YearReviewSettingsSection(cardTheme: CardTheme, modifier: Modifier = Modifier) {
    val flags by YearReviewPrefs.flags.collectAsStateWithLifecycle()
    // 不 remember：App 開著跨過 12/10 或 2/1 時，下次重組（例如回到設定頁）入口就會跟著出現／消失。
    val today = YearReviewWindow.today()
    val year = YearReviewWindow.availableYear(today, flags.earlyAccess)
    var openYear by rememberSaveable { mutableStateOf<Int?>(null) }
    val showTestTools = YearReviewPrefs.TEST_TOOLS_ENABLED && BuildConfig.DEBUG
    if (year == null && !showTestTools) return

    openYear?.let { YearReviewDialog(year = it, cardTheme = cardTheme, onDismiss = { openYear = null }) }

    val colors = NnoworkTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, tint = colors.accentForeground, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text(stringResource(R.string.yr_feature_name), style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, colors.primaryText))
        }
        if (year != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .pixelCard()
                    .clickable(role = Role.Button) {
                        // 自己從設定頁打開也算看過，之後不再跳邀請卡
                        YearReviewPrefs.markInviteShown(year)
                        openYear = year
                    }
                    .padding(NnoworkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🎞️", fontSize = 26.sp)
                Spacer(Modifier.size(NnoworkSpacing.sm))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.yr_year_in_review, year), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, colors.primaryText))
                        Tag(stringResource(R.string.yr_limited_time), colors.accentSoft, colors.primaryText)
                        if (flags.anyOn) Tag(stringResource(R.string.yr_debug_testing), PixelColors.red, PixelColors.white)
                    }
                    Text(stringResource(R.string.yr_settings_subtitle), style = pixelTextStyle(NnoworkTheme.type.small, color = colors.secondaryText))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.secondaryText)
            }
        }
        if (showTestTools) DebugToggles()
    }
}

@Composable
private fun Tag(text: String, background: androidx.compose.ui.graphics.Color, foreground: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = pixelTextStyle(10.sp, FontWeight.Bold, foreground),
        modifier = Modifier
            .clip(RoundedCornerShape(NnoworkRadius.small))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** DEBUG 測試開關（與 iOS 同一組 key；文字只有繁中）。 */
@Composable
private fun DebugToggles() {
    val flags by YearReviewPrefs.flags.collectAsStateWithLifecycle()
    val colors = NnoworkTheme.colors
    Column(
        Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
    ) {
        Text(stringResource(R.string.yr_debug_header), style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, colors.secondaryText))
        listOf(
            Triple(R.string.yr_debug_early_access, YearReviewTestFlags.EARLY_ACCESS_KEY, flags.earlyAccess),
            Triple(R.string.yr_debug_simulate_window, YearReviewTestFlags.SIMULATE_INVITE_WINDOW_KEY, flags.simulateInviteWindow),
            Triple(R.string.yr_debug_load_delay, YearReviewTestFlags.LOAD_DELAY_KEY, flags.loadDelay),
            Triple(R.string.yr_debug_demo_data, YearReviewTestFlags.DEMO_DATA_KEY, flags.demoData),
        ).forEach { (label, key, checked) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(label), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.SemiBold, colors.primaryText), modifier = Modifier.weight(1f))
                Switch(checked = checked, onCheckedChange = { YearReviewPrefs.setFlag(key, it) }, colors = nnoworkSwitchColors())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            PixelButton(onClick = YearReviewPrefs::resetInviteShown, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.yr_debug_reset_invite), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.SemiBold, NnoworkTheme.colors.primaryText))
            }
            PixelButton(onClick = YearReviewPrefs::requestShowInviteNow, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.yr_debug_show_invite), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.SemiBold, NnoworkTheme.colors.primaryText))
            }
        }
        PixelButton(onClick = YearReviewPrefs::turnAllOff, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.yr_debug_all_off), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.SemiBold, NnoworkTheme.colors.primaryText))
        }
    }
}
