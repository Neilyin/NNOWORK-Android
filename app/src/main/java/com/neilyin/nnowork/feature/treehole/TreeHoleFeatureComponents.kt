package com.neilyin.nnowork.feature.treehole

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import java.util.Locale
import kotlinx.coroutines.delay

/** UI/feed shared contract. [apiValue] can be passed straight to `p_post_type`. */
enum class TreeHoleFeedMode(val apiValue: String?) {
    ALL(null),
    STAMP("stamp"),
    QUESTION("question"),
}

@Immutable
data class TreeHoleFeedFilterStrings(
    val all: String = "全部",
    val stamp: String = "心情",
    val question: String = "解惑",
    val allIndustries: String = "全部職業",
    val openIndustryFilter: String = "開啟職業篩選",
    val clearIndustryFilter: String = "清除職業篩選",
    val selectedState: String = "已選取",
    val notSelectedState: String = "未選取",
)

/**
 * Feed 的雙軸篩選列。類型是單選 tab，行業由另一個 multi-select sheet 處理。
 * 整排可水平捲動，避免長文案或多語系擠壓 touch target。
 */
@Composable
fun TreeHoleFeedFilterBar(
    selectedMode: TreeHoleFeedMode,
    industryLabel: String,
    industryFilterActive: Boolean,
    onModeSelected: (TreeHoleFeedMode) -> Unit,
    onOpenIndustryFilter: () -> Unit,
    onClearIndustryFilter: () -> Unit,
    modifier: Modifier = Modifier,
    strings: TreeHoleFeedFilterStrings = TreeHoleFeedFilterStrings(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeedModePill(
            label = strings.all,
            icon = Icons.AutoMirrored.Filled.List,
            selected = selectedMode == TreeHoleFeedMode.ALL,
            selectedState = strings.selectedState,
            notSelectedState = strings.notSelectedState,
            onClick = { onModeSelected(TreeHoleFeedMode.ALL) },
        )
        FeedModePill(
            label = strings.stamp,
            icon = Icons.Filled.EmojiEvents,
            selected = selectedMode == TreeHoleFeedMode.STAMP,
            selectedState = strings.selectedState,
            notSelectedState = strings.notSelectedState,
            onClick = { onModeSelected(TreeHoleFeedMode.STAMP) },
        )
        FeedModePill(
            label = strings.question,
            icon = Icons.Filled.QuestionAnswer,
            selected = selectedMode == TreeHoleFeedMode.QUESTION,
            selectedState = strings.selectedState,
            notSelectedState = strings.notSelectedState,
            selectedColor = QUESTION_ACCENT,
            onClick = { onModeSelected(TreeHoleFeedMode.QUESTION) },
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .width(1.dp)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        )

        Row(
            modifier = Modifier
                .heightIn(min = MIN_TOUCH_TARGET)
                .widthIn(min = MIN_TOUCH_TARGET)
                .background(
                    if (industryFilterActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                )
                .border(
                    2.dp,
                    if (industryFilterActive) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.outline,
                    RectangleShape,
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = strings.openIndustryFilter,
                    onClick = onOpenIndustryFilter,
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = "${strings.openIndustryFilter}，$industryLabel"
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = null,
                tint = if (industryFilterActive) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = industryLabel.ifBlank { strings.allIndustries },
                style = pixelTextStyle(
                    13.sp,
                    if (industryFilterActive) FontWeight.Bold else FontWeight.Medium,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (industryFilterActive) {
            PixelIconAction(
                icon = Icons.Filled.Close,
                contentDescription = strings.clearIndustryFilter,
                onClick = onClearIndustryFilter,
            )
        }
    }
}

@Composable
private fun FeedModePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedState: String,
    notSelectedState: String,
    onClick: () -> Unit,
    selectedColor: Color = PixelColors.green,
) {
    Row(
        modifier = Modifier
            .heightIn(min = MIN_TOUCH_TARGET)
            .widthIn(min = MIN_TOUCH_TARGET)
            .background(if (selected) selectedColor else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                2.dp,
                if (selected) selectedColor else MaterialTheme.colorScheme.outline,
                RectangleShape,
            )
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                stateDescription = if (selected) selectedState else notSelectedState
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) PixelColors.black else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = label,
            style = pixelTextStyle(
                12.sp,
                if (selected) FontWeight.Bold else FontWeight.Medium,
                if (selected) PixelColors.black else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Immutable
data class TreeHoleIndustryOptionUi(
    val key: String,
    val label: String = key,
    val color: Color = PixelColors.green,
    val icon: ImageVector = Icons.Filled.Work,
)

@Immutable
data class TreeHoleIndustryFilterStrings(
    val title: String = "選擇職業",
    val close: String = "關閉職業篩選",
    val all: String = "全部",
    val mine: String = "我的職業",
    val clear: String = "清除篩選",
    val showingAll: String = "目前顯示全部",
    val selectedCountTemplate: String = "已選 %d 個",
    val applyAll: String = "顯示全部貼文",
    val applyCountTemplate: String = "套用（%d 個職業）",
    val selectedState: String = "已選取",
    val notSelectedState: String = "未選取",
)

/**
 * 多選行業 sheet。[selectedIndustries] 空集合代表「全部」。
 * 使用者只會在按下套用時收到 [onApply]，點 chip 不會觸發網路重載。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeHoleIndustryFilterSheet(
    industries: List<TreeHoleIndustryOptionUi>,
    selectedIndustries: Set<String>,
    myIndustry: String?,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    strings: TreeHoleIndustryFilterStrings = TreeHoleIndustryFilterStrings(),
) {
    val options = remember(industries) { industries.distinctBy { it.key } }
    val validKeys = remember(options) { options.mapTo(linkedSetOf()) { it.key } }
    var draft by remember(selectedIndustries, validKeys) {
        mutableStateOf(selectedIndustries.intersect(validKeys))
    }
    val validMyIndustry = myIndustry?.takeIf { it in validKeys }
    val summary = if (draft.isEmpty()) {
        strings.showingAll
    } else {
        strings.selectedCountTemplate.withCount(draft.size)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onBackground) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.title,
                        style = pixelTextStyle(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                    )
                    Text(
                        text = summary,
                        style = pixelTextStyle(12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                PixelIconAction(
                    icon = Icons.Filled.Close,
                    contentDescription = strings.close,
                    onClick = onDismiss,
                )
            }

            PixelDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterQuickAction(
                    label = strings.all,
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = { draft = emptySet() },
                )
                validMyIndustry?.let { mine ->
                    FilterQuickAction(
                        label = strings.mine,
                        icon = Icons.Filled.Person,
                        onClick = { draft = setOf(mine) },
                    )
                }
                if (draft.isNotEmpty()) {
                    FilterQuickAction(
                        label = strings.clear,
                        icon = Icons.Filled.Close,
                        onClick = { draft = emptySet() },
                    )
                }
            }

            PixelDivider()

            if (options.isEmpty()) {
                Text(
                    text = strings.showingAll,
                    style = pixelTextStyle(14.sp, color = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.padding(20.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 112.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 112.dp, max = 420.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(options, key = { it.key }) { option ->
                        IndustryToggleChip(
                            option = option,
                            selected = option.key in draft,
                            selectedState = strings.selectedState,
                            notSelectedState = strings.notSelectedState,
                            onToggle = {
                                draft = if (option.key in draft) draft - option.key else draft + option.key
                            },
                        )
                    }
                }
            }

            PixelDivider()

            PixelButton(
                onClick = {
                    onApply(draft)
                    onDismiss()
                },
                backgroundColor = PixelColors.green,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics { role = Role.Button },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = if (draft.isEmpty()) strings.applyAll else strings.applyCountTemplate.withCount(draft.size),
                    style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black),
                )
            }
        }
    }
}

@Immutable
data class TreeHoleIndustryPickerStrings(
    val title: String = "選擇產業",
    val close: String = "關閉產業選擇",
    val apply: String = "套用",
    val selectedState: String = "已選取",
    val notSelectedState: String = "未選取",
)

/** Profile setup/edit 使用的單選版本；選擇只寫入 draft，按下套用才回傳。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeHoleIndustryPickerSheet(
    industries: List<TreeHoleIndustryOptionUi>,
    selectedIndustry: String?,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    strings: TreeHoleIndustryPickerStrings = TreeHoleIndustryPickerStrings(),
) {
    val options = remember(industries) { industries.distinctBy { it.key } }
    val validKeys = remember(options) { options.mapTo(linkedSetOf()) { it.key } }
    var draft by remember(selectedIndustry, validKeys) {
        mutableStateOf(selectedIndustry?.takeIf { it in validKeys })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onBackground) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.title,
                    style = pixelTextStyle(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.weight(1f),
                )
                PixelIconAction(
                    icon = Icons.Filled.Close,
                    contentDescription = strings.close,
                    onClick = onDismiss,
                )
            }

            PixelDivider()

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 112.dp, max = 460.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(options, key = { it.key }) { option ->
                    IndustryRadioChip(
                        option = option,
                        selected = option.key == draft,
                        selectedState = strings.selectedState,
                        notSelectedState = strings.notSelectedState,
                        onSelect = { draft = option.key },
                    )
                }
            }

            PixelDivider()

            val canApply = draft != null
            PixelButton(
                onClick = {
                    val selected = draft ?: return@PixelButton
                    onApply(selected)
                    onDismiss()
                },
                enabled = canApply,
                backgroundColor = if (canApply) PixelColors.green else PixelColors.gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics { role = Role.Button },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(strings.apply, style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black))
            }
        }
    }
}

@Composable
private fun IndustryRadioChip(
    option: TreeHoleIndustryOptionUi,
    selected: Boolean,
    selectedState: String,
    notSelectedState: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET)
            .background(if (selected) option.color else MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, if (selected) option.color else MaterialTheme.colorScheme.outline, RectangleShape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .semantics(mergeDescendants = true) {
                stateDescription = if (selected) selectedState else notSelectedState
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = if (selected) PixelColors.black else option.color,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = option.label,
            style = pixelTextStyle(
                12.sp,
                if (selected) FontWeight.Bold else FontWeight.Medium,
                if (selected) PixelColors.black else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = PixelColors.black,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun FilterQuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    PixelButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = MIN_TOUCH_TARGET)
            .semantics { role = Role.Button },
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(label, style = pixelTextStyle(12.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant))
    }
}

@Composable
private fun IndustryToggleChip(
    option: TreeHoleIndustryOptionUi,
    selected: Boolean,
    selectedState: String,
    notSelectedState: String,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET)
            .background(if (selected) option.color else MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, if (selected) option.color else MaterialTheme.colorScheme.outline, RectangleShape)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .semantics(mergeDescendants = true) {
                stateDescription = if (selected) selectedState else notSelectedState
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = if (selected) PixelColors.black else option.color,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = option.label,
            style = pixelTextStyle(
                12.sp,
                if (selected) FontWeight.Bold else FontWeight.Medium,
                if (selected) PixelColors.black else MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = PixelColors.black,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Immutable
data class TreeHoleQuestionQuotaUi(
    val limit: Int = 3,
    val remaining: Int = 3,
    val joined: Boolean = true,
)

@Immutable
data class TreeHoleQuestionIdentityUi(
    val nickname: String,
    val industry: String,
    val role: String = "",
    val companyName: String = "",
    val showCompany: Boolean = false,
    val accentColor: Color = PixelColors.green,
)

@Immutable
data class TreeHoleQuestionStrings(
    val title: String = "問問大家",
    val close: String = "關閉提問",
    val introTitle: String = "你的問題會經過審核後出現在樹洞",
    val quotaUnknown: String = "沒通過的問題不算額度，可以再問一次",
    val quotaRemainingTemplate: String = "今天還能問 %d 則（沒通過的不算額度）",
    val quotaExhausted: String = "今天的提問額度用完了，明天再來",
    val notJoined: String = "要先加入樹洞才能發問",
    val editorLabel: String = "問題",
    val editorPlaceholder: String = "想問問同溫層什麼？例如：這種主管該忍多久？",
    val lengthHint: String = "問題請寫 5～200 字",
    val submit: String = "送出問題",
    val submitting: String = "送出中",
    val cancel: String = "先不問",
    val avatarSuffix: String = "的頭像",
)

/**
 * 提問 sheet 只處理 UI 驗證與重複點擊防護。額度與真正送出結果仍以呼叫端為準。
 * [quota] 為 null 時樂觀放行，符合後端才是最終把關者的規則。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeHoleQuestionSheet(
    identity: TreeHoleQuestionIdentityUi,
    quota: TreeHoleQuestionQuotaUi?,
    submitting: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    quotaMessage: String? = null,
    initialContent: String = "",
    strings: TreeHoleQuestionStrings = TreeHoleQuestionStrings(),
) {
    var content by rememberSaveable(initialContent) { mutableStateOf(initialContent.take(MAX_QUESTION_LENGTH)) }
    // Compose 可能在 ViewModel 的 submitting 回寫前多收到一次 tap，因此先在 UI thread 同步上鎖。
    var locallyLocked by remember { mutableStateOf(false) }
    var observedExternalSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(locallyLocked, submitting) {
        if (!locallyLocked) return@LaunchedEffect
        if (submitting) {
            observedExternalSubmitting = true
        } else if (observedExternalSubmitting) {
            locallyLocked = false
            observedExternalSubmitting = false
        } else {
            // 呼叫端同步拒絕送出或尚未接上 loading 時，不會永久鎖住按鈕。
            delay(SUBMIT_DEBOUNCE_MILLIS)
            if (!submitting && !observedExternalSubmitting) locallyLocked = false
        }
    }

    val trimmed = content.trim()
    val quotaAllowsSubmit = quota?.let { it.joined && it.remaining > 0 } ?: true
    val busy = submitting || locallyLocked
    val validLength = trimmed.length in MIN_QUESTION_LENGTH..MAX_QUESTION_LENGTH
    val canSubmit = validLength && quotaAllowsSubmit && !busy
    val resolvedQuotaMessage = quotaMessage ?: when {
        quota == null -> strings.quotaUnknown
        !quota.joined -> strings.notJoined
        quota.remaining <= 0 -> strings.quotaExhausted
        else -> strings.quotaRemainingTemplate.withCount(quota.remaining)
    }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        modifier = modifier,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onBackground) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.title,
                    style = pixelTextStyle(19.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.weight(1f),
                )
                PixelIconAction(
                    icon = Icons.Filled.Close,
                    contentDescription = strings.close,
                    enabled = !busy,
                    onClick = onDismiss,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelCard(
                        // Sheet 是深色底，使用實色 content surface，避免半透明藍色與深色底混合後讓文字失去對比。
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        borderColor = QUESTION_ACCENT,
                        borderWidth = 2.dp,
                    )
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = null,
                    tint = QUESTION_ACCENT_DARK,
                    modifier = Modifier.size(26.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        strings.introTitle,
                        style = pixelTextStyle(14.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                    Text(
                        resolvedQuotaMessage,
                        style = pixelTextStyle(12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }

            QuestionIdentityCard(identity = identity, avatarSuffix = strings.avatarSuffix)

            OutlinedTextField(
                value = content,
                onValueChange = { content = it.take(MAX_QUESTION_LENGTH) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = strings.editorLabel },
                enabled = !busy,
                label = { Text(strings.editorLabel) },
                placeholder = { Text(strings.editorPlaceholder) },
                supportingText = {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (trimmed.isNotEmpty() && !validLength) {
                            Text(
                                strings.lengthHint,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        Text("${content.length} / $MAX_QUESTION_LENGTH")
                    }
                },
                isError = (trimmed.isNotEmpty() && !validLength) || errorMessage != null,
                minLines = 5,
                maxLines = 8,
                shape = RectangleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    cursorColor = QUESTION_ACCENT_DARK,
                    focusedBorderColor = QUESTION_ACCENT,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    // supporting text 畫在輸入框容器外，必須對應 sheet 背景，不能沿用淺色輸入面的深色文字。
                    focusedSupportingTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedSupportingTextColor = MaterialTheme.colorScheme.onBackground,
                    disabledSupportingTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    errorTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    errorCursorColor = MaterialTheme.colorScheme.error,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorLabelColor = MaterialTheme.colorScheme.error,
                    errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                    errorSupportingTextColor = MaterialTheme.colorScheme.error,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                ),
            )

            errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = pixelTextStyle(13.sp, FontWeight.Bold, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }

            PixelButton(
                onClick = {
                    if (!canSubmit) return@PixelButton
                    locallyLocked = true
                    onSubmit(trimmed)
                },
                enabled = canSubmit,
                backgroundColor = if (canSubmit) QUESTION_ACCENT else PixelColors.gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .semantics {
                        role = Role.Button
                        if (busy) stateDescription = strings.submitting
                    },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .semantics { contentDescription = strings.submitting },
                        color = PixelColors.black,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(strings.submitting, style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black))
                } else {
                    Text(strings.submit, style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black))
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = PixelColors.black,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            PixelButton(
                onClick = onDismiss,
                enabled = !busy,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_TOUCH_TARGET)
                    .semantics { role = Role.Button },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    strings.cancel,
                    style = pixelTextStyle(14.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun QuestionIdentityCard(
    identity: TreeHoleQuestionIdentityUi,
    avatarSuffix: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TreeHoleInitialsAvatar(
            nickname = identity.nickname,
            accentColor = identity.accentColor,
            size = 42.dp,
            contentDescription = "${identity.nickname}$avatarSuffix",
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    identity.nickname,
                    style = pixelTextStyle(14.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                identity.role.takeIf { it.isNotBlank() }?.let { role ->
                    Text(
                        role,
                        style = pixelTextStyle(10.sp, FontWeight.Bold, PixelColors.black),
                        modifier = Modifier
                            .background(PixelColors.yellow)
                            .border(1.dp, PixelColors.black, RectangleShape)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                            .semantics { contentDescription = "職稱：$role" },
                        maxLines = 1,
                    )
                }
            }
            Text(
                buildList {
                    identity.industry.takeIf { it.isNotBlank() }?.let(::add)
                    if (identity.showCompany) identity.companyName.takeIf { it.isNotBlank() }?.let(::add)
                }.joinToString(" · ").ifBlank { "職場倖存者" },
                style = pixelTextStyle(12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

enum class TreeHoleAnswerBadgeTierUi(val threshold: Int) {
    Rookie(0),
    Passerby(100),
    Senior(500),
    Strategist(2_000),
    Master(5_000),
}

@Immutable
data class TreeHoleAnswerBadgeStrings(
    val rookie: String = "樹洞新人",
    val passerby: String = "熱心路人",
    val senior: String = "樹洞前輩",
    val strategist: String = "職場軍師",
    val master: String = "解惑大師",
    val expSummaryTemplate: String = "%d EXP · %s 個有用解答",
    val nextTierTemplate: String = "再 %d EXP 成為「%s」",
    val highestTier: String = "已經是最高級了，謝謝你一直在回答",
)

fun treeHoleAnswerBadgeTierFor(exp: Int): TreeHoleAnswerBadgeTierUi {
    val safeExp = exp.coerceAtLeast(0)
    return TreeHoleAnswerBadgeTierUi.entries.last { safeExp >= it.threshold }
}

/** Feed 卡和留言預設只顯示第 2 級（100 EXP）以上；個人頁可用 [showAllTiers]。 */
@Composable
fun TreeHoleAnswerBadgeMark(
    exp: Int,
    modifier: Modifier = Modifier,
    showAllTiers: Boolean = false,
    strings: TreeHoleAnswerBadgeStrings = TreeHoleAnswerBadgeStrings(),
) {
    val safeExp = exp.coerceAtLeast(0)
    val tier = treeHoleAnswerBadgeTierFor(safeExp)
    val visible = showAllTiers || tier.threshold >= TreeHoleAnswerBadgeTierUi.Passerby.threshold
    if (!visible) return

    val name = tier.displayName(strings)
    val color = tier.accessibleColor()
    Row(
        modifier = modifier
            .wrapContentWidth()
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color, RectangleShape)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name，$safeExp EXP"
            }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = tier.icon(),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp),
        )
        Text(name, style = pixelTextStyle(10.sp, FontWeight.Bold, color))
    }
}

@Composable
fun TreeHoleAnswerBadgeProgressCard(
    exp: Int,
    helpfulCount: Int,
    modifier: Modifier = Modifier,
    strings: TreeHoleAnswerBadgeStrings = TreeHoleAnswerBadgeStrings(),
) {
    val safeExp = exp.coerceAtLeast(0)
    val safeHelpfulCount = helpfulCount.coerceAtLeast(0)
    val tier = treeHoleAnswerBadgeTierFor(safeExp)
    val next = tier.nextTier()
    val currentName = tier.displayName(strings)
    val color = tier.accessibleColor()
    val progress = if (next == null) {
        1f
    } else {
        val range = (next.threshold - tier.threshold).coerceAtLeast(1)
        ((safeExp - tier.threshold).toFloat() / range.toFloat()).coerceIn(0f, 1f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pixelCard()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(currentName)
                    append("，")
                    append(strings.expSummaryTemplate.withCountAndName(safeExp, safeHelpfulCount.toString()))
                    next?.let {
                        append("，")
                        append(strings.nextTierTemplate.withCountAndName(it.threshold - safeExp, it.displayName(strings)))
                    }
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.14f), CircleShape)
                    .border(1.dp, color.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tier.icon(),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(21.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(currentName, style = pixelTextStyle(15.sp, FontWeight.Bold, color))
                Text(
                    strings.expSummaryTemplate.withCountAndName(safeExp, safeHelpfulCount.toString()),
                    style = pixelTextStyle(12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }

        if (next == null) {
            Text(
                strings.highestTier,
                style = pixelTextStyle(11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        } else {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = next.accessibleColor(),
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f),
            )
            Text(
                strings.nextTierTemplate.withCountAndName(
                    (next.threshold - safeExp).coerceAtLeast(0),
                    next.displayName(strings),
                ),
                style = pixelTextStyle(11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

/** 沒有自訂圖片時的可存取頭像：以暱稱第一個 Unicode code point 當作 initials。 */
@Composable
fun TreeHoleInitialsAvatar(
    nickname: String,
    modifier: Modifier = Modifier,
    accentColor: Color = PixelColors.green,
    size: Dp = 40.dp,
    contentDescription: String? = null,
) {
    val normalizedName = nickname.trim()
    val initial = remember(normalizedName) { normalizedName.firstCodePointOrQuestionMark() }
    val resolvedDescription = contentDescription
        ?: if (normalizedName.isEmpty()) "匿名使用者頭像" else "$normalizedName 的頭像"

    Box(
        modifier = modifier
            .size(size)
            .background(accentColor.copy(alpha = 0.18f), CircleShape)
            .border(2.dp, accentColor.copy(alpha = 0.72f), CircleShape)
            .semantics { this.contentDescription = resolvedDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = pixelTextStyle(size.value.times(0.38f).sp, FontWeight.Bold, accentColor),
            maxLines = 1,
        )
    }
}

@Composable
private fun PixelIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = MIN_TOUCH_TARGET, minHeight = MIN_TOUCH_TARGET)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.outline, RectangleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PixelDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
    )
}

private fun TreeHoleAnswerBadgeTierUi.displayName(strings: TreeHoleAnswerBadgeStrings): String = when (this) {
    TreeHoleAnswerBadgeTierUi.Rookie -> strings.rookie
    TreeHoleAnswerBadgeTierUi.Passerby -> strings.passerby
    TreeHoleAnswerBadgeTierUi.Senior -> strings.senior
    TreeHoleAnswerBadgeTierUi.Strategist -> strings.strategist
    TreeHoleAnswerBadgeTierUi.Master -> strings.master
}

private fun TreeHoleAnswerBadgeTierUi.nextTier(): TreeHoleAnswerBadgeTierUi? = when (this) {
    TreeHoleAnswerBadgeTierUi.Rookie -> TreeHoleAnswerBadgeTierUi.Passerby
    TreeHoleAnswerBadgeTierUi.Passerby -> TreeHoleAnswerBadgeTierUi.Senior
    TreeHoleAnswerBadgeTierUi.Senior -> TreeHoleAnswerBadgeTierUi.Strategist
    TreeHoleAnswerBadgeTierUi.Strategist -> TreeHoleAnswerBadgeTierUi.Master
    TreeHoleAnswerBadgeTierUi.Master -> null
}

private fun TreeHoleAnswerBadgeTierUi.icon(): ImageVector = when (this) {
    TreeHoleAnswerBadgeTierUi.Rookie -> Icons.Filled.Spa
    TreeHoleAnswerBadgeTierUi.Passerby -> Icons.Filled.Person
    TreeHoleAnswerBadgeTierUi.Senior -> Icons.Filled.Star
    TreeHoleAnswerBadgeTierUi.Strategist -> Icons.Filled.Lightbulb
    TreeHoleAnswerBadgeTierUi.Master -> Icons.Filled.EmojiEvents
}

/** 深色版 tier 顏色，在淺色卡片上仍有足夠的文字對比。 */
private fun TreeHoleAnswerBadgeTierUi.accessibleColor(): Color = when (this) {
    TreeHoleAnswerBadgeTierUi.Rookie -> Color(0xFF455A64)
    TreeHoleAnswerBadgeTierUi.Passerby -> Color(0xFF2E7D32)
    TreeHoleAnswerBadgeTierUi.Senior -> Color(0xFF0D5BA7)
    TreeHoleAnswerBadgeTierUi.Strategist -> Color(0xFF6A1B9A)
    TreeHoleAnswerBadgeTierUi.Master -> Color(0xFF7A5000)
}

private fun String.withCount(count: Int): String = replace("%d", count.toString())

private fun String.withCountAndName(count: Int, name: String): String =
    replace("%d", count.toString()).replace("%s", name)

private fun String.firstCodePointOrQuestionMark(): String {
    if (isEmpty()) return "？"
    val end = offsetByCodePoints(0, 1)
    return substring(0, end).uppercase(Locale.getDefault())
}

private val QUESTION_ACCENT = Color(0xFF6BB8FA)
private val QUESTION_ACCENT_DARK = Color(0xFF0B5FA5)
private val MIN_TOUCH_TARGET = 48.dp
private const val MIN_QUESTION_LENGTH = 5
private const val MAX_QUESTION_LENGTH = 200
private const val SUBMIT_DEBOUNCE_MILLIS = 700L
