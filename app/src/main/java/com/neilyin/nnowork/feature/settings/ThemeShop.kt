package com.neilyin.nnowork.feature.settings

import androidx.compose.material3.HorizontalDivider
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.PixelButtonVariant
import com.neilyin.nnowork.ui.theme.NnoworkSheetDefaults
import androidx.compose.foundation.background
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.neilyin.nnowork.R
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.ThemeStampMark
import com.neilyin.nnowork.ui.theme.isUnlocked
import com.neilyin.nnowork.ui.theme.palette
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import com.neilyin.nnowork.ui.theme.stampTiltDegrees

@Composable
internal fun ThemeShopGrid(
    selectedThemeId: Int,
    unlockedStyles: Collection<Int>,
    onSelect: (CardTheme) -> Unit,
    onPreview: (CardTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = NnoworkSize.minTouchTarget)
                .clip(RoundedCornerShape(NnoworkRadius.control))
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = NnoworkSpacing.xxs, vertical = NnoworkSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.theme_shop_count, CardTheme.entries.size),
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(if (expanded) R.string.theme_shop_collapse else R.string.theme_shop_expand),
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.primaryText),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = NnoworkTheme.colors.primaryText,
            )
        }

        if (expanded) {
            Text(
                stringResource(R.string.theme_shop_hint),
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
            )
            CardTheme.entries.chunked(THEME_COLUMNS).forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
                ) {
                    rowThemes.forEach { theme ->
                        val unlocked = theme.isUnlocked(unlockedStyles)
                        ThemeShopTile(
                            theme = theme,
                            selected = selectedThemeId == theme.id,
                            unlocked = unlocked,
                            onClick = { if (unlocked) onSelect(theme) else onPreview(theme) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(THEME_COLUMNS - rowThemes.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ThemeShopTile(
    theme: CardTheme,
    selected: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(theme.titleRes)
    val state = when {
        selected -> stringResource(R.string.theme_state_using)
        unlocked -> stringResource(R.string.theme_state_available)
        else -> stringResource(R.string.theme_state_locked, theme.coinCost)
    }
    val shape = RoundedCornerShape(NnoworkRadius.field)
    val border = if (selected) NnoworkTheme.colors.accentForeground else NnoworkTheme.colors.divider

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 132.dp)
            .clip(RoundedCornerShape(NnoworkRadius.card))
            .background(if (selected) NnoworkTheme.colors.accentSoft else NnoworkTheme.colors.appBG)
            .border(if (selected) NnoworkLine.control else NnoworkLine.hairline, border, RoundedCornerShape(NnoworkRadius.card))
            .semantics(mergeDescendants = true) {
                contentDescription = "$title, $state"
                role = Role.Button
                this.selected = selected
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(shape)
                .background(theme.backgroundColor)
                .border(NnoworkLine.hairline, theme.borderColor, shape),
            contentAlignment = Alignment.Center,
        ) {
            ThemeStampMark(
                theme = theme,
                index = theme.id,
                tint = theme.accentColor,
                modifier = Modifier.size(30.dp),
            )
            if (!unlocked) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(NnoworkTheme.colors.scrim),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = NnoworkTheme.colors.onStrong, modifier = Modifier.size(18.dp))
                        Text(
                            theme.coinCost.toString(),
                            style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.onStrong),
                        )
                    }
                }
            } else if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(21.dp)
                        .background(theme.accentColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = theme.palette.onAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            title,
            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            state,
            style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemePreviewSheet(
    theme: CardTheme,
    coinBalance: Int,
    onBuy: () -> Unit,
    onDismiss: (didBuy: Boolean, didStamp: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var stampedCount by rememberSaveable(theme.id) { mutableIntStateOf(4) }
    var didStamp by rememberSaveable(theme.id) { mutableStateOf(false) }
    val totalCells = 8
    val isFull = stampedCount >= totalCells
    val canAfford = coinBalance >= theme.coinCost

    val scrim = NnoworkTheme.colors.scrim
    // 預覽 sheet 直接套用被預覽主題的整套語意色，使用者看到的就是購買後的實際外觀。
    NnoworkTheme(cardTheme = theme, applySystemBars = false) {
    ModalBottomSheet(
        onDismissRequest = { onDismiss(false, didStamp) },
        sheetState = sheetState,
        shape = NnoworkSheetDefaults.shape,
        containerColor = NnoworkTheme.colors.appBG,
        contentColor = NnoworkTheme.colors.primaryText,
        scrimColor = scrim,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .navigationBarsPadding(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(NnoworkSpacing.roomy),
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.roomy),
            ) {
                item {
                    ThemePreviewHeader(
                        theme = theme,
                        onClose = { onDismiss(false, didStamp) },
                    )
                }
                item {
                    ThemePreviewSample(
                        theme = theme,
                        stampedCount = stampedCount,
                        totalCells = totalCells,
                        isFull = isFull,
                        onStamp = {
                            if (!isFull) {
                                stampedCount++
                                didStamp = true
                            }
                        },
                    )
                }
                item {
                    Text(
                        stringResource(theme.descriptionRes),
                        style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.secondaryText),
                        textAlign = TextAlign.Center,
                        lineHeight = NnoworkTheme.type.body * 1.5f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            HorizontalDivider(thickness = NnoworkLine.hairline, color = NnoworkTheme.colors.divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NnoworkTheme.colors.surface)
                    .padding(horizontal = NnoworkSpacing.roomy, vertical = NnoworkSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
            ) {
                PixelButton(
                    onClick = {
                        onBuy()
                        onDismiss(true, didStamp)
                    },
                    enabled = canAfford,
                    variant = PixelButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.theme_preview_buy, theme.coinCost),
                        style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.buttonText),
                    )
                }
                Text(
                    stringResource(
                        if (canAfford) R.string.theme_preview_balance else R.string.theme_preview_insufficient,
                        coinBalance,
                    ),
                    style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
                )
                TextButton(
                    onClick = { onDismiss(false, didStamp) },
                    modifier = Modifier.defaultMinSize(minHeight = NnoworkSize.minTouchTarget),
                ) {
                    Text(
                        stringResource(R.string.theme_preview_close),
                        style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.accentForeground),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun ThemePreviewHeader(theme: CardTheme, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(theme.titleRes),
                style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.theme_preview_badge),
                style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.accentForeground),
                modifier = Modifier
                    .background(NnoworkTheme.colors.accentSoft, RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
            Spacer(Modifier.width(NnoworkSpacing.xxs))
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(NnoworkSize.minTouchTarget),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.theme_preview_close),
                    tint = NnoworkTheme.colors.primaryText,
                )
            }
        }
        Text(
            stringResource(R.string.theme_preview_hint),
            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
            lineHeight = NnoworkTheme.type.small * 1.6f,
        )
    }
}

@Composable
private fun ThemePreviewSample(
    theme: CardTheme,
    stampedCount: Int,
    totalCells: Int,
    isFull: Boolean,
    onStamp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(NnoworkSpacing.cozy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.cozy),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Visibility, contentDescription = null, tint = NnoworkTheme.colors.secondaryText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(NnoworkSpacing.tight))
            Text(
                stringResource(R.string.theme_preview_sample),
                style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.secondaryText),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.cardSurfaceColor, RoundedCornerShape(maxOf(theme.cellCornerRadius, 8.dp)))
                .border(NnoworkLine.emphasis, theme.borderColor, RoundedCornerShape(maxOf(theme.cellCornerRadius, 8.dp)))
                .padding(NnoworkSpacing.cozy),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.theme_preview_card_title),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, theme.titleColor),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$stampedCount / $totalCells",
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, theme.titleColor),
                )
            }
            repeat(2) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                    repeat(4) { column ->
                        val index = row * 4 + column
                        ThemePreviewCell(
                            theme = theme,
                            index = index,
                            stamped = index < stampedCount,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NnoworkRadius.card))
                .background(NnoworkTheme.colors.appBG)
                .border(NnoworkLine.hairline, NnoworkTheme.colors.divider, RoundedCornerShape(NnoworkRadius.card))
                .padding(NnoworkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(NnoworkTheme.colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = NnoworkTheme.colors.accentForeground, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(NnoworkSpacing.snug))
            Column {
                Text(stringResource(R.string.theme_preview_post), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                Text(stringResource(R.string.theme_preview_time), style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText))
            }
        }

        PixelButton(
            onClick = onStamp,
            enabled = !isFull,
            variant = PixelButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(if (isFull) R.string.theme_preview_full else R.string.theme_preview_add_stamp),
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.buttonText),
            )
        }
    }
}

@Composable
private fun ThemePreviewCell(
    theme: CardTheme,
    index: Int,
    stamped: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(maxOf(theme.cellCornerRadius, NnoworkRadius.small))
    val fill = if (stamped) theme.stampedCellColor else theme.emptyCellColor
    val borderOpacity = if (stamped) theme.stampedCellBorderOpacity else theme.emptyCellBorderOpacity
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(fill)
            .border(NnoworkLine.control, theme.borderColor.copy(alpha = borderOpacity), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (stamped) {
            ThemeStampMark(
                theme = theme,
                index = index,
                tint = theme.stampIconColor,
                modifier = Modifier
                    .fillMaxSize(0.58f)
                    .rotate(stampTiltDegrees(index)),
            )
        } else {
            Text(
                (index + 1).toString(),
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, theme.palette.onContent),
            )
        }
    }
}

private const val THEME_COLUMNS = 3
