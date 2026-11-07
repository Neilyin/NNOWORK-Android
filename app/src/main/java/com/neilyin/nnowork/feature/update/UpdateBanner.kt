package com.neilyin.nnowork.feature.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.install.model.UpdateAvailability
import com.neilyin.nnowork.BuildConfig
import com.neilyin.nnowork.R
import com.neilyin.nnowork.data.settings.PromptPrefsRepository
import com.neilyin.nnowork.ui.adaptive.readableWidth
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelButtonVariant
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import com.neilyin.nnowork.util.PlayStore
import kotlinx.coroutines.launch

/**
 * 非強制更新 banner 的顯示規則（純函式；對應 iOS AppUpdateChecker + RootView 的 showUpdateBanner）。
 * 以 Play 的 availableVersionCode 為版本識別：關閉後記住該版本，更新的版本出現時會再提示。
 */
object UpdateBannerPolicy {
    fun shouldShow(updateAvailable: Boolean, availableVersionCode: Int, dismissedVersionCode: Int): Boolean =
        updateAvailable && availableVersionCode > 0 && availableVersionCode > dismissedVersionCode
}

/** 查詢到的可用更新；Play 只給版本碼，不給版本名稱。 */
data class AvailableUpdate(val versionCode: Int)

/** 向 Google Play 查詢是否有新版；沒有、非 Play 安裝或任何失敗皆回 null（靜默，不影響使用）。 */
suspend fun checkPlayForUpdate(context: android.content.Context): AvailableUpdate? = runCatching {
    val info = AppUpdateManagerFactory.create(context).requestAppUpdateInfo()
    if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
        AvailableUpdate(info.availableVersionCode())
    } else {
        null
    }
}.getOrNull()

/**
 * 放在主畫面最上層的更新提示（iOS：RootView 頂部滑入）。啟動後查詢一次，
 * 有新版且該版本沒被關閉過才顯示。
 */
@Composable
fun UpdateBannerHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = PromptPrefsRepository.instance
    val scope = rememberCoroutineScope()
    val dismissed by prefs.dismissedUpdateVersionCode.collectAsStateWithLifecycle(initialValue = Int.MAX_VALUE)
    var update by remember { mutableStateOf<AvailableUpdate?>(null) }

    LaunchedEffect(Unit) { update = checkPlayForUpdate(context) }

    val current = update
    AnimatedVisibility(
        visible = current != null && UpdateBannerPolicy.shouldShow(true, current.versionCode, dismissed),
        modifier = modifier,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        if (current != null) {
            UpdateBanner(
                latestVersion = current.versionCode.toString(),
                currentVersion = BuildConfig.VERSION_NAME,
                onUpdate = { PlayStore.openListing(context) },
                onDismiss = { scope.launch { prefs.dismissUpdate(current.versionCode) } },
                modifier = Modifier.statusBarsPadding().padding(horizontal = NnoworkSpacing.md, vertical = NnoworkSpacing.xs),
            )
        }
    }
}

@Composable
fun UpdateBanner(
    latestVersion: String,
    currentVersion: String,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NnoworkTheme.colors
    Row(
        modifier = modifier
            .readableWidth()
            .pixelCard(backgroundColor = colors.elevatedSurface)
            .padding(horizontal = NnoworkSpacing.md, vertical = NnoworkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(NnoworkSize.minTouchTarget - NnoworkSpacing.xs)
                .clip(RoundedCornerShape(NnoworkRadius.field))
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = colors.accentForeground, modifier = Modifier.size(NnoworkSize.icon))
        }
        Spacer(Modifier.width(NnoworkSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.update_banner_title, latestVersion),
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, colors.onElevated),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.update_banner_subtitle, currentVersion),
                style = pixelTextStyle(NnoworkTheme.type.caption, color = colors.secondaryText),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(NnoworkSpacing.xs))
        PixelButton(
            onClick = onUpdate,
            variant = PixelButtonVariant.Primary,
            contentPadding = PaddingValues(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.xs),
        ) {
            Text(stringResource(R.string.update_banner_action), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, colors.buttonText))
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(NnoworkSize.minTouchTarget)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.update_banner_dismiss),
                tint = colors.secondaryText,
                modifier = Modifier.size(NnoworkSize.compactIcon),
            )
        }
    }
}
