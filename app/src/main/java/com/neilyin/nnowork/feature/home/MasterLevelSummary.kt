package com.neilyin.nnowork.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.neilyin.nnowork.R
import com.neilyin.nnowork.ui.adaptive.NnoworkAdaptive
import com.neilyin.nnowork.ui.adaptive.readableWidth
import com.neilyin.nnowork.ui.theme.NnoworkDialogDefaults
import com.neilyin.nnowork.ui.theme.NnoworkElevation
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelButtonVariant
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.launch

/**
 * 第 3 張起集滿的「忍耐大師！」里程碑提示（iOS HomeSubviews `masterEasterEggOverlay`）。
 * 遮罩上放一張跟對話框同底色的卡片：淺色主題不會出現整片深色對話框，Night 主題也一樣可讀。
 */
@Composable
internal fun MasterMilestoneOverlay(round: Int, onOpen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NnoworkTheme.colors.scrim)
            // 吃掉點擊：iOS 點背景不做任何事。
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(NnoworkSpacing.lg)
                .widthIn(max = NnoworkAdaptive.formMaxWidth)
                .pixelCard(
                    backgroundColor = NnoworkDialogDefaults.containerColor,
                    shape = NnoworkDialogDefaults.shape,
                    elevation = NnoworkElevation.hero,
                )
                .padding(horizontal = NnoworkSpacing.lg, vertical = NnoworkSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🏆", style = pixelTextStyle(NnoworkTheme.type.hero))
            Spacer(Modifier.height(NnoworkSpacing.md))
            Text(
                stringResource(R.string.master_overlay_title),
                style = pixelTextStyle(NnoworkTheme.type.display, FontWeight.Bold, NnoworkTheme.colors.highlight),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(NnoworkSpacing.xs))
            Text(
                stringResource(R.string.master_overlay_subtitle, round),
                style = pixelTextStyle(NnoworkTheme.type.subtitle, color = NnoworkDialogDefaults.contentColor),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(NnoworkSpacing.lg))
            PixelButton(onClick = onOpen, variant = PixelButtonVariant.Celebrate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(NnoworkSize.icon))
                Spacer(Modifier.size(NnoworkSpacing.xs))
                Text(stringResource(R.string.master_overlay_action), style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold))
            }
        }
    }
}

/**
 * 忍耐大師階段總結（iOS `MasterLevelReceiptView`）：收據＋儲存到相簿／再來一張／勇敢離職／分享。
 * 全螢幕對話框，App 底色走主題；收據卡用 elevatedSurface，大螢幕限寬置中。
 */
@Composable
internal fun MasterLevelSummaryDialog(
    stats: MasterSummaryStats,
    onDismiss: () -> Unit,
    onStartNewCard: (maxStamps: Int) -> Unit,
    onQuit: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        MasterLevelSummaryContent(stats, onDismiss, onStartNewCard, onQuit)
    }
}

@Composable
private fun MasterLevelSummaryContent(
    stats: MasterSummaryStats,
    onDismiss: () -> Unit,
    onStartNewCard: (maxStamps: Int) -> Unit,
    onQuit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSlotPicker by rememberSaveable { mutableStateOf(false) }
    // 收據「印出來」：從上方滑入，按鈕等收據到位後才出現。
    val printed = remember { MutableTransitionState(false).apply { targetState = true } }
    val imageText = rememberMasterImageText(stats)
    val chooserTitle = stringResource(R.string.master_share)
    val savedMessage = stringResource(R.string.master_saved)
    val failedMessage = stringResource(R.string.master_image_failed)

    val save: () -> Unit = {
        scope.launch {
            runCatching { MasterSummaryExporter.saveToGallery(context, imageText) }
                .onSuccess { Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show() }
        }
    }
    // API 29+ 存相簿不需權限；API 26–28 需要 WRITE_EXTERNAL_STORAGE（Manifest 須宣告 maxSdkVersion=28）。
    val legacyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save() else Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show()
    }
    val onSave: () -> Unit = {
        val needsLegacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsLegacyPermission) legacyPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else save()
    }
    val onShare: () -> Unit = {
        scope.launch {
            runCatching { MasterSummaryExporter.share(context, imageText, chooserTitle) }
                .onFailure { Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NnoworkTheme.colors.appBG, NnoworkTheme.colors.appBGBottom)))
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = NnoworkSize.minTouchTarget)
                .readableWidth(NnoworkAdaptive.formMaxWidth)
                .padding(horizontal = NnoworkSpacing.lg, vertical = NnoworkSpacing.page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.lg),
        ) {
            AnimatedVisibility(
                visibleState = printed,
                enter = slideInVertically { -it } + fadeIn(),
            ) {
                MasterReceiptCard(stats)
            }
            AnimatedVisibility(visible = printed.isIdle && printed.currentState, enter = fadeIn()) {
                AnimatedContent(
                    targetState = showSlotPicker,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "masterActions",
                ) { picking ->
                    if (picking) {
                        MasterSlotPicker(onPick = onStartNewCard, onBack = { showSlotPicker = false })
                    } else {
                        MasterActions(
                            onSave = onSave,
                            onAnother = { showSlotPicker = true },
                            onQuit = onQuit,
                            onShare = onShare,
                        )
                    }
                }
            }
            Spacer(Modifier.height(NnoworkSpacing.md))
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = NnoworkSpacing.xs),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.master_close_desc),
                tint = NnoworkTheme.colors.secondaryText,
            )
        }
    }
}

@Composable
private fun MasterReceiptCard(stats: MasterSummaryStats) {
    val colors = NnoworkTheme.colors
    val company = stats.companyName.ifBlank { stringResource(R.string.master_company_fallback) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = colors.elevatedSurface, elevation = NnoworkElevation.hero)
            .padding(horizontal = NnoworkSpacing.lg, vertical = NnoworkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.roomy),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.master_title),
                style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, colors.highlight),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(NnoworkSpacing.xs))
            Text(stringResource(R.string.master_subtitle), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, colors.special))
            Spacer(Modifier.height(NnoworkSpacing.xxs))
            Text(
                company,
                style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Medium, colors.secondaryText),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MasterDashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            MasterDateRow(stringResource(R.string.master_from), formatMasterDate(stats.firstStampDate))
            MasterDateRow(stringResource(R.string.master_to), formatMasterDate(stats.completionDate))
        }
        MasterDashedDivider()
        Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
            MasterStatRow(Icons.Filled.Schedule, stringResource(R.string.master_row_days), stringResource(R.string.master_value_days, stats.totalDays))
            MasterStatRow(Icons.Filled.Verified, stringResource(R.string.master_row_stamps), stringResource(R.string.master_value_stamps, stats.totalStamps))
            MasterStatRow(Icons.Filled.MonetizationOn, stringResource(R.string.master_row_saved), formatMasterSavedAmount(stats.savedAmount), highlight = true)
        }
        MasterDashedDivider()
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = NnoworkSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
        ) {
            Text(stringResource(R.string.master_still_holding), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, colors.secondaryText))
            Text(
                "🏆 " + stringResource(R.string.master_model_employee, company) + " 🏆",
                style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, colors.special),
                textAlign = TextAlign.Center,
            )
        }
        Text(
            stringResource(R.string.master_footer_date, formatMasterFullDate(System.currentTimeMillis())),
            style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Medium, colors.secondaryText),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MasterDateRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.secondaryText), modifier = Modifier.weight(1f))
        Text(value, style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true))
    }
}

@Composable
private fun MasterStatRow(icon: ImageVector, label: String, value: String, highlight: Boolean = false) {
    val colors = NnoworkTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = if (highlight) colors.success else colors.secondaryText, modifier = Modifier.size(NnoworkSize.compactIcon))
        Spacer(Modifier.size(NnoworkSpacing.xs))
        Text(label, style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Medium, colors.primaryText), modifier = Modifier.weight(1f))
        Text(value, style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, if (highlight) colors.success else colors.primaryText, monospace = true))
    }
}

@Composable
private fun MasterDashedDivider() {
    Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs), modifier = Modifier.fillMaxWidth()) {
        repeat(18) {
            Spacer(
                Modifier
                    .weight(1f)
                    .height(NnoworkSize.timelineConnector)
                    .background(NnoworkTheme.colors.divider),
            )
        }
    }
}

@Composable
private fun MasterActions(
    onSave: () -> Unit,
    onAnother: () -> Unit,
    onQuit: () -> Unit,
    onShare: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
        MasterActionButton(Icons.Filled.Download, stringResource(R.string.master_save_photos), PixelButtonVariant.Tonal, onSave)
        Text(
            stringResource(R.string.master_hint),
            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = NnoworkSpacing.xxs),
        )
        MasterActionButton(Icons.Filled.Replay, stringResource(R.string.master_another), PixelButtonVariant.Primary, onAnother)
        MasterActionButton(Icons.Filled.FlightTakeoff, stringResource(R.string.master_quit), PixelButtonVariant.Celebrate, onQuit)
        MasterActionButton(Icons.Filled.Share, stringResource(R.string.master_share), PixelButtonVariant.Outlined, onShare)
    }
}

@Composable
private fun MasterActionButton(
    icon: ImageVector,
    label: String,
    variant: PixelButtonVariant,
    onClick: () -> Unit,
) {
    PixelButton(
        onClick = onClick,
        variant = variant,
        modifier = Modifier.fillMaxWidth().heightIn(min = NnoworkSize.controlHeight),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(NnoworkSize.compactIcon))
        Spacer(Modifier.size(NnoworkSpacing.xs))
        Text(label, style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold))
    }
}

@Composable
private fun MasterSlotPicker(onPick: (Int) -> Unit, onBack: () -> Unit) {
    val colors = NnoworkTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
    ) {
        Text(
            stringResource(R.string.master_pick_slots),
            style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, colors.primaryText),
            textAlign = TextAlign.Center,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
            MASTER_SLOT_OPTIONS.forEach { count ->
                PixelButton(
                    onClick = { onPick(count) },
                    variant = PixelButtonVariant.Tonal,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(NnoworkRadius.card),
                    modifier = Modifier.weight(1f).heightIn(min = 88.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$count", style = pixelTextStyle(NnoworkTheme.type.display, FontWeight.Bold, monospace = true))
                        Text(stringResource(R.string.master_slots_unit), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium))
                    }
                }
            }
        }
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.master_back), style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Medium, colors.secondaryText))
        }
    }
}

/** 分享圖／存相簿用的文字（依目前語系）。 */
@Composable
private fun rememberMasterImageText(stats: MasterSummaryStats): MasterSummaryImageText {
    val company = stats.companyName.ifBlank { stringResource(R.string.master_company_fallback) }
    return MasterSummaryImageText(
        title = stringResource(R.string.master_title),
        subtitle = stringResource(R.string.master_subtitle),
        company = company,
        fromLabel = stringResource(R.string.master_from),
        toLabel = stringResource(R.string.master_to),
        fromValue = formatMasterDate(stats.firstStampDate),
        toValue = formatMasterDate(stats.completionDate),
        rows = listOf(
            stringResource(R.string.master_row_days) to stringResource(R.string.master_value_days, stats.totalDays),
            stringResource(R.string.master_row_stamps) to stringResource(R.string.master_value_stamps, stats.totalStamps),
            stringResource(R.string.master_row_saved) to formatMasterSavedAmount(stats.savedAmount),
        ),
        stillHolding = stringResource(R.string.master_still_holding),
        modelEmployee = stringResource(R.string.master_model_employee, company),
        footerDate = stringResource(R.string.master_footer_date, formatMasterFullDate(System.currentTimeMillis())),
        exportFooter = stringResource(R.string.master_export_footer),
    )
}
