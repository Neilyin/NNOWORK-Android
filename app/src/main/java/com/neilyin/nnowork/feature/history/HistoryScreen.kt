package com.neilyin.nnowork.feature.history

import android.widget.Toast
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.NnoworkTextField
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HistoryUiState(
    val settings: AppSettings = AppSettings(),
    val records: List<StampRecordEntity> = emptyList(),
    val plan: ResignationPlan = ResignationPlan(),
) {
    val grouped: Map<Int, List<StampRecordEntity>>
        get() = records.sortedByDescending { it.date }.groupBy { it.roundNumber }

    val mostFrequentReason: String?
        get() = records.groupingBy { it.reason }.eachCount().maxByOrNull { it.value }?.key

    val hasMasterReceipt: Boolean
        get() = records.any { it.roundNumber >= 3 } &&
            (settings.currentRound > 3 || settings.roundCompletedDates.size >= 3 || (settings.currentRound >= 3 && settings.isCardLocked))
}

class HistoryViewModel(
    private val stampDao: StampDao = ServiceLocator.stampDao,
    private val settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
    private val planRepository: PlanRepository = ServiceLocator.planRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> =
        combine(settingsRepository.settings, stampDao.observeAll(), planRepository.plan) { settings, records, plan ->
            HistoryUiState(settings, records, plan)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun updateReason(record: StampRecordEntity, reason: String) {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { stampDao.update(record.copy(reason = trimmed)) }
    }

    fun delete(record: StampRecordEntity) {
        viewModelScope.launch { stampDao.delete(record) }
    }

    fun startNewCompany() {
        viewModelScope.launch {
            stampDao.deleteAll()
            planRepository.reset()
            settingsRepository.update { current ->
                current.copy(
                    companyName = "",
                    currentRound = 1,
                    isOnboardingFinished = false,
                    firstStampDate = null,
                    isCardLocked = false,
                    cardCompletedDate = null,
                    roundStartDates = emptyList(),
                    roundCompletedDates = emptyList(),
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(
    receiptRequest: Int = 0,
    viewModel: HistoryViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        receiptRequest = receiptRequest,
        onUpdateReason = viewModel::updateReason,
        onDelete = viewModel::delete,
        onStartNewCompany = viewModel::startNewCompany,
    )
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    receiptRequest: Int = 0,
    onUpdateReason: (StampRecordEntity, String) -> Unit,
    onDelete: (StampRecordEntity) -> Unit,
    onStartNewCompany: () -> Unit,
) {
    var editing by remember { mutableStateOf<StampRecordEntity?>(null) }
    var showReceipt by remember { mutableStateOf(false) }
    var showFreedom by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()

    LaunchedEffect(receiptRequest, state.records.size) {
        if (receiptRequest > 0 && state.records.isNotEmpty()) {
            showReceipt = true
        }
    }

    if (editing != null) {
        EditRecordDialog(
            record = editing!!,
            canDelete = canDelete(editing!!, state.settings),
            onDismiss = { editing = null },
            onSave = {
                onUpdateReason(editing!!, it)
                editing = null
            },
            onDelete = {
                onDelete(editing!!)
                editing = null
            },
        )
    }

    if (showReceipt) {
        ReceiptDialog(
            state = state,
            onDismiss = { showReceipt = false },
            onShare = {
                shareScope.launch {
                    runCatching { ReceiptSharer.share(context, state.receiptShareContent()) }
                        .onFailure { Toast.makeText(context, "收據圖片產生失敗，請稍後再試。", Toast.LENGTH_SHORT).show() }
                }
            },
            onFreedom = {
                showReceipt = false
                showFreedom = true
            },
        )
    }

    if (showFreedom) {
        FreedomScreen(
            companyName = state.settings.companyName,
            completedCards = state.grouped.size,
            totalStamps = state.records.size,
            onViewHistory = { showFreedom = false },
            onStartNewCompany = {
                showFreedom = false
                onStartNewCompany()
            },
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = NnoworkSpacing.page),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
    ) {
        item {
            Text(
                "離職紀錄",
                style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.padding(top = NnoworkSpacing.md),
            )
        }
        if (state.records.isEmpty()) {
            item { EmptyHistory() }
        } else {
            item { ReceiptSummaryCard(state = state, onOpen = { showReceipt = true }) }
            item { Stats(state) }
            state.grouped.keys.sortedDescending().forEach { round ->
                item { RoundHeader(round = round, count = state.grouped[round].orEmpty().size) }
                items(state.grouped[round].orEmpty(), key = { it.id }) { record ->
                    RecordRow(record = record, onEdit = { editing = record })
                }
            }
        }
        item { Spacer(Modifier.height(NnoworkSpacing.sm)) }
    }
}

@Composable
private fun ReceiptSummaryCard(state: HistoryUiState, onOpen: () -> Unit) {
    val title = if (state.hasMasterReceipt) "🔥 是個狠人 🔥" else "離職收據"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(
                backgroundColor = if (state.hasMasterReceipt) NnoworkTheme.colors.special
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .padding(NnoworkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (state.hasMasterReceipt) Icons.Filled.WorkspacePremium else Icons.AutoMirrored.Filled.ReceiptLong,
            null,
            tint = if (state.hasMasterReceipt) NnoworkTheme.colors.highlight else NnoworkTheme.colors.info,
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.size(NnoworkSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, if (state.hasMasterReceipt) NnoworkTheme.colors.textOnDark else NnoworkTheme.colors.primaryText))
            Text("把這段想離職的日子印成一張收據", style = pixelTextStyle(NnoworkTheme.type.small, color = if (state.hasMasterReceipt) NnoworkTheme.colors.textOnDark.copy(alpha = 0.75f) else NnoworkTheme.colors.secondaryText))
        }
        PixelButton(onClick = onOpen, backgroundColor = NnoworkTheme.colors.highlight, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.sm, vertical = 9.dp)) {
            Text("查看", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
    }
}

@Composable
private fun ReceiptDialog(
    state: HistoryUiState,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onFreedom: () -> Unit,
) {
    val firstStampDate = state.records.minByOrNull { it.date }?.date ?: System.currentTimeMillis()
    val completedDate = state.settings.cardCompletedDate
        ?: state.settings.roundCompletedDates.lastOrNull()
        ?: state.records.maxByOrNull { it.date }?.date
        ?: System.currentTimeMillis()
    val totalDays = daysBetween(firstStampDate, completedDate)
    val company = state.settings.companyName.ifBlank { "公司" }
    val master = state.hasMasterReceipt

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (master) "🔥 是個狠人 🔥" else "離職收據", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, if (master) NnoworkTheme.colors.highlight else NnoworkTheme.colors.textOnDark), modifier = Modifier.weight(1f))
                PixelButton(onClick = onDismiss, backgroundColor = NnoworkTheme.colors.disabledBG, contentPadding = androidx.compose.foundation.layout.PaddingValues(7.dp)) {
                    Icon(Icons.Filled.Close, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelCard(backgroundColor = NnoworkTheme.colors.elevatedSurface)
                    .padding(NnoworkSpacing.roomy),
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.cozy),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(company, style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.secondaryText), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (master) "模範員工忍耐證明" else "這些日子辛苦了", style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, if (master) NnoworkTheme.colors.special else NnoworkTheme.colors.info))
                }
                DashedDivider()
                ReceiptRow(Icons.Filled.CalendarMonth, "開始日期", dateFormat.format(Date(firstStampDate)))
                ReceiptRow(Icons.Filled.CalendarMonth, "完成日期", dateFormat.format(Date(completedDate)))
                DashedDivider()
                ReceiptRow(Icons.Filled.CalendarMonth, if (master) "忍了多久" else "忍耐天數", "$totalDays 天")
                ReceiptRow(Icons.Filled.CheckCircle, if (master) "蓋了多少章" else "蓋章次數", "${state.records.size} 章")
                ReceiptRow(Icons.Filled.Savings, if (master) "存了多少錢" else "自由預備金", currency(state.plan.currentSaved), highlight = true)
                DashedDivider()
                Text(
                    if (master) "$company 的模範員工，仍然堅持下去。"
                    else "祝福你的新旅程，也祝你每一次想走都更靠近自由。",
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, if (master) NnoworkTheme.colors.special else NnoworkTheme.colors.info),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "離職集點卡 • ${fullDateFormat.format(Date())}",
                    style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                PixelButton(onClick = onShare, backgroundColor = NnoworkTheme.colors.highlight, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.sm, vertical = 11.dp)) {
                    Icon(Icons.Filled.Share, "分享收據", tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("分享", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
                PixelButton(onClick = onFreedom, backgroundColor = if (master) NnoworkTheme.colors.special else NnoworkTheme.colors.buttonBG, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.sm, vertical = 11.dp)) {
                    Text("奔向自由", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("收起", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.textOnDark))
            }
        },
    )
}

private fun HistoryUiState.receiptShareContent(): ReceiptShareContent {
    val first = records.minByOrNull { it.date }?.date ?: System.currentTimeMillis()
    val completed = settings.cardCompletedDate
        ?: settings.roundCompletedDates.lastOrNull()
        ?: records.maxByOrNull { it.date }?.date
        ?: System.currentTimeMillis()
    return ReceiptShareContent(
        companyName = settings.companyName.ifBlank { "公司" },
        master = hasMasterReceipt,
        firstStampDate = first,
        completedDate = completed,
        totalDays = daysBetween(first, completed),
        totalStamps = records.size,
        savedAmount = plan.currentSaved,
    )
}

@Composable
private fun ReceiptRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (highlight) NnoworkTheme.colors.success else NnoworkTheme.colors.secondaryText, modifier = Modifier.size(21.dp))
        Spacer(Modifier.size(NnoworkSpacing.xs))
        Text(label, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.primaryText), modifier = Modifier.weight(1f))
        Text(value, style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, if (highlight) NnoworkTheme.colors.success else NnoworkTheme.colors.primaryText))
    }
}

@Composable
private fun DashedDivider() {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(16) {
            Spacer(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(NnoworkTheme.colors.disabledBG.copy(alpha = 0.55f)),
            )
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
    ) {
        Icon(Icons.Filled.Book, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(72.dp))
        Text("還沒有離職紀錄", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, MaterialTheme.colorScheme.onBackground))
        Text("開始蓋章，記錄你的離職心路歷程", style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.secondaryText))
    }
}

@Composable
private fun Stats(state: HistoryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
            StatCard("總蓋章數", state.records.size.toString(), Icons.Filled.CheckCircle, NnoworkTheme.colors.info, Modifier.weight(1f))
            StatCard("使用卡片", state.grouped.size.toString(), Icons.Filled.ViewModule, NnoworkTheme.colors.warning, Modifier.weight(1f))
        }
        state.mostFrequentReason?.let { reason ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelCard(backgroundColor = NnoworkTheme.colors.danger)
                    .padding(NnoworkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = NnoworkTheme.colors.textOnDark)
                Spacer(Modifier.size(NnoworkSpacing.snug))
                Column {
                    Text("最常見的原因", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.textOnDark.copy(alpha = 0.75f)))
                    Text(reason, style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.textOnDark), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .pixelCard()
            .padding(NnoworkSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Text(value, style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
    }
}

@Composable
private fun RoundHeader(round: Int, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = MaterialTheme.colorScheme.surface, borderColor = roundColor(round))
            .padding(NnoworkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "第 $round 張卡",
            style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, MaterialTheme.colorScheme.onSurface),
        )
        Spacer(Modifier.weight(1f))
        Text(roundSubtitle(round), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, roundColor(round)))
        Spacer(Modifier.size(NnoworkSpacing.sm))
        Text(count.toString(), style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText), modifier = Modifier.background(roundColor(round)).padding(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.xxs))
    }
}

@Composable
private fun RecordRow(record: StampRecordEntity, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(NnoworkSpacing.cozy),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(0.35f)) {
            Text(dateFormat.format(Date(record.date)), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            Text(timeFormat.format(Date(record.date)), style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
        }
        Spacer(
            Modifier
                .height(NnoworkSize.minTouchTarget)
                .size(width = 2.dp, height = NnoworkSize.minTouchTarget)
                .background(NnoworkTheme.colors.primaryText),
        )
        Spacer(Modifier.size(NnoworkSpacing.sm))
        Text(record.reason, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.primaryText), modifier = Modifier.weight(1f), maxLines = 3)
        PixelButton(onClick = onEdit, backgroundColor = NnoworkTheme.colors.disabledBG, contentPadding = androidx.compose.foundation.layout.PaddingValues(NnoworkSpacing.xs)) {
            Icon(Icons.Filled.Edit, null, tint = NnoworkTheme.colors.primaryText)
        }
    }
}

@Composable
private fun EditRecordDialog(
    record: StampRecordEntity,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var reason by remember(record.id) { mutableStateOf(record.reason) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("編輯記錄", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.textOnDark)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
                Text("${dateFormat.format(Date(record.date))} ${timeFormat.format(Date(record.date))}", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
                NnoworkTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "離職理由",
                    placeholder = "寫下這次想離職的原因…",
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                )
            }
        },
        confirmButton = {
            PixelButton(onClick = { onSave(reason) }, backgroundColor = NnoworkTheme.colors.success) {
                Icon(Icons.Filled.Save, null, tint = NnoworkTheme.colors.primaryText)
                Spacer(Modifier.size(NnoworkSpacing.tight))
                Text("儲存", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                if (canDelete) {
                    PixelButton(onClick = onDelete, backgroundColor = NnoworkTheme.colors.danger) {
                        Icon(Icons.Filled.Delete, null, tint = NnoworkTheme.colors.textOnDark)
                    }
                }
                PixelButton(onClick = onDismiss, backgroundColor = NnoworkTheme.colors.disabledBG) {
                    Text("取消", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            }
        },
    )
}

private fun canDelete(record: StampRecordEntity, settings: AppSettings): Boolean =
    record.roundNumber == settings.currentRound && !settings.isCardLocked

private fun daysBetween(startMillis: Long, endMillis: Long): Int {
    val start = Calendar.getInstance().apply {
        timeInMillis = startMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = Calendar.getInstance().apply {
        timeInMillis = endMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diff = end.timeInMillis - start.timeInMillis
    return (diff / 86_400_000L).coerceAtLeast(0).toInt()
}

private fun currency(value: Double): String =
    "$" + NumberFormat.getNumberInstance(Locale.TAIWAN).format(value.toLong())

private fun roundSubtitle(round: Int): String = when (round) {
    1 -> "初次萌生退意"
    2 -> "再給機會"
    3 -> "忍無可忍"
    else -> "忍耐仙人"
}

@Composable
private fun roundColor(round: Int) = when (round) {
    1 -> NnoworkTheme.colors.info
    2 -> NnoworkTheme.colors.warning
    3 -> NnoworkTheme.colors.special
    else -> NnoworkTheme.colors.secondaryText
}

private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
private val timeFormat = SimpleDateFormat("HH:mm", Locale.TAIWAN)
private val fullDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.TAIWAN)

@Preview(name = "離職紀錄", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HistoryContentPreview() {
    val now = 1_782_270_000_000L
    HistoryContent(
        state = HistoryUiState(
            settings = AppSettings(currentRound = 2, isCardLocked = false),
            records = listOf(
                StampRecordEntity(id = "1", date = now, reason = "無效會議又開了兩小時", roundNumber = 2),
                StampRecordEntity(id = "2", date = now - 86_400_000L, reason = "需求又改第三版", roundNumber = 2),
                StampRecordEntity(id = "3", date = now - 172_800_000L, reason = "加班沒有加班費", roundNumber = 1),
                StampRecordEntity(id = "4", date = now - 259_200_000L, reason = "老闆突然說明天要 demo", roundNumber = 1),
            ),
        ),
        onUpdateReason = { _, _ -> },
        onDelete = {},
        onStartNewCompany = {},
    )
}
