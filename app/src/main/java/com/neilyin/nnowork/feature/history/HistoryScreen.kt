package com.neilyin.nnowork.feature.history

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryUiState(
    val settings: AppSettings = AppSettings(),
    val records: List<StampRecordEntity> = emptyList(),
) {
    val grouped: Map<Int, List<StampRecordEntity>>
        get() = records.sortedByDescending { it.date }.groupBy { it.roundNumber }

    val mostFrequentReason: String?
        get() = records.groupingBy { it.reason }.eachCount().maxByOrNull { it.value }?.key
}

class HistoryViewModel(
    private val stampDao: StampDao = ServiceLocator.stampDao,
    settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> =
        combine(settingsRepository.settings, stampDao.observeAll()) { settings, records ->
            HistoryUiState(settings, records)
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
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        state = state,
        onUpdateReason = viewModel::updateReason,
        onDelete = viewModel::delete,
    )
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    onUpdateReason: (StampRecordEntity, String) -> Unit,
    onDelete: (StampRecordEntity) -> Unit,
) {
    var editing by remember { mutableStateOf<StampRecordEntity?>(null) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelColors.darkGray)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "離職紀錄",
                style = pixelTextStyle(30.sp, FontWeight.Bold, PixelColors.white),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        if (state.records.isEmpty()) {
            item { EmptyHistory() }
        } else {
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
private fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Book, null, tint = PixelColors.white, modifier = Modifier.size(72.dp))
        Text("還沒有離職紀錄", style = pixelTextStyle(22.sp, FontWeight.Bold, PixelColors.white))
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
            .pixelCard(backgroundColor = PixelColors.white.copy(alpha = 0.12f), borderColor = roundColor(round))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("第 $round 張卡", style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.white))
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
        containerColor = PixelColors.darkGray,
        title = { Text("編輯記錄", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${dateFormat.format(Date(record.date))} ${timeFormat.format(Date(record.date))}", style = pixelTextStyle(13.sp, color = PixelColors.gray))
                BasicTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    textStyle = pixelTextStyle(16.sp, FontWeight.Medium, PixelColors.black),
                    cursorBrush = SolidColor(PixelColors.black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(PixelColors.white)
                        .border(3.dp, PixelColors.black, RectangleShape)
                        .padding(12.dp),
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
