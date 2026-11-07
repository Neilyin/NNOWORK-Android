package com.neilyin.nnowork.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.neilyin.nnowork.R
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(NnoworkRadius.control))
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.theme_shop_count, CardTheme.entries.size),
                style = pixelTextStyle(14.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(if (expanded) R.string.theme_shop_collapse else R.string.theme_shop_expand),
                style = pixelTextStyle(12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            Text(
                stringResource(R.string.theme_shop_hint),
                style = pixelTextStyle(12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            CardTheme.entries.chunked(THEME_COLUMNS).forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
    val shape = RoundedCornerShape(if (theme.cellCornerRadius > 0.dp) 12.dp else 2.dp)
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 132.dp)
            .clip(RoundedCornerShape(NnoworkRadius.card))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(if (selected) 3.dp else 2.dp, border, RoundedCornerShape(NnoworkRadius.card))
            .semantics(mergeDescendants = true) {
                contentDescription = "$title, $state"
                role = Role.Button
                this.selected = selected
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(shape)
                .background(theme.backgroundColor)
                .border(2.dp, theme.borderColor, shape),
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
                        .background(Color.Black.copy(alpha = 0.56f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(
                            theme.coinCost.toString(),
                            style = pixelTextStyle(11.sp, FontWeight.Bold, Color.White),
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
            style = pixelTextStyle(12.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            state,
            style = pixelTextStyle(10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
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

    ModalBottomSheet(
        onDismissRequest = { onDismiss(false, didStamp) },
        sheetState = sheetState,
        containerColor = theme.backgroundColor,
        contentColor = theme.titleColor,
        scrimColor = Color.Black.copy(alpha = 0.58f),
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
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
                        style = pixelTextStyle(13.sp, color = theme.titleColor),
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.cardSurfaceColor)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PixelButton(
                    onClick = {
                        onBuy()
                        onDismiss(true, didStamp)
                    },
                    enabled = canAfford,
                    backgroundColor = theme.accentColor,
                    borderColor = theme.primaryColor,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.theme_preview_buy, theme.coinCost),
                        style = pixelTextStyle(15.sp, FontWeight.Bold, theme.palette.onAccent),
                    )
                }
                Text(
                    stringResource(
                        if (canAfford) R.string.theme_preview_balance else R.string.theme_preview_insufficient,
                        coinBalance,
                    ),
                    style = pixelTextStyle(11.sp, color = theme.titleColor),
                )
                TextButton(
                    onClick = { onDismiss(false, didStamp) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(
                        stringResource(R.string.theme_preview_close),
                        style = pixelTextStyle(14.sp, FontWeight.Bold, theme.titleColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewHeader(theme: CardTheme, onClose: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(theme.titleRes),
                style = pixelTextStyle(25.sp, FontWeight.Bold, theme.titleColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.theme_preview_badge),
                style = pixelTextStyle(11.sp, FontWeight.Bold, theme.titleColor),
                modifier = Modifier
                    .background(theme.accentColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.theme_preview_close),
                    tint = theme.titleColor,
                )
            }
        }
        Text(
            stringResource(R.string.theme_preview_hint),
            style = pixelTextStyle(12.sp, color = theme.titleColor),
            lineHeight = 19.sp,
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
            .border(2.dp, theme.titleColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Visibility, contentDescription = null, tint = theme.titleColor.copy(alpha = 0.68f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.theme_preview_sample),
                style = pixelTextStyle(11.sp, FontWeight.Bold, theme.titleColor),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.cardSurfaceColor, RoundedCornerShape(maxOf(theme.cellCornerRadius, 8.dp)))
                .border(2.dp, theme.borderColor.copy(alpha = 0.72f), RoundedCornerShape(maxOf(theme.cellCornerRadius, 8.dp)))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.theme_preview_card_title),
                    style = pixelTextStyle(15.sp, FontWeight.Bold, theme.titleColor),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$stampedCount / $totalCells",
                    style = pixelTextStyle(14.sp, FontWeight.Bold, theme.titleColor),
                )
            }
            repeat(2) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                .background(theme.cardSurfaceColor, RoundedCornerShape(10.dp))
                .border(2.dp, theme.borderColor.copy(alpha = 0.62f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(theme.accentColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.theme_preview_post), style = pixelTextStyle(13.sp, FontWeight.Bold, theme.titleColor))
                Text(stringResource(R.string.theme_preview_time), style = pixelTextStyle(11.sp, color = theme.titleColor))
            }
        }

        PixelButton(
            onClick = onStamp,
            enabled = !isFull,
            backgroundColor = theme.accentColor,
            borderColor = theme.primaryColor,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(if (isFull) R.string.theme_preview_full else R.string.theme_preview_add_stamp),
                style = pixelTextStyle(15.sp, FontWeight.Bold, theme.palette.onAccent),
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
    val shape = RoundedCornerShape(theme.cellCornerRadius)
    val fill = if (stamped) theme.stampedCellColor else theme.emptyCellColor
    val borderOpacity = if (stamped) theme.stampedCellBorderOpacity else theme.emptyCellBorderOpacity
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(fill)
            .border(2.dp, theme.borderColor.copy(alpha = borderOpacity), shape),
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
                style = pixelTextStyle(12.sp, FontWeight.Bold, theme.palette.onContent),
            )
        }
    }
}

private const val THEME_COLUMNS = 3
