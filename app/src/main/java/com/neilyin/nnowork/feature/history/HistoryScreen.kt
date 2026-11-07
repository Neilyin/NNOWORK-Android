package com.neilyin.nnowork.feature.history

import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.PixelBadge
import com.neilyin.nnowork.ui.theme.contentColorFor
import com.neilyin.nnowork.ui.theme.NnoworkPageTitle
import com.neilyin.nnowork.ui.theme.NnoworkDialogDefaults
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.TextButton
import androidx.compose.runtime.key
import androidx.compose.foundation.shape.RoundedCornerShape
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.neilyin.nnowork.ui.adaptive.LocalNnoworkWindowInfo
import com.neilyin.nnowork.ui.adaptive.NnoworkTwoPane
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
import com.neilyin.nnowork.data.auth.AuthRepository
import com.neilyin.nnowork.data.jobarchive.JobArchiveEntity
import com.neilyin.nnowork.data.jobarchive.JobArchiveRepository
import com.neilyin.nnowork.feature.jobarchive.JobArchiveDetailScreen
import com.neilyin.nnowork.feature.jobarchive.JobTransitionSheet
import com.neilyin.nnowork.feature.jobarchive.PastJobsSection
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    /** 目前登入帳號的過往工作（未登入時為空，整區不顯示）。 */
    val archives: List<JobArchiveEntity> = emptyList(),
) {
    val grouped: Map<Int, List<StampRecordEntity>>
        get() = records.sortedByDescending { it.date }.groupBy { it.roundNumber }

    val mostFrequentReason: String?
        get() = records.groupingBy { it.reason }.eachCount().maxByOrNull { it.value }?.key

    val hasMasterReceipt: Boolean
        get() = records.any { it.roundNumber >= 3 } &&
            (settings.currentRound > 3 || settings.roundCompletedDates.size >= 3 || (settings.currentRound >= 3 && settings.isCardLocked))
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val stampDao: StampDao = ServiceLocator.stampDao,
    private val settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
    private val planRepository: PlanRepository = ServiceLocator.planRepository,
    private val jobArchiveRepository: JobArchiveRepository = ServiceLocator.jobArchiveRepository,
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
) : ViewModel() {
    /** 只顯示目前帳號的封存；登入／登出／換帳號時跟著切換。 */
    private val archives = authRepository.state
        .map { authRepository.currentUserId }
        .distinctUntilChanged()
        .flatMapLatest { owner -> jobArchiveRepository.visibleArchives(owner) }

    val uiState: StateFlow<HistoryUiState> =
        combine(settingsRepository.settings, stampDao.observeAll(), planRepository.plan, archives) { settings, records, plan, archives ->
            HistoryUiState(settings, records, plan, archives)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun updateReason(record: StampRecordEntity, reason: String) {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { stampDao.update(record.copy(reason = trimmed)) }
    }

    fun delete(record: StampRecordEntity) {
        viewModelScope.launch { stampDao.delete(record) }
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
    )
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    receiptRequest: Int = 0,
    onUpdateReason: (StampRecordEntity, String) -> Unit,
    onDelete: (StampRecordEntity) -> Unit,
) {
    val twoPane = LocalNnoworkWindowInfo.current.usesListDetail
    // 選中的紀錄／過往工作、編輯中的文字、對話框開關都用 rememberSaveable（只存 id 與字串），
    // 摺疊／展開、旋轉後仍停在同一筆，雙欄 ↔ 單欄切換時也接得上（雙欄右欄 ↔ 單欄編輯對話框）。
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editDraft by rememberSaveable { mutableStateOf("") }
    val editing = editingId?.let { id -> state.records.firstOrNull { it.id == id } }
    var showReceipt by rememberSaveable { mutableStateOf(false) }
    var showFreedom by rememberSaveable { mutableStateOf(false) }
    var showJobTransition by rememberSaveable { mutableStateOf(false) }
    var selectedArchiveId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedArchive = selectedArchiveId?.let { id -> state.archives.firstOrNull { it.id == id } }
    // 清單捲動位置放在不隨版面變動的位置，單欄／雙欄共用同一個 state。
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()

    val selectRecord: (StampRecordEntity) -> Unit = { record ->
        selectedArchiveId = null
        editingId = record.id
        editDraft = record.reason
    }
    val closeEditor: () -> Unit = { editingId = null }
    val selectArchive: (String) -> Unit = { id ->
        // 右欄一次只顯示一種（iOS：selectedArchive 與 editingRecord 互斥）
        editingId = null
        selectedArchiveId = id
    }

    LaunchedEffect(receiptRequest, state.records.size) {
        if (receiptRequest > 0 && state.records.isNotEmpty()) {
            showReceipt = true
        }
    }

    if (editing != null && !twoPane) {
        EditRecordDialog(
            record = editing,
            reason = editDraft,
            onReasonChange = { editDraft = it },
            canDelete = canDelete(editing, state.settings),
            onDismiss = closeEditor,
            onSave = {
                onUpdateReason(editing, it)
                closeEditor()
            },
            onDelete = {
                onDelete(editing)
                closeEditor()
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

    // 自由畫面「重新集點（新公司）」走換工作流程（封存或明確確認不保存），不再直接清資料。
    if (showJobTransition) {
        JobTransitionSheet(
            onDismiss = { showJobTransition = false },
            onCompleted = {
                showJobTransition = false
                showFreedom = false
            },
        )
    }

    if (showFreedom) {
        FreedomScreen(
            companyName = state.settings.companyName,
            completedCards = state.grouped.size,
            totalStamps = state.records.size,
            onViewHistory = { showFreedom = false },
            onStartNewCompany = { showJobTransition = true },
        )
        return
    }

    if (!twoPane) {
        if (selectedArchive != null) {
            JobArchiveDetailScreen(archive = selectedArchive, onBack = { selectedArchiveId = null })
            return
        }
        HistoryList(
            state = state,
            listState = listState,
            selectedRecordId = null,
            onSelectRecord = null,
            onEdit = selectRecord,
            onSelectArchive = selectArchive,
            onOpenReceipt = { showReceipt = true },
        )
        return
    }

    // 寬螢幕（iOS HistoryView wideBody）：左紀錄列表、右選中紀錄／過往工作。
    NnoworkTwoPane(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        first = {
            HistoryList(
                state = state,
                listState = listState,
                selectedRecordId = editing?.id,
                onSelectRecord = selectRecord,
                onEdit = selectRecord,
                onSelectArchive = selectArchive,
                onOpenReceipt = { showReceipt = true },
            )
        },
        second = {
            when {
                selectedArchive != null -> {
                    // 過往工作詳情直接顯示在右欄；返回鈕＝關閉右欄（iOS「關閉」）。
                    key(selectedArchive.id) {
                        JobArchiveDetailScreen(archive = selectedArchive, onBack = { selectedArchiveId = null })
                    }
                }
                editing != null -> key(editing.id) {
                    EditRecordPane(
                        record = editing,
                        reason = editDraft,
                        onReasonChange = { editDraft = it },
                        canDelete = canDelete(editing, state.settings),
                        onCancel = closeEditor,
                        onSave = {
                            onUpdateReason(editing, it)
                            closeEditor()
                        },
                        onDelete = {
                            onDelete(editing)
                            closeEditor()
                        },
                    )
                }
                else -> HistoryDetailPlaceholder(
                    nothingAtAll = state.records.isEmpty() && state.archives.isEmpty(),
                )
            }
        },
    )
}

/** 寬螢幕右欄的編輯表單（iOS editRecordForm，單欄時是 [EditRecordDialog]）。 */
@Composable
private fun EditRecordPane(
    record: StampRecordEntity,
    reason: String,
    onReasonChange: (String) -> Unit,
    canDelete: Boolean,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NnoworkSpacing.page, vertical = NnoworkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "編輯記錄",
                style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCancel) {
                Text("取消", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.accentForeground))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pixelCard()
                .padding(NnoworkSpacing.md),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
        ) {
            Text(
                "${dateFormat.format(Date(record.date))} ${timeFormat.format(Date(record.date))}",
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText, monospace = true),
            )
            NnoworkTextField(
                value = reason,
                onValueChange = onReasonChange,
                label = "離職理由",
                placeholder = "寫下這次想離職的原因…",
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
            if (canDelete) {
                PixelButton(onClick = onDelete, backgroundColor = NnoworkTheme.colors.disabledBG) {
                    Icon(Icons.Filled.Delete, "刪除記錄", tint = NnoworkTheme.colors.danger)
                }
            }
            Spacer(Modifier.weight(1f))
            PixelButton(
                onClick = { onSave(reason) },
                enabled = reason.isNotBlank(),
                backgroundColor = if (reason.isNotBlank()) NnoworkTheme.colors.success else NnoworkTheme.colors.disabledBG,
            ) {
                Icon(Icons.Filled.Save, null, tint = NnoworkTheme.colors.primaryText)
                Spacer(Modifier.size(NnoworkSpacing.tight))
                Text("儲存", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        }
    }
}

/** 寬螢幕右欄尚未選取時的提示（iOS wideDetailColumn 的空狀態）。 */
@Composable
private fun HistoryDetailPlaceholder(nothingAtAll: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(NnoworkSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (nothingAtAll) Icons.Filled.Book else Icons.Filled.EditNote,
            contentDescription = null,
            tint = NnoworkTheme.colors.accentForeground,
            modifier = Modifier.size(NnoworkSize.icon * 2),
        )
        Spacer(Modifier.height(NnoworkSpacing.md))
        Text(
            if (nothingAtAll) "還沒有離職紀錄" else "點左邊的紀錄，在這裡查看與編輯",
            style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.secondaryText),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryList(
    state: HistoryUiState,
    listState: LazyListState,
    selectedRecordId: String?,
    /** 雙欄時整列可點選；單欄維持只用編輯鈕。 */
    onSelectRecord: ((StampRecordEntity) -> Unit)?,
    onEdit: (StampRecordEntity) -> Unit,
    onSelectArchive: (String) -> Unit,
    onOpenReceipt: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = NnoworkSpacing.page),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
    ) {
        item {
            NnoworkPageTitle("離職紀錄", modifier = Modifier.padding(top = NnoworkSpacing.md))
        }
        if (state.archives.isNotEmpty()) {
            item(key = "past-jobs") {
                PastJobsSection(archives = state.archives, onSelect = { onSelectArchive(it.id) })
            }
        }
        if (state.records.isEmpty()) {
            item { EmptyHistory() }
        } else {
            item { ReceiptSummaryCard(state = state, onOpen = onOpenReceipt) }
            item { Stats(state) }
            state.grouped.keys.sortedDescending().forEach { round ->
                item { RoundHeader(round = round, count = state.grouped[round].orEmpty().size) }
                items(state.grouped[round].orEmpty(), key = { it.id }) { record ->
                    RecordRow(
                        record = record,
                        selected = record.id == selectedRecordId,
                        onSelect = onSelectRecord?.let { select -> { select(record) } },
                        onEdit = { onEdit(record) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(NnoworkSpacing.sm)) }
    }
}

@Composable
private fun ReceiptSummaryCard(state: HistoryUiState, onOpen: () -> Unit) {
    val title = if (state.hasMasterReceipt) "🔥 是個狠人 🔥" else "離職收據"
    // 大師收據卡是紫色實心，字色依底色挑；一般收據卡用主題文字色。
    val cardText = if (state.hasMasterReceipt) contentColorFor(NnoworkTheme.colors.special) else NnoworkTheme.colors.primaryText
    val cardSubText = if (state.hasMasterReceipt) cardText.copy(alpha = 0.8f) else NnoworkTheme.colors.secondaryText
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
            tint = if (state.hasMasterReceipt) cardText else NnoworkTheme.colors.accentForeground,
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.size(NnoworkSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, cardText))
            Text("把這段想離職的日子印成一張收據", style = pixelTextStyle(NnoworkTheme.type.small, color = cardSubText))
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
        containerColor = NnoworkDialogDefaults.containerColor,
        shape = NnoworkDialogDefaults.shape,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (master) "🔥 是個狠人 🔥" else "離職收據", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, if (master) NnoworkTheme.colors.highlight else NnoworkTheme.colors.onElevated), modifier = Modifier.weight(1f))
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
                    Text("奔向自由", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("收起", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Medium, NnoworkTheme.colors.accentForeground))
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
        Icon(Icons.Filled.Book, null, tint = NnoworkTheme.colors.secondaryText, modifier = Modifier.size(72.dp))
        Text("還沒有離職紀錄", style = pixelTextStyle(NnoworkTheme.type.heading, FontWeight.Bold, NnoworkTheme.colors.primaryText))
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
                    // 方向 B：重點卡改用低彩度 accentSoft，不再用整塊飽和紅。
                    .pixelCard(backgroundColor = NnoworkTheme.colors.accentSoft, borderColor = NnoworkTheme.colors.accent)
                    .padding(NnoworkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = NnoworkTheme.colors.danger)
                Spacer(Modifier.size(NnoworkSpacing.snug))
                Column {
                    Text("最常見的原因", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Medium, NnoworkTheme.colors.secondaryText))
                    Text(reason, style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText), maxLines = 2, overflow = TextOverflow.Ellipsis)
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
        Text(value, style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true))
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
    }
}

@Composable
private fun RoundHeader(round: Int, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = NnoworkTheme.colors.surface, borderColor = NnoworkTheme.colors.accent)
            .padding(NnoworkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "第 $round 張卡",
            style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText),
        )
        Spacer(Modifier.weight(1f))
        Text(roundSubtitle(round), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.accentForeground))
        Spacer(Modifier.size(NnoworkSpacing.sm))
        PixelBadge(backgroundColor = NnoworkTheme.colors.accent) {
            Text(count.toString(), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onAccent, monospace = true))
        }
    }
}

@Composable
private fun RecordRow(
    record: StampRecordEntity,
    onEdit: () -> Unit,
    selected: Boolean = false,
    /** 雙欄時整列可點，選中的那列用 accentSoft 底＋accent 框標示。 */
    onSelect: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.pixelCard(
                        backgroundColor = NnoworkTheme.colors.accentSoft,
                        borderColor = NnoworkTheme.colors.accent,
                        borderWidth = NnoworkLine.control,
                        borderAlpha = 1f,
                    )
                } else {
                    Modifier.pixelCard()
                },
            )
            .then(if (onSelect != null) Modifier.clip(RoundedCornerShape(NnoworkRadius.card)).clickable(onClick = onSelect) else Modifier)
            .padding(NnoworkSpacing.cozy),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 日期欄依內容寬度排（不用固定比例），單行不換行；字級放大 1.3 倍時由右側原因欄讓位。
        Column {
            Text(
                dateFormat.format(Date(record.date)),
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true),
                maxLines = 1,
                softWrap = false,
            )
            Text(
                timeFormat.format(Date(record.date)),
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText, monospace = true),
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(
            Modifier
                .height(NnoworkSize.minTouchTarget)
                .size(width = NnoworkLine.hairline, height = NnoworkSize.minTouchTarget)
                .background(NnoworkTheme.colors.divider),
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
    reason: String,
    onReasonChange: (String) -> Unit,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NnoworkDialogDefaults.containerColor,
        shape = NnoworkDialogDefaults.shape,
        title = { Text("編輯記錄", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
                Text("${dateFormat.format(Date(record.date))} ${timeFormat.format(Date(record.date))}", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
                NnoworkTextField(
                    value = reason,
                    onValueChange = onReasonChange,
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
                        Icon(Icons.Filled.Delete, null, tint = NnoworkTheme.colors.onElevated)
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
    )
}
