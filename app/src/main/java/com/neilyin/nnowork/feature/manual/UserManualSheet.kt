package com.neilyin.nnowork.feature.manual

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.neilyin.nnowork.R
import com.neilyin.nnowork.ui.adaptive.NnoworkAdaptive
import com.neilyin.nnowork.ui.adaptive.readableWidth
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSheetDefaults
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** 使用手冊（iOS UserManualView，以 Bottom sheet 呈現）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManualSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val expandedSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })
    var expanded by rememberSaveable(stateSaver = expandedSaver) { mutableStateOf(emptySet<String>()) }
    val colors = NnoworkTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = NnoworkAdaptive.sheetMaxWidth,
        shape = NnoworkSheetDefaults.shape,
        containerColor = colors.appBG,
        contentColor = colors.primaryText,
        scrimColor = NnoworkSheetDefaults.scrimColor,
        dragHandle = { NnoworkSheetDefaults.DragHandle() },
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight().navigationBarsPadding()) {
            Box(modifier = Modifier.fillMaxWidth().readableWidth().padding(horizontal = NnoworkSpacing.page)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = NnoworkSize.minTouchTarget),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = colors.accentForeground, modifier = Modifier.size(NnoworkSize.icon))
                    Text(
                        stringResource(R.string.manual_title),
                        style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, colors.primaryText),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(R.string.manual_subtitle),
                        style = pixelTextStyle(NnoworkTheme.type.small, color = colors.secondaryText),
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).size(NnoworkSize.minTouchTarget)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.manual_close_desc), tint = colors.secondaryText)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .readableWidth()
                    .padding(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
            ) {
                ManualSections.forEach { section ->
                    ManualSectionCard(
                        section = section,
                        expanded = section.id in expanded,
                        onToggle = { expanded = toggleExpanded(expanded, section.id) },
                    )
                }
                Spacer(Modifier.height(NnoworkSpacing.lg))
            }
        }
    }
}

@Composable
private fun ManualTone.color(): Color {
    val c = NnoworkTheme.colors
    return when (this) {
        ManualTone.Warning -> c.warning
        ManualTone.Info -> c.info
        ManualTone.Success -> c.success
        ManualTone.Special -> c.special
        ManualTone.Teal -> c.teal
        ManualTone.Highlight -> c.highlight
        ManualTone.Accent -> c.accentForeground
        ManualTone.Muted -> c.secondaryText
    }
}

private fun ManualIcon.vector(): ImageVector = when (this) {
    ManualIcon.StampCard -> Icons.Filled.GridView
    ManualIcon.Plan -> Icons.AutoMirrored.Filled.ListAlt
    ManualIcon.Salary -> Icons.Filled.AttachMoney
    ManualIcon.History -> Icons.Filled.Book
    ManualIcon.TreeHole -> Icons.Filled.Park
    ManualIcon.Coin -> Icons.Filled.Toll
    ManualIcon.Account -> Icons.Filled.AccountCircle
    ManualIcon.Settings -> Icons.Filled.Settings
}

@Composable
private fun ManualSectionCard(section: ManualSection, expanded: Boolean, onToggle: () -> Unit) {
    val colors = NnoworkTheme.colors
    val tone = section.tone.color()
    val title = stringResource(section.title)
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "manualChevron")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(borderColor = if (expanded) tone else Color.Unspecified)
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(NnoworkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
        ) {
            Icon(section.icon.vector(), contentDescription = null, tint = tone, modifier = Modifier.size(NnoworkSize.icon))
            Text(
                title,
                style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, colors.onSurface),
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(NnoworkSize.icon).rotate(chevron),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = NnoworkSpacing.md, end = NnoworkSpacing.md, bottom = NnoworkSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
            ) {
                when (section.demo) {
                    ManualDemo.StampCard -> StampCardDemo()
                    ManualDemo.Salary -> {
                        SalaryDemo()
                        ScheduleCalendarDemo()
                    }
                    ManualDemo.TreeHole -> TreeHoleDemo()
                    null -> Unit
                }
                Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
                    section.bullets.forEach { bullet ->
                        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                            Box(
                                Modifier
                                    .padding(top = NnoworkSpacing.xs)
                                    .size(NnoworkSpacing.tight)
                                    .clip(CircleShape)
                                    .background(tone),
                            )
                            Text(
                                stringResource(bullet),
                                style = pixelTextStyle(NnoworkTheme.type.small, color = colors.onSurface),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 示意圖共用的底：elevated 面 + hairline。 */
@Composable
private fun Modifier.demoBox(): Modifier {
    val shape = RoundedCornerShape(NnoworkRadius.card)
    return this
        .fillMaxWidth()
        .clip(shape)
        .background(NnoworkTheme.colors.elevatedSurface, shape)
        .border(NnoworkLine.hairline, NnoworkTheme.colors.divider, shape)
        .padding(NnoworkSpacing.sm)
}

// 集點卡：3x3 迷你格子，前 5 格已蓋章。
@Composable
private fun StampCardDemo() {
    val colors = NnoworkTheme.colors
    val shape = RoundedCornerShape(NnoworkRadius.small)
    Column(Modifier.demoBox(), verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
                repeat(3) { col ->
                    val stamped = row * 3 + col < 5
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(NnoworkSpacing.xl)
                            .clip(shape)
                            .background(if (stamped) colors.accent else colors.surface, shape)
                            .border(NnoworkLine.hairline, colors.divider, shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (stamped) Icon(Icons.Filled.Check, null, tint = colors.onAccent, modifier = Modifier.size(NnoworkSize.compactIcon))
                    }
                }
            }
        }
    }
}

// 薪水計時器：迷你即時面板（金額為示意，不翻譯）。
@Composable
private fun SalaryDemo() {
    val colors = NnoworkTheme.colors
    Column(Modifier.demoBox(), verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.manual_demo_earned_today),
                style = pixelTextStyle(NnoworkTheme.type.caption, color = colors.secondaryText),
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.HourglassBottom, null, tint = colors.highlight, modifier = Modifier.size(NnoworkSpacing.sm))
            Spacer(Modifier.width(NnoworkSpacing.xxs))
            Text("3:12:40", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, colors.highlight, monospace = true))
        }
        Text("NT$1,247", style = pixelTextStyle(NnoworkTheme.type.display, FontWeight.Bold, colors.success, monospace = true))
        Box(Modifier.fillMaxWidth().height(NnoworkSpacing.tight).clip(CircleShape).background(colors.divider)) {
            Box(Modifier.fillMaxWidth(0.68f).height(NnoworkSpacing.tight).clip(CircleShape).background(colors.success))
        }
    }
}

private enum class DemoShift { Off, Morning, Evening }

// 排班月曆示意：休假／早班／晚班各一個顏色。
@Composable
private fun ScheduleCalendarDemo() {
    val colors = NnoworkTheme.colors
    val week = listOf(
        DemoShift.Off, DemoShift.Morning, DemoShift.Morning, DemoShift.Evening,
        DemoShift.Evening, DemoShift.Morning, DemoShift.Off,
    )
    val locale = Locale.getDefault()
    // 日一二三四五六（日為週首，與 iOS 相同）
    val weekdays = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)
    fun DemoShift.fill(): Color = when (this) {
        DemoShift.Off -> colors.warning
        DemoShift.Morning -> colors.info
        DemoShift.Evening -> colors.success
    }
    Column(Modifier.demoBox(), verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
        Text(stringResource(R.string.manual_demo_calendar), style = pixelTextStyle(NnoworkTheme.type.caption, color = colors.secondaryText))
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
            week.forEachIndexed { i, shift ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
                    Text(weekdays[i].getDisplayName(TextStyle.NARROW, locale), style = pixelTextStyle(NnoworkTheme.type.caption, color = colors.secondaryText))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NnoworkSpacing.xxl)
                            .clip(RoundedCornerShape(NnoworkRadius.small))
                            .background(shift.fill()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("${i + 4}", style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, colors.onStrong))
                        when (shift) {
                            DemoShift.Morning -> Icon(Icons.Filled.WbSunny, null, tint = colors.onStrong, modifier = Modifier.size(NnoworkSpacing.sm))
                            DemoShift.Evening -> Icon(Icons.Filled.Bedtime, null, tint = colors.onStrong, modifier = Modifier.size(NnoworkSpacing.sm))
                            DemoShift.Off -> Unit
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
            LegendDot(colors.warning, stringResource(R.string.manual_demo_off))
            LegendDot(colors.info, stringResource(R.string.manual_demo_morning))
            LegendDot(colors.success, stringResource(R.string.manual_demo_evening))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
        Box(Modifier.size(NnoworkSpacing.snug).clip(RoundedCornerShape(NnoworkSpacing.xxs / 2)).background(color))
        Text(label, style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText))
    }
}

// 取暖樹洞：迷你貼文卡 + 表情列。
@Composable
private fun TreeHoleDemo() {
    val colors = NnoworkTheme.colors
    Column(Modifier.demoBox(), verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            Box(Modifier.size(NnoworkSpacing.lg).clip(CircleShape).background(colors.teal.copy(alpha = 0.4f)))
            Text(stringResource(R.string.manual_demo_anon), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, colors.onElevated))
            Text(
                stringResource(R.string.manual_demo_clerk),
                style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, colors.identityBadgeText),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.identityBadgeBG)
                    .padding(horizontal = NnoworkSpacing.xs, vertical = NnoworkSpacing.xxs / 2),
            )
        }
        Text(stringResource(R.string.manual_demo_post), style = pixelTextStyle(NnoworkTheme.type.small, color = colors.onElevated))
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.md)) {
            listOf("❤️ 12", "💪 8", "😢 3").forEach {
                Text(it, style = pixelTextStyle(NnoworkTheme.type.caption, color = colors.secondaryText))
            }
        }
    }
}
