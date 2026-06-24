package com.neilyin.nnowork.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
    private val planRepository: PlanRepository = ServiceLocator.planRepository,
    private val stampDao: StampDao = ServiceLocator.stampDao,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    fun updateCompany(name: String) = update { it.copy(companyName = name.trim()) }
    fun updateWidgetInterval(minutes: Int) = update { it.copy(widgetRefreshInterval = minutes) }
    fun selectTheme(theme: CardTheme) = update { it.copy(cardStyle = theme.id) }
    fun unlockTheme(theme: CardTheme) = update {
        if (it.coinCount < theme.coinCost || it.unlockedStyles.contains(theme.id)) {
            it
        } else {
            it.copy(
                coinCount = it.coinCount - theme.coinCost,
                unlockedStyles = it.unlockedStyles + theme.id,
                cardStyle = theme.id,
            )
        }
    }
    fun addReason(reason: String) {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) return
        update {
            if (it.commonReasons.contains(trimmed)) it else it.copy(commonReasons = it.commonReasons + trimmed)
        }
    }
    fun deleteReason(reason: String) = update { it.copy(commonReasons = it.commonReasons.filterNot { item -> item == reason }) }
    fun resetAll() {
        viewModelScope.launch {
            stampDao.deleteAll()
            planRepository.reset()
            settingsRepository.update { AppSettings() }
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsContent(
        settings = settings,
        onUpdateCompany = viewModel::updateCompany,
        onSelectTheme = viewModel::selectTheme,
        onUnlockTheme = viewModel::unlockTheme,
        onAddReason = viewModel::addReason,
        onDeleteReason = viewModel::deleteReason,
        onUpdateWidgetInterval = viewModel::updateWidgetInterval,
        onResetAll = viewModel::resetAll,
    )
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onUpdateCompany: (String) -> Unit,
    onSelectTheme: (CardTheme) -> Unit,
    onUnlockTheme: (CardTheme) -> Unit,
    onAddReason: (String) -> Unit,
    onDeleteReason: (String) -> Unit,
    onUpdateWidgetInterval: (Int) -> Unit,
    onResetAll: () -> Unit,
) {
    var company by remember(settings.companyName) { mutableStateOf(settings.companyName) }
    var newReason by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            containerColor = PixelColors.darkGray,
            title = { Text("確定要全部重來？", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
            text = { Text("這會刪除集點紀錄、離職計畫與設定。此操作無法復原。", style = pixelTextStyle(14.sp, color = PixelColors.gray)) },
            confirmButton = {
                PixelButton(
                    onClick = {
                        onResetAll()
                        showReset = false
                    },
                    backgroundColor = PixelColors.red,
                ) {
                    Text("確定重置", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white))
                }
            },
            dismissButton = {
                PixelButton(onClick = { showReset = false }, backgroundColor = PixelColors.gray) {
                    Text("取消", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixelColors.darkGray)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("設定", style = pixelTextStyle(30.sp, FontWeight.Bold, PixelColors.white))
        CoinSection(settings.coinCount)
        SettingsSection(icon = Icons.Filled.Business, title = "公司資訊") {
            LabeledTextField(
                label = "公司名稱",
                value = company,
                placeholder = "輸入公司名稱",
                onValueChange = { company = it },
            )
            Spacer(Modifier.height(10.dp))
            PixelButton(onClick = { onUpdateCompany(company) }, backgroundColor = PixelColors.blue, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Save, null, tint = PixelColors.white)
                Spacer(Modifier.size(8.dp))
                Text("儲存公司名稱", style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.white))
            }
        }
        SettingsSection(icon = Icons.Filled.Style, title = "集點卡設定") {
            Text("目標格數：${settings.maxStamps} 格，集滿後可在首頁建立下一張卡時更換。", style = pixelTextStyle(13.sp, color = PixelColors.gray))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CardTheme.entries.forEach { theme ->
                    ThemeButton(
                        theme = theme,
                        selected = settings.cardStyle == theme.id,
                        unlocked = settings.unlockedStyles.contains(theme.id),
                        coins = settings.coinCount,
                        onSelect = { onSelectTheme(theme) },
                        onUnlock = { onUnlockTheme(theme) },
                    )
                }
            }
        }
        SettingsSection(icon = Icons.Filled.Refresh, title = "常用蓋章原因") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = newReason,
                    onValueChange = { newReason = it },
                    singleLine = true,
                    textStyle = pixelTextStyle(15.sp, FontWeight.Medium, PixelColors.black),
                    cursorBrush = SolidColor(PixelColors.black),
                    modifier = Modifier
                        .weight(1f)
                        .background(PixelColors.white)
                        .border(3.dp, PixelColors.black, RectangleShape)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (newReason.isEmpty()) Text("輸入新原因...", style = pixelTextStyle(14.sp, color = PixelColors.gray))
                        inner()
                    },
                )
                Spacer(Modifier.size(8.dp))
                PixelButton(
                    onClick = {
                        onAddReason(newReason)
                        newReason = ""
                    },
                    enabled = newReason.isNotBlank(),
                    backgroundColor = if (newReason.isBlank()) PixelColors.gray else PixelColors.green,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Icon(Icons.Filled.Add, null, tint = PixelColors.black)
                }
            }
            Spacer(Modifier.height(10.dp))
            settings.commonReasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PixelColors.white)
                        .border(2.dp, PixelColors.black, RectangleShape)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(reason, style = pixelTextStyle(15.sp, FontWeight.Medium, PixelColors.black), modifier = Modifier.weight(1f))
                    PixelButton(onClick = { onDeleteReason(reason) }, backgroundColor = PixelColors.red, contentPadding = PaddingValues(7.dp)) {
                        Icon(Icons.Filled.Delete, null, tint = PixelColors.white, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        SettingsSection(icon = Icons.Filled.Timer, title = "小工具設定") {
            Text("語錄刷新頻率", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.gray))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(5, 10, 30, 60, 180, 300, 480, 1440).forEach { minutes ->
                    PixelButton(
                        onClick = { onUpdateWidgetInterval(minutes) },
                        backgroundColor = if (settings.widgetRefreshInterval == minutes) PixelColors.yellow else PixelColors.gray,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(intervalLabel(minutes), style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("注意：Android 小工具實際刷新可能受系統省電策略影響。", style = pixelTextStyle(12.sp, color = PixelColors.gray))
        }
        SettingsSection(icon = Icons.Filled.RestartAlt, title = "重置") {
            PixelButton(onClick = { showReset = true }, backgroundColor = PixelColors.red, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, null, tint = PixelColors.white)
                Spacer(Modifier.size(8.dp))
                Text("全部重來", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.white))
            }
        }
        Text(
            "離職集點卡 Android 1.0\nMade By Neil 尼歐",
            style = pixelTextStyle(12.sp, color = PixelColors.gray),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun CoinSection(coins: Int) {
    SettingsSection(icon = Icons.Filled.AttachMoney, title = "離職金幣") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🪙", style = pixelTextStyle(34.sp))
            Spacer(Modifier.size(10.dp))
            Column {
                Text(coins.toString(), style = pixelTextStyle(28.sp, FontWeight.Bold, PixelColors.black))
                Text("每日蓋章 +1，一天上限一枚", style = pixelTextStyle(12.sp, color = PixelColors.gray))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PixelColors.yellow, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(8.dp))
            Text(title, style = pixelTextStyle(18.sp, FontWeight.Bold, PixelColors.white))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pixelCard()
                .padding(16.dp),
            content = content,
        )
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.gray))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = pixelTextStyle(16.sp, FontWeight.Medium, PixelColors.black),
            cursorBrush = SolidColor(PixelColors.black),
            modifier = Modifier
                .fillMaxWidth()
                .background(PixelColors.white)
                .border(3.dp, PixelColors.black, RectangleShape)
                .padding(12.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, style = pixelTextStyle(15.sp, color = PixelColors.gray))
                inner()
            },
        )
    }
}

@Composable
private fun ThemeButton(
    theme: CardTheme,
    selected: Boolean,
    unlocked: Boolean,
    coins: Int,
    onSelect: () -> Unit,
    onUnlock: () -> Unit,
) {
    val canUnlock = coins >= theme.coinCost
    Column(
        modifier = Modifier
            .background(if (selected) PixelColors.blue.copy(alpha = 0.18f) else PixelColors.white)
            .border(2.dp, if (selected) PixelColors.blue else PixelColors.black, RectangleShape)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(theme.backgroundColor)
                .border(2.dp, theme.borderColor, RectangleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(theme.stampIcon, null, tint = theme.accentColor, modifier = Modifier.size(30.dp))
            if (!unlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PixelColors.black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Lock, null, tint = PixelColors.white, modifier = Modifier.size(18.dp))
                        Text("${theme.coinCost}🪙", style = pixelTextStyle(10.sp, FontWeight.Bold, PixelColors.white))
                    }
                }
            }
        }
        Text(theme.title, style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.black))
        PixelButton(
            onClick = if (unlocked) onSelect else onUnlock,
            enabled = unlocked || canUnlock,
            backgroundColor = when {
                unlocked && selected -> PixelColors.green
                unlocked -> PixelColors.blue
                canUnlock -> PixelColors.yellow
                else -> PixelColors.gray
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Text(
                if (unlocked) if (selected) "使用中" else "套用" else "解鎖",
                style = pixelTextStyle(12.sp, FontWeight.Bold, if (unlocked) PixelColors.white else PixelColors.black),
            )
        }
    }
}

private fun intervalLabel(minutes: Int): String = when (minutes) {
    60 -> "1 小時"
    180 -> "3 小時"
    300 -> "5 小時"
    480 -> "8 小時"
    1440 -> "每天"
    else -> "$minutes 分鐘"
}

@Preview(name = "設定", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsContentPreview() {
    SettingsContent(
        settings = AppSettings(
            companyName = "血汗科技",
            cardStyle = CardTheme.SAKURA.id,
            maxStamps = 20,
            widgetRefreshInterval = 60,
            coinCount = 22,
            unlockedStyles = listOf(CardTheme.RPG.id, CardTheme.CYBERPUNK.id, CardTheme.VACATION.id, CardTheme.SAKURA.id),
            commonReasons = listOf("無效會議", "需求一直改", "加班沒錢", "老闆廢話太多"),
        ),
        onUpdateCompany = {},
        onSelectTheme = {},
        onUnlockTheme = {},
        onAddReason = {},
        onDeleteReason = {},
        onUpdateWidgetInterval = {},
        onResetAll = {},
    )
}
