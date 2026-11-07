package com.neilyin.nnowork.feature.history

import android.widget.Toast
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
import androidx.compose.ui.unit.sp
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
import com.neilyin.nnowork.ui.theme.PixelColors
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
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "離職紀錄",
                style = pixelTextStyle(30.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.padding(top = 16.dp),
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
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ReceiptSummaryCard(state: HistoryUiState, onOpen: () -> Unit) {
    val title = if (state.hasMasterReceipt) "🔥 是個狠人 🔥" else "離職收據"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(
                backgroundColor = if (state.hasMasterReceipt) PixelColors.purple
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (state.hasMasterReceipt) Icons.Filled.WorkspacePremium else Icons.AutoMirrored.Filled.ReceiptLong,
            null,
            tint = if (state.hasMasterReceipt) PixelColors.yellow else PixelColors.blue,
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = pixelTextStyle(19.sp, FontWeight.Bold, if (state.hasMasterReceipt) PixelColors.white else PixelColors.black))
            Text("把這段想離職的日子印成一張收據", style = pixelTextStyle(12.sp, color = if (state.hasMasterReceipt) PixelColors.white.copy(alpha = 0.75f) else PixelColors.gray))
        }
        PixelButton(onClick = onOpen, backgroundColor = PixelColors.yellow, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 9.dp)) {
            Text("查看", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
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
                Text(if (master) "🔥 是個狠人 🔥" else "離職收據", style = pixelTextStyle(21.sp, FontWeight.Bold, if (master) PixelColors.yellow else PixelColors.white), modifier = Modifier.weight(1f))
                PixelButton(onClick = onDismiss, backgroundColor = PixelColors.gray, contentPadding = androidx.compose.foundation.layout.PaddingValues(7.dp)) {
                    Icon(Icons.Filled.Close, null, tint = PixelColors.black, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelCard(backgroundColor = PixelColors.white)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(company, style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.gray), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (master) "模範員工忍耐證明" else "這些日子辛苦了", style = pixelTextStyle(18.sp, FontWeight.Bold, if (master) PixelColors.purple else PixelColors.blue))
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
                    style = pixelTextStyle(15.sp, FontWeight.Bold, if (master) PixelColors.purple else PixelColors.blue),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "離職集點卡 • ${fullDateFormat.format(Date())}",
                    style = pixelTextStyle(11.sp, color = PixelColors.gray),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(onClick = onShare, backgroundColor = PixelColors.yellow, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 11.dp)) {
                    Icon(Icons.Filled.Share, "分享收據", tint = PixelColors.black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("分享", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
                }
                PixelButton(onClick = onFreedom, backgroundColor = if (master) PixelColors.purple else PixelColors.blue, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 11.dp)) {
                    Text("奔向自由", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.white))
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("收起", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.white))
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
        Icon(icon, null, tint = if (highlight) PixelColors.green else PixelColors.gray, modifier = Modifier.size(21.dp))
        Spacer(Modifier.size(8.dp))
        Text(label, style = pixelTextStyle(14.sp, FontWeight.Medium, PixelColors.black), modifier = Modifier.weight(1f))
        Text(value, style = pixelTextStyle(16.sp, FontWeight.Bold, if (highlight) PixelColors.green else PixelColors.black))
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
                    .background(PixelColors.gray.copy(alpha = 0.55f)),
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Book, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(72.dp))
        Text("還沒有離職紀錄", style = pixelTextStyle(22.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground))
        Text("開始蓋章，記錄你的離職心路歷程", style = pixelTextStyle(14.sp, color = PixelColors.gray))
    }
}

@Composable
private fun Stats(state: HistoryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("總蓋章數", state.records.size.toString(), Icons.Filled.CheckCircle, PixelColors.blue, Modifier.weight(1f))
            StatCard("使用卡片", state.grouped.size.toString(), Icons.Filled.ViewModule, PixelColors.orange, Modifier.weight(1f))
        }
        state.mostFrequentReason?.let { reason ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pixelCard(backgroundColor = PixelColors.red)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = PixelColors.white)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text("最常見的原因", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.white.copy(alpha = 0.75f)))
                    Text(reason, style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.white), maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Text(value, style = pixelTextStyle(28.sp, FontWeight.Bold, PixelColors.black))
        Text(label, style = pixelTextStyle(13.sp, color = PixelColors.gray))
    }
}

@Composable
private fun RoundHeader(round: Int, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = MaterialTheme.colorScheme.surface, borderColor = roundColor(round))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "第 $round 張卡",
            style = pixelTextStyle(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurface),
        )
        Spacer(Modifier.weight(1f))
        Text(roundSubtitle(round), style = pixelTextStyle(13.sp, FontWeight.Bold, roundColor(round)))
        Spacer(Modifier.size(12.dp))
        Text(count.toString(), style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.black), modifier = Modifier.background(roundColor(round)).padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun RecordRow(record: StampRecordEntity, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(0.35f)) {
            Text(dateFormat.format(Date(record.date)), style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
            Text(timeFormat.format(Date(record.date)), style = pixelTextStyle(12.sp, color = PixelColors.gray))
        }
        Spacer(
            Modifier
                .height(48.dp)
                .size(width = 2.dp, height = 48.dp)
                .background(PixelColors.black),
        )
        Spacer(Modifier.size(12.dp))
        Text(record.reason, style = pixelTextStyle(15.sp, FontWeight.Medium, PixelColors.black), modifier = Modifier.weight(1f), maxLines = 3)
        PixelButton(onClick = onEdit, backgroundColor = PixelColors.gray, contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
            Icon(Icons.Filled.Edit, null, tint = PixelColors.black)
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
        title = { Text("編輯記錄", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${dateFormat.format(Date(record.date))} ${timeFormat.format(Date(record.date))}", style = pixelTextStyle(13.sp, color = PixelColors.gray))
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
            PixelButton(onClick = { onSave(reason) }, backgroundColor = PixelColors.green) {
                Icon(Icons.Filled.Save, null, tint = PixelColors.black)
                Spacer(Modifier.size(6.dp))
                Text("儲存", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canDelete) {
                    PixelButton(onClick = onDelete, backgroundColor = PixelColors.red) {
                        Icon(Icons.Filled.Delete, null, tint = PixelColors.white)
                    }
                }
                PixelButton(onClick = onDismiss, backgroundColor = PixelColors.gray) {
                    Text("取消", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
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

private fun roundColor(round: Int) = when (round) {
    1 -> PixelColors.blue
    2 -> PixelColors.orange
    3 -> PixelColors.purple
    else -> PixelColors.gray
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
