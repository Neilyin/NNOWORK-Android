package com.neilyin.nnowork.feature.plan

import com.neilyin.nnowork.ui.theme.contrastRatio
import com.neilyin.nnowork.ui.theme.fieldSurface
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.neilyin.nnowork.ui.theme.nnoworkSwitchColors
import com.neilyin.nnowork.ui.theme.NnoworkField
import com.neilyin.nnowork.ui.theme.contentColorFor
import com.neilyin.nnowork.ui.theme.NnoworkPageTitle
import com.neilyin.nnowork.ui.theme.NnoworkDialogDefaults
import android.app.DatePickerDialog
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import com.neilyin.nnowork.ui.adaptive.ReadableWidthContainer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.plan.HourlyTrackingMode
import com.neilyin.nnowork.data.plan.ManualSalaryTimer
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.plan.SalaryCalculator
import com.neilyin.nnowork.data.plan.SalaryCalculator.isConfigured
import com.neilyin.nnowork.data.plan.Shift
import com.neilyin.nnowork.data.plan.TodoItem
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.notify.ReminderScheduler
import com.neilyin.nnowork.ui.theme.NnoworkTextField
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelBadge
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.neilyin.nnowork.data.settings.ReviewMoment
import com.neilyin.nnowork.feature.review.ReviewPrompter
import com.neilyin.nnowork.util.findActivity
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PlanViewModel(
    private val repository: PlanRepository = ServiceLocator.planRepository,
) : ViewModel() {
    val plan: StateFlow<ResignationPlan> = repository.plan.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ResignationPlan(),
    )

    // 自由預備金 / 履歷
    fun setTarget(value: Double) = update { it.copy(targetAmount = value.coerceAtLeast(0.0)) }
    fun setSaved(value: Double) = update { it.copy(currentSaved = value.coerceAtLeast(0.0)) }
    fun addFunds(value: Double) = update { it.copy(currentSaved = (it.currentSaved + value).coerceAtLeast(0.0)) }
    fun toggleResume() = update { it.copy(hasResume = !it.hasResume) }

    // 薪資
    fun setSalaryPayMode(mode: Int) = update { plan ->
        val normalized = mode.coerceIn(0, 1)
        val timerSafePlan = if (normalized == 0) {
            // 手動計時只屬於時薪制；切回月薪時停止並保留本次結果，不能在背景偷跑。
            ManualSalaryTimer.stop(plan, System.currentTimeMillis())
        } else {
            plan
        }
        timerSafePlan.copy(salaryPayMode = normalized)
    }
    fun setHourlyTrackingMode(mode: Int) = update { plan ->
        val normalized = HourlyTrackingMode.normalize(mode)
        val timerSafePlan = if (normalized == HourlyTrackingMode.SCHEDULE) {
            // 離開手動計時時先結束當前片段，避免計時器在時段制背後繼續跑。
            ManualSalaryTimer.stop(plan, System.currentTimeMillis())
        } else {
            plan
        }
        timerSafePlan.copy(hourlyTrackingMode = normalized)
    }
    fun setMonthlySalary(value: Double) {
        update { it.copy(monthlySalary = value.coerceAtLeast(0.0)) }
        com.neilyin.nnowork.analytics.Analytics.log("salary_settings_saved", "mode" to "monthly")
    }
    fun setHourlyRate(value: Double) {
        update { it.copy(hourlyRate = value.coerceAtLeast(0.0)) }
        com.neilyin.nnowork.analytics.Analytics.log("salary_settings_saved", "mode" to "hourly")
    }
    fun startManualSalaryTimer() = update {
        ManualSalaryTimer.start(it, System.currentTimeMillis())
    }
    fun pauseManualSalaryTimer() = update {
        ManualSalaryTimer.pause(it, System.currentTimeMillis())
    }
    fun stopManualSalaryTimer() = update {
        ManualSalaryTimer.stop(it, System.currentTimeMillis())
    }
    fun setWorkStart(minutes: Int) = update { it.copy(workStartMinutes = minutes.coerceIn(0, 1_439)) }
    fun setWorkEnd(minutes: Int) = update { it.copy(workEndMinutes = minutes.coerceIn(0, 1_439)) }
    fun setLunchUnpaid(enabled: Boolean) = update { it.copy(lunchUnpaid = enabled) }
    fun setLunchMinutes(minutes: Int) = update { it.copy(lunchMinutes = minutes.coerceIn(0, 480)) }
    fun cycleCurrency() = update {
        val list = listOf("NT$", "$", "¥", "CN¥")
        val idx = list.indexOf(it.currencySymbol)
        it.copy(currencySymbol = list[(idx + 1).mod(list.size)])
    }
    fun togglePayday(day: Int) = update {
        val next = if (it.paydayDays.contains(day)) it.paydayDays - day else it.paydayDays + day
        it.copy(paydayDays = next)
    }

    fun setScheduleMode(mode: Int) = update { plan ->
        val now = System.currentTimeMillis()
        val next = plan.copy(monthlyScheduleMode = mode)
        when {
            mode == 0 && next.customRestDays.isEmpty() ->
                next.copy(customRestDays = SalaryCalculator.defaultRedRestDays(now))
            mode == 1 && !next.shiftUseTemplates && next.customRestDays.isEmpty() ->
                next.copy(customRestDays = SalaryCalculator.defaultWeekendRestDays(now))
            else -> next
        }
    }

    fun setShiftUseTemplates(use: Boolean) = update { plan ->
        val next = plan.copy(shiftUseTemplates = use)
        if (!use && next.customRestDays.isEmpty()) {
            next.copy(customRestDays = SalaryCalculator.defaultWeekendRestDays(System.currentTimeMillis()))
        } else next
    }

    fun resetRestDays() = update {
        it.copy(customRestDays = SalaryCalculator.defaultRedRestDays(System.currentTimeMillis()))
    }

    fun toggleRestDay(key: String) = update {
        val next = if (it.customRestDays.contains(key)) it.customRestDays - key else it.customRestDays + key
        it.copy(customRestDays = next)
    }

    fun cycleDayShift(key: String) = update { plan ->
        if (plan.shifts.isEmpty()) return@update plan
        val current = plan.dayShifts[key]
        val idx = plan.shifts.indexOfFirst { it.id == current }
        val nextId = when {
            current == null -> plan.shifts.first().id
            idx == plan.shifts.lastIndex -> null
            else -> plan.shifts[idx + 1].id
        }
        val map = plan.dayShifts.toMutableMap()
        if (nextId == null) map.remove(key) else map[key] = nextId
        plan.copy(dayShifts = map)
    }

    fun addShift() = update {
        val n = it.shifts.size + 1
        it.copy(shifts = it.shifts + Shift(name = "班別 $n"))
    }
    fun updateShift(shift: Shift) = update {
        it.copy(shifts = it.shifts.map { s -> if (s.id == shift.id) shift else s })
    }
    fun deleteShift(id: String) = update { plan ->
        plan.copy(
            shifts = plan.shifts.filterNot { it.id == id },
            dayShifts = plan.dayShifts.filterValues { it != id },
        )
    }

    // 待辦（含提醒）
    fun addTodo(title: String, reminderAt: Long?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val item = TodoItem(title = trimmed, reminderAt = reminderAt)
        update { it.copy(todoItems = it.todoItems + item) }
        reminderAt?.let { ReminderScheduler.schedule(ServiceLocator.applicationContext, item.id, trimmed, it) }
    }
    fun completeTodo(item: TodoItem) {
        ReminderScheduler.cancel(ServiceLocator.applicationContext, item.id)
        update {
            it.copy(
                todoItems = it.todoItems.filterNot { todo -> todo.id == item.id },
                completedTodoItems = it.completedTodoItems + item,
            )
        }
    }
    fun restoreTodo(item: TodoItem) = update {
        it.copy(
            completedTodoItems = it.completedTodoItems.filterNot { todo -> todo.id == item.id },
            todoItems = it.todoItems + item,
        )
    }
    fun deleteTodo(item: TodoItem) {
        ReminderScheduler.cancel(ServiceLocator.applicationContext, item.id)
        update {
            it.copy(
                todoItems = it.todoItems.filterNot { todo -> todo.id == item.id },
                completedTodoItems = it.completedTodoItems.filterNot { todo -> todo.id == item.id },
            )
        }
    }

    private fun update(transform: (ResignationPlan) -> ResignationPlan) {
        viewModelScope.launch {
            repository.update(transform)
            com.neilyin.nnowork.analytics.Analytics.log("resignation_plan_updated")
        }
    }
}

@Composable
fun PlanScreen(viewModel: PlanViewModel = viewModel()) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val progress = if (plan.targetAmount <= 0.0) 0f else
        (plan.currentSaved / plan.targetAmount).coerceIn(0.0, 1.0).toFloat()

    // 大螢幕：內容限寬置中（iOS duoReadableWidth）；手機寬度時左右內距＝page，與原本相同。
    ReadableWidthContainer(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        minPadding = NnoworkSpacing.page,
    ) { sidePadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = sidePadding),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.roomy),
        ) {
            item {
                NnoworkPageTitle("離職計畫", modifier = Modifier.padding(top = NnoworkSpacing.md))
            }
            item { HeaderCard() }
            item { SalaryTimerCard(plan = plan, viewModel = viewModel) }
            item { FundCard(plan, progress, viewModel::setTarget, viewModel::setSaved, viewModel::addFunds) }
            item { ResumeCard(plan.hasResume, viewModel::toggleResume) }
            item { TodoComposer(onAdd = viewModel::addTodo) }
            items(plan.todoItems, key = { it.id }) { item ->
                TodoRow(item, false, { viewModel.completeTodo(item) }, {}, { viewModel.deleteTodo(item) })
            }
            if (plan.completedTodoItems.isNotEmpty()) {
                item { Text("已完成", style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.secondaryText)) }
                items(plan.completedTodoItems, key = { it.id }) { item ->
                    TodoRow(item, true, {}, { viewModel.restoreTodo(item) }, { viewModel.deleteTodo(item) })
                }
            }
            if (plan.todoItems.isEmpty() && plan.completedTodoItems.isEmpty()) {
                item {
                    Text(
                        "還沒有待辦事項，先寫下一件能讓自由更靠近的小事。",
                        style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.secondaryText),
                        modifier = Modifier.fillMaxWidth().padding(vertical = NnoworkSpacing.lg),
                    )
                }
            }
            item { Spacer(Modifier.height(NnoworkSpacing.sm)) }
        }
    }
}

/** [Shift] 的 rememberSaveable saver。 */
private val ShiftSaver = listSaver<Shift?, Any>(
    save = { shift ->
        if (shift == null) emptyList()
        else listOf(shift.id, shift.name, shift.startMinutes, shift.endMinutes, shift.colorHex, shift.icon)
    },
    restore = { v ->
        if (v.size < 6) null
        else Shift(
            id = v[0] as String,
            name = v[1] as String,
            startMinutes = v[2] as Int,
            endMinutes = v[3] as Int,
            colorHex = v[4] as String,
            icon = v[5] as String,
        )
    },
)

// ───────────────────────── 薪水計時器 §4 ─────────────────────────

@Composable
private fun SalaryTimerCard(plan: ResignationPlan, viewModel: PlanViewModel) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var expanded by rememberSaveable { mutableStateOf(!plan.isConfigured) }
    val snapshot = remember(plan, now) { SalaryCalculator.snapshot(plan, now) }
    val usesManualTimer = plan.salaryPayMode == 1 &&
        HourlyTrackingMode.normalize(plan.hourlyTrackingMode) == HourlyTrackingMode.MANUAL

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.roomy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.cozy),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(Icons.Filled.AttachMoney, "薪水計時器", NnoworkTheme.colors.highlight)
            Spacer(Modifier.width(NnoworkSpacing.tight))
            Box(Modifier.clip(RoundedCornerShape(NnoworkRadius.small)).background(NnoworkTheme.colors.accentSoft).padding(horizontal = NnoworkSpacing.tight, vertical = 2.dp)) {
                Text("BETA", style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.accentForeground))
            }
            Spacer(Modifier.weight(1f))
            PixelButton(
                onClick = { expanded = !expanded },
                backgroundColor = NnoworkTheme.colors.disabledBG,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.xs),
            ) {
                Text(if (expanded) "收合" else "設定", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        }

        if (expanded) {
            SalarySettings(plan, viewModel, now)
        } else if (usesManualTimer) {
            ManualHourlyTimerBoard(
                plan = plan,
                now = now,
                onStart = viewModel::startManualSalaryTimer,
                onPause = viewModel::pauseManualSalaryTimer,
                onStop = viewModel::stopManualSalaryTimer,
            )
        } else if (snapshot.configured) {
            SalaryBoard(snapshot, plan)
        } else {
            Text("設定薪資和上班時間後，就能即時看今天賺了多少。", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
        }
    }
}

@Composable
private fun ManualHourlyTimerBoard(
    plan: ResignationPlan,
    now: Long,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    val elapsed = ManualSalaryTimer.elapsedMillis(plan, now)
    val earnings = ManualSalaryTimer.earned(plan, now)
    val (statusLabel, statusColor) = when (plan.manualTimerStatus) {
        ManualSalaryTimer.RUNNING -> "計時中" to NnoworkTheme.colors.success
        ManualSalaryTimer.PAUSED -> "已暫停" to NnoworkTheme.colors.disabledBG
        else -> if (elapsed > 0L) "已停止" to NnoworkTheme.colors.disabledBG else "尚未開始" to NnoworkTheme.colors.disabledBG
    }

    // 計時卡：頁底色的內嵌面＋accent＋一個狀態色（計時中＝success），不再用彩色底配多種原色字。
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pixelCard(backgroundColor = NnoworkTheme.colors.appBG, borderColor = NnoworkTheme.colors.border, elevation = 0.dp)
            .padding(NnoworkSpacing.cozy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timer, contentDescription = null, tint = NnoworkTheme.colors.accentForeground, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text("手動計時", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText), modifier = Modifier.weight(1f))
            PixelBadge(backgroundColor = statusColor) {
                Text(statusLabel, style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        }

        Text(
            SalaryCalculator.formatCountdown(elapsed),
            style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            "本次累積 ${SalaryCalculator.formatMoney(plan, earnings)}",
            style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.secondaryText),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            PixelButton(
                onClick = onStart,
                enabled = plan.manualTimerStatus != ManualSalaryTimer.RUNNING,
                backgroundColor = NnoworkTheme.colors.success,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.xs, vertical = NnoworkSpacing.snug),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(NnoworkSpacing.xxs))
                Text(
                    when {
                        plan.manualTimerStatus == ManualSalaryTimer.PAUSED -> "繼續"
                        elapsed > 0L -> "重新開始"
                        else -> "開始"
                    },
                    style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                    maxLines = 1,
                )
            }
            PixelButton(
                onClick = onPause,
                enabled = plan.manualTimerStatus == ManualSalaryTimer.RUNNING,
                backgroundColor = NnoworkTheme.colors.disabledBG,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.xs, vertical = NnoworkSpacing.snug),
            ) {
                Icon(Icons.Filled.Pause, contentDescription = null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(NnoworkSpacing.xxs))
                Text("暫停", style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.primaryText), maxLines = 1)
            }
            PixelButton(
                onClick = onStop,
                enabled = plan.manualTimerStatus == ManualSalaryTimer.RUNNING ||
                    plan.manualTimerStatus == ManualSalaryTimer.PAUSED,
                backgroundColor = NnoworkTheme.colors.disabledBG,
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.xs, vertical = NnoworkSpacing.snug),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, tint = NnoworkTheme.colors.danger, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(NnoworkSpacing.xxs))
                Text("停止", style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.onElevated), maxLines = 1)
            }
        }
        Text(
            "停止會保留本次結果；再次開始會從 00:00:00 重新計算。",
            style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
        )
    }
}

@Composable
private fun SalaryBoard(snapshot: SalaryCalculator.Snapshot, plan: ResignationPlan) {
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
            SalaryStat("今日已賺", SalaryCalculator.formatMoney(plan, snapshot.todayEarned), NnoworkTheme.colors.success, Modifier.weight(1f))
            SalaryStat("本期累積", SalaryCalculator.formatMoney(plan, snapshot.cycleEarned), NnoworkTheme.colors.info, Modifier.weight(1f))
        }
        LinearProgressIndicator(
            progress = { snapshot.progress },
            modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(NnoworkRadius.small)),
            color = NnoworkTheme.colors.accent,
            trackColor = NnoworkTheme.colors.accentSoft,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, tint = NnoworkTheme.colors.warning, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text(snapshot.statusText, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText), modifier = Modifier.weight(1f))
            if (snapshot.phase == SalaryCalculator.Phase.WORKING) {
                Text("剩 ${SalaryCalculator.formatCountdown(snapshot.countdownMillis)}", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.warning))
            }
        }
    }
}

@Composable
private fun SalaryStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(NnoworkRadius.field))
            .background(NnoworkTheme.colors.accentSoft)
            .padding(NnoworkSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
    ) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
        Text(value, style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, color.takeIf { contrastRatio(it, NnoworkTheme.colors.accentSoft) >= 4.5f } ?: NnoworkTheme.colors.primaryText, monospace = true))
    }
}

@Composable
private fun SalarySettings(plan: ResignationPlan, viewModel: PlanViewModel, now: Long) {
    var showAllPaydays by rememberSaveable { mutableStateOf(false) }
    // 編輯中的班別（可能是尚未存檔的新班別）整筆存進 saved state，摺疊／旋轉後對話框仍開著。
    var editingShift by rememberSaveable(stateSaver = ShiftSaver) { mutableStateOf<Shift?>(null) }

    editingShift?.let { shift ->
        ShiftEditorDialog(
            shift = shift,
            onDismiss = { editingShift = null },
            onSave = { viewModel.updateShift(it); editingShift = null },
            onDelete = { viewModel.deleteShift(it.id); editingShift = null },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.cozy)) {
        // 計薪方式
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            listOf(0 to "月薪制", 1 to "時薪制").forEach { (mode, label) ->
                PixelButton(
                    onClick = { viewModel.setSalaryPayMode(mode) },
                    backgroundColor = if (plan.salaryPayMode == mode) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG,
                    modifier = Modifier.weight(1f),
                ) { Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText)) }
            }
        }

        val hourlyMode = HourlyTrackingMode.normalize(plan.hourlyTrackingMode)
        if (plan.salaryPayMode == 1) {
            Text("時薪計時方式", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                listOf(
                    HourlyTrackingMode.SCHEDULE to "時段制",
                    HourlyTrackingMode.MANUAL to "手動計時",
                ).forEach { (mode, label) ->
                    PixelButton(
                        onClick = { viewModel.setHourlyTrackingMode(mode) },
                        backgroundColor = if (hourlyMode == mode) NnoworkTheme.colors.buttonBG else NnoworkTheme.colors.disabledBG,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            if (mode == HourlyTrackingMode.SCHEDULE) Icons.Filled.Schedule else Icons.Filled.Timer,
                            contentDescription = null,
                            tint = NnoworkTheme.colors.primaryText,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(NnoworkSpacing.tight))
                        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText), maxLines = 1)
                    }
                }
            }
            Text(
                if (hourlyMode == HourlyTrackingMode.SCHEDULE) {
                    "依上下班時間與排休自動計算。"
                } else {
                    "由你自己按開始、暫停與停止，只累計實際計時。"
                },
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
            )
        }

        // iOS NiuMaSettingsSheet：第一次把薪水計時器設定好（金額 > 0）是正向時刻。
        val activity = LocalContext.current.findActivity()
        val onSalarySaved: (Double) -> Unit = { value ->
            if (value > 0) activity?.let { ReviewPrompter.onPositiveMoment(it, ReviewMoment.TIMER_SETUP, 900) }
        }
        if (plan.salaryPayMode == 0) {
            MoneyField("月薪", plan.monthlySalary, plan.currencySymbol, viewModel::cycleCurrency) { viewModel.setMonthlySalary(it); onSalarySaved(it) }
        } else {
            MoneyField("時薪", plan.hourlyRate, plan.currencySymbol, viewModel::cycleCurrency) { viewModel.setHourlyRate(it); onSalarySaved(it) }
        }

        val usesScheduledHours = plan.salaryPayMode == 0 || hourlyMode == HourlyTrackingMode.SCHEDULE
        if (!usesScheduledHours) {
            ManualHourlyTimerBoard(
                plan = plan,
                now = now,
                onStart = viewModel::startManualSalaryTimer,
                onPause = viewModel::pauseManualSalaryTimer,
                onStop = viewModel::stopManualSalaryTimer,
            )
            return@Column
        }

        // 上下班時間
        Text("上下班時間（end ≤ start 視為夜班跨日）", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            TimeField("上班", plan.workStartMinutes, viewModel::setWorkStart, Modifier.weight(1f))
            TimeField("下班", plan.workEndMinutes, viewModel::setWorkEnd, Modifier.weight(1f))
        }

        // 午休
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("午休不計薪", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                Text("開啟後會從每日工時扣除午休", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
            }
            Switch(plan.lunchUnpaid, viewModel::setLunchUnpaid, colors = nnoworkSwitchColors())
        }
        if (plan.lunchUnpaid) {
            ChipRow("午休分鐘（15 為單位）", listOf(15, 30, 45, 60, 90, 120), plan.lunchMinutes) { viewModel.setLunchMinutes(it) }
        }

        // 發薪日（多選）
        Text("發薪日（可多選 1–30）", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
        MultiChipRow(listOf(5, 10, 15, 25), plan.paydayDays) { viewModel.togglePayday(it) }
        PixelButton(
            onClick = { showAllPaydays = !showAllPaydays },
            backgroundColor = NnoworkTheme.colors.disabledBG,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.xs),
        ) { Text(if (showAllPaydays) "收合" else "⋯ 1–30", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText)) }
        if (showAllPaydays) {
            DayGrid(selected = plan.paydayDays) { viewModel.togglePayday(it) }
        }

        // 班制
        Text("排休方式", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
            listOf(0 to "見紅休", 1 to "排班制").forEach { (mode, label) ->
                PixelButton(
                    onClick = { viewModel.setScheduleMode(mode) },
                    backgroundColor = if (plan.monthlyScheduleMode == mode) NnoworkTheme.colors.success else NnoworkTheme.colors.disabledBG,
                    modifier = Modifier.weight(1f),
                ) { Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText)) }
            }
        }

        if (plan.monthlyScheduleMode == 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                listOf(false to "固定工時", true to "多種班別").forEach { (use, label) ->
                    PixelButton(
                        onClick = { viewModel.setShiftUseTemplates(use) },
                        backgroundColor = if (plan.shiftUseTemplates == use) NnoworkTheme.colors.buttonBG else NnoworkTheme.colors.disabledBG,
                        modifier = Modifier.weight(1f),
                    ) { Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.onElevated)) }
                }
            }
            if (plan.shiftUseTemplates) {
                ShiftTemplateSection(
                    shifts = plan.shifts,
                    onAdd = viewModel::addShift,
                    onEdit = { editingShift = it },
                )
            }
        } else {
            PixelButton(
                onClick = viewModel::resetRestDays,
                backgroundColor = NnoworkTheme.colors.disabledBG,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.xs),
            ) { Text("重設假日（週末+國定假日）", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText)) }
        }

        // 月曆
        ScheduleCalendar(plan = plan, now = now, viewModel = viewModel)
    }
}

@Composable
private fun ShiftTemplateSection(shifts: List<Shift>, onAdd: () -> Unit, onEdit: (Shift) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("班別模板", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText), modifier = Modifier.weight(1f))
            PixelButton(onClick = onAdd, backgroundColor = NnoworkTheme.colors.success, contentPadding = androidx.compose.foundation.layout.PaddingValues(NnoworkSpacing.xs)) {
                Icon(Icons.Filled.Add, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(16.dp))
            }
        }
        if (shifts.isEmpty()) {
            Text("先新增班別，再到下方月曆點格子指派。", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
        }
        shifts.forEach { shift ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(NnoworkRadius.field))
                    .background(NnoworkTheme.colors.accentSoft)
                    .clickable { onEdit(shift) }.padding(NnoworkSpacing.snug),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(NnoworkRadius.small)).background(parseColor(shift.colorHex)), contentAlignment = Alignment.Center) {
                    Icon(shiftIcon(shift.icon), null, tint = contentColorFor(parseColor(shift.colorHex)), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.size(NnoworkSpacing.snug))
                Text(shift.name.ifBlank { "未命名班別" }, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText), modifier = Modifier.weight(1f))
                Text("${minToHHmm(shift.startMinutes)}–${minToHHmm(shift.endMinutes)}", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
            }
        }
    }
}

@Composable
private fun ScheduleCalendar(plan: ResignationPlan, now: Long, viewModel: PlanViewModel) {
    val multiShift = plan.monthlyScheduleMode == 1 && plan.shiftUseTemplates
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CalendarMonth, null, tint = NnoworkTheme.colors.info, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(NnoworkSpacing.tight))
            Text(
                if (multiShift) "排班月曆（點格子循環班別）" else "排休月曆（點格子切換工作/休假）",
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText),
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                null, tint = NnoworkTheme.colors.secondaryText, modifier = Modifier.size(20.dp),
            )
        }
        if (expanded) {
            monthsFrom(now).forEach { monthStart ->
                MonthGrid(monthStart, plan, multiShift) { key ->
                    if (multiShift) viewModel.cycleDayShift(key) else viewModel.toggleRestDay(key)
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(monthStart: Long, plan: ResignationPlan, multiShift: Boolean, onTapDay: (String) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
    val leading = firstWeekday - 1

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(NnoworkRadius.field))
            .background(NnoworkTheme.colors.surface)
            .border(NnoworkLine.hairline, NnoworkTheme.colors.divider, RoundedCornerShape(NnoworkRadius.field))
            .padding(NnoworkSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs),
    ) {
        Text("${year}年 ${month}月", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(it, style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.secondaryText), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val cells = leading + daysInMonth
        val rows = (cells + 6) / 7
        var day = 1
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (c in 0 until 7) {
                    val index = r * 7 + c
                    if (index < leading || day > daysInMonth) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dayMillis = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }.timeInMillis
                        DayCell(dayMillis, plan, multiShift, onTapDay)
                        day++
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DayCell(
    dayMillis: Long,
    plan: ResignationPlan,
    multiShift: Boolean,
    onTapDay: (String) -> Unit,
) {
    val key = SalaryCalculator.dateKey(dayMillis)
    val dayNum = Calendar.getInstance().apply { timeInMillis = dayMillis }.get(Calendar.DAY_OF_MONTH)
    val holiday = SalaryCalculator.isHoliday(dayMillis)
    val rest = SalaryCalculator.isRestDay(plan, dayMillis)
    val shift = if (multiShift) plan.shifts.firstOrNull { it.id == plan.dayShifts[key] } else null

    val bg = when {
        shift != null -> parseColor(shift.colorHex)
        rest -> NnoworkTheme.colors.accentSoft
        else -> NnoworkTheme.colors.elevatedSurface
    }
    Box(
        modifier = Modifier.weight(1f).aspectRatio(1f)
            .clip(RoundedCornerShape(NnoworkRadius.small))
            .background(bg).border(NnoworkLine.hairline, NnoworkTheme.colors.divider, RoundedCornerShape(NnoworkRadius.small))
            .clickable { onTapDay(key) },
        contentAlignment = Alignment.Center,
    ) {
        if (shift != null) {
            Icon(shiftIcon(shift.icon), null, tint = contentColorFor(bg), modifier = Modifier.size(13.dp))
        } else {
            Text(
                dayNum.toString(),
                style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, if (rest) NnoworkTheme.colors.primaryText else NnoworkTheme.colors.mutedText)
                    .copy(textDecoration = if (holiday) TextDecoration.Underline else null),
            )
        }
    }
}

@Composable
private fun ShiftEditorDialog(shift: Shift, onDismiss: () -> Unit, onSave: (Shift) -> Unit, onDelete: (Shift) -> Unit) {
    var name by rememberSaveable(shift.id) { mutableStateOf(shift.name) }
    var start by rememberSaveable(shift.id) { mutableStateOf(shift.startMinutes) }
    var end by rememberSaveable(shift.id) { mutableStateOf(shift.endMinutes) }
    var color by rememberSaveable(shift.id) { mutableStateOf(shift.colorHex) }
    var icon by rememberSaveable(shift.id) { mutableStateOf(shift.icon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NnoworkDialogDefaults.containerColor,
        shape = NnoworkDialogDefaults.shape,
        title = { Text("編輯班別", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
                NnoworkTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "班別名稱",
                    placeholder = "例如：早班",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                    TimeField("開始", start, { start = it }, Modifier.weight(1f))
                    TimeField("結束", end, { end = it }, Modifier.weight(1f))
                }
                Text("圖示", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
                Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                    shiftIconKeys.forEach { k ->
                        Box(
                            Modifier.size(38.dp).clip(RoundedCornerShape(NnoworkRadius.small))
                                .background(if (icon == k) NnoworkTheme.colors.accent else NnoworkTheme.colors.disabledBG)
                                .clickable { icon = k },
                            contentAlignment = Alignment.Center,
                        ) { Icon(shiftIcon(k), null, tint = if (icon == k) NnoworkTheme.colors.onAccent else NnoworkTheme.colors.primaryText, modifier = Modifier.size(20.dp)) }
                    }
                }
                Text("底色", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
                Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                    shiftColors.forEach { hex ->
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(NnoworkRadius.small)).background(parseColor(hex))
                                .border(if (color == hex) NnoworkLine.emphasis else NnoworkLine.hairline, if (color == hex) NnoworkTheme.colors.primaryText else NnoworkTheme.colors.divider, RoundedCornerShape(NnoworkRadius.small)).clickable { color = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            PixelButton(onClick = { onSave(shift.copy(name = name.trim(), startMinutes = start, endMinutes = end, colorHex = color, icon = icon)) }, backgroundColor = NnoworkTheme.colors.success) {
                Text("儲存", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
        dismissButton = {
            PixelButton(onClick = { onDelete(shift) }, backgroundColor = NnoworkTheme.colors.danger) {
                Text("刪除", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
            }
        },
    )
}

@Composable
private fun MoneyField(label: String, value: Double, currency: String, onCurrency: () -> Unit, onSave: (Double) -> Unit) {
    var text by rememberSaveable(value) { mutableStateOf(if (value == 0.0) "" else value.toLong().toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelButton(onClick = onCurrency, backgroundColor = NnoworkTheme.colors.disabledBG, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.xs, vertical = NnoworkSpacing.tight)) {
                Text(currency, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
            Spacer(Modifier.size(NnoworkSpacing.xs))
            NnoworkField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() } },
                singleLine = true,
                textStyle = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(NnoworkSpacing.xs))
            PixelButton(onClick = { onSave(text.toDoubleOrNull() ?: 0.0) }, backgroundColor = NnoworkTheme.colors.buttonBG, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.xs)) {
                Text("儲存", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.onElevated))
            }
        }
    }
}

@Composable
private fun TimeField(label: String, minutes: Int, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
        Box(
            Modifier.fillMaxWidth().fieldSurface()
                .clickable {
                    TimePickerDialog(context, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, true).show()
                }.padding(NnoworkSpacing.sm),
            contentAlignment = Alignment.Center,
        ) { Text(minToHHmm(minutes), style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText)) }
    }
}

@Composable
private fun ChipRow(label: String, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
        Text(label, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
            options.forEach { item ->
                PixelButton(
                    onClick = { onSelect(item) },
                    backgroundColor = if (selected == item) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.xs),
                ) { Text(item.toString(), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText)) }
            }
        }
    }
}

@Composable
private fun MultiChipRow(options: List<Int>, selected: Set<Int>, onToggle: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
        options.forEach { item ->
            PixelButton(
                onClick = { onToggle(item) },
                backgroundColor = if (selected.contains(item)) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.xs),
            ) { Text("${item}號", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText)) }
        }
    }
}

@Composable
private fun DayGrid(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
        (1..30 step 6).forEach { startDay ->
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
                (startDay until startDay + 6).forEach { d ->
                    if (d <= 30) {
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .clip(RoundedCornerShape(NnoworkRadius.small))
                                .background(if (selected.contains(d)) NnoworkTheme.colors.accent else NnoworkTheme.colors.disabledBG)
                                .clickable { onToggle(d) },
                            contentAlignment = Alignment.Center,
                        ) { Text(d.toString(), style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, if (selected.contains(d)) NnoworkTheme.colors.onAccent else NnoworkTheme.colors.primaryText)) }
                    } else Box(Modifier.weight(1f))
                }
            }
        }
    }
}

// ───────────────────────── 其餘區塊 ─────────────────────────

@Composable
private fun HeaderCard() {
    Row(
        modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.roomy),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.TravelExplore, null, tint = NnoworkTheme.colors.accentForeground, modifier = Modifier.size(42.dp))
        Spacer(Modifier.size(NnoworkSpacing.cozy))
        Column {
            Text("自由需要準備", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            Text("好好規劃，邁向更好的未來", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
        }
    }
}

@Composable
private fun FundCard(plan: ResignationPlan, progress: Float, onTarget: (Double) -> Unit, onSaved: (Double) -> Unit, onQuickAdd: (Double) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.roomy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.cozy),
    ) {
        SectionTitle(Icons.Filled.Savings, "自由預備金", NnoworkTheme.colors.success)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("目標進度", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
            Spacer(Modifier.weight(1f))
            Text("${(progress * 100).toInt()}%", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.success))
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(NnoworkRadius.small)),
            color = NnoworkTheme.colors.accent,
            trackColor = NnoworkTheme.colors.accentSoft,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        MoneyField("目標金額", plan.targetAmount, plan.currencySymbol, {}, onTarget)
        MoneyField("目前已存", plan.currentSaved, plan.currencySymbol, {}, onSaved)
        if (plan.targetAmount > plan.currentSaved) {
            Text(
                "還差 ${SalaryCalculator.formatMoney(plan, plan.targetAmount - plan.currentSaved)} 就達標了",
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.warning),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
            listOf(5_000.0, 10_000.0).forEach { amount ->
                PixelButton(onClick = { onQuickAdd(amount) }, backgroundColor = NnoworkTheme.colors.disabledBG, modifier = Modifier.weight(1f)) {
                    Text("+ ${SalaryCalculator.formatMoney(plan, amount)}", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            }
        }
    }
}

@Composable
private fun ResumeCard(checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.roomy),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Description, null, tint = NnoworkTheme.colors.info, modifier = Modifier.size(28.dp))
        Spacer(Modifier.size(NnoworkSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text("履歷準備", style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            Text(
                if (checked) "太好了，隨時可以出發" else "記得更新履歷和作品集",
                style = pixelTextStyle(NnoworkTheme.type.small, color = if (checked) NnoworkTheme.colors.success else NnoworkTheme.colors.secondaryText),
            )
        }
        PixelButton(onClick = onToggle, backgroundColor = if (checked) NnoworkTheme.colors.success else NnoworkTheme.colors.disabledBG, contentPadding = androidx.compose.foundation.layout.PaddingValues(NnoworkSpacing.snug)) {
            Icon(if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = NnoworkTheme.colors.primaryText)
        }
    }
}

@Composable
private fun TodoComposer(onAdd: (String, Long?) -> Unit) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var reminderAt by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.roomy),
        verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
    ) {
        SectionTitle(Icons.Filled.CheckCircle, "待辦事項", NnoworkTheme.colors.warning)
        Row(verticalAlignment = Alignment.CenterVertically) {
            NnoworkTextField(
                value = text,
                onValueChange = { text = it },
                label = "新增待辦事項",
                placeholder = "例如：更新履歷",
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(NnoworkSpacing.snug))
            PixelButton(
                onClick = { onAdd(text, reminderAt); text = ""; reminderAt = null },
                backgroundColor = if (text.isBlank()) NnoworkTheme.colors.disabledBG else NnoworkTheme.colors.warning,
                enabled = text.isNotBlank(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(NnoworkSpacing.cozy),
            ) { Icon(Icons.Filled.Add, null, tint = NnoworkTheme.colors.primaryText) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelButton(
                onClick = { pickDateTime(context) { reminderAt = it } },
                backgroundColor = if (reminderAt != null) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.xs),
            ) {
                Icon(Icons.Filled.NotificationsActive, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(NnoworkSpacing.tight))
                Text(
                    reminderAt?.let { reminderFormat.format(Date(it)) } ?: "設定提醒",
                    style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
            }
            if (reminderAt != null) {
                Spacer(Modifier.size(NnoworkSpacing.xs))
                PixelButton(onClick = { reminderAt = null }, backgroundColor = NnoworkTheme.colors.danger, contentPadding = androidx.compose.foundation.layout.PaddingValues(NnoworkSpacing.xs)) {
                    Icon(Icons.Filled.Delete, null, tint = NnoworkTheme.colors.onElevated, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun TodoRow(item: TodoItem, completed: Boolean, onComplete: () -> Unit, onRestore: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().pixelCard(
            backgroundColor = if (completed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            else MaterialTheme.colorScheme.surfaceVariant,
        ).padding(NnoworkSpacing.cozy),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelButton(onClick = if (completed) onRestore else onComplete, backgroundColor = if (completed) NnoworkTheme.colors.success else NnoworkTheme.colors.warning, contentPadding = androidx.compose.foundation.layout.PaddingValues(NnoworkSpacing.xs)) {
            Icon(if (completed) Icons.Filled.Restore else Icons.Filled.RadioButtonUnchecked, null, tint = NnoworkTheme.colors.primaryText)
        }
        Spacer(Modifier.size(NnoworkSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Medium, if (completed) NnoworkTheme.colors.secondaryText else NnoworkTheme.colors.primaryText)
                    .copy(textDecoration = if (completed) TextDecoration.LineThrough else null),
            )
            item.reminderAt?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = NnoworkTheme.colors.warning, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.size(NnoworkSpacing.xxs))
                    Text(reminderFormat.format(Date(it)), style = pixelTextStyle(NnoworkTheme.type.caption, FontWeight.Bold, NnoworkTheme.colors.warning))
                }
            }
        }
        PixelButton(onClick = onDelete, backgroundColor = NnoworkTheme.colors.danger, contentPadding = androidx.compose.foundation.layout.PaddingValues(NnoworkSpacing.xs)) {
            Icon(Icons.Filled.Delete, null, tint = NnoworkTheme.colors.onElevated)
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color)
        Spacer(Modifier.size(NnoworkSpacing.xs))
        Text(title, style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.primaryText))
    }
}

// ───────────────────────── 工具 ─────────────────────────

private val reminderFormat = SimpleDateFormat("MM/dd HH:mm", Locale.TAIWAN)
private val shiftIconKeys = listOf("sunrise", "sun", "moon", "star")
private val shiftColors = listOf("#4A90D9", "#E67E22", "#27AE60", "#8E44AD", "#C0392B", "#16A085")

private fun shiftIcon(key: String): ImageVector = when (key) {
    "sunrise" -> Icons.Filled.WbTwilight
    "moon" -> Icons.Filled.Nightlight
    "star" -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.WbSunny
}

@Composable
private fun parseColor(hex: String): Color {
    val fallback = NnoworkTheme.colors.shiftFallback
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}

private fun minToHHmm(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

/** 從本月起到月曆上限（2026-12）的每月起始毫秒。 */
private fun monthsFrom(now: Long): List<Long> {
    val out = mutableListOf<Long>()
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    while (SalaryCalculator.dateKey(cal.timeInMillis) <= SalaryCalculator.CALENDAR_END_KEY) {
        out.add(cal.timeInMillis)
        cal.add(Calendar.MONTH, 1)
    }
    // 至少顯示本月
    if (out.isEmpty()) out.add(cal.timeInMillis)
    return out
}

private fun pickDateTime(context: android.content.Context, onPicked: (Long) -> Unit) {
    val now = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, y, mo, d ->
            TimePickerDialog(
                context,
                { _, h, mi ->
                    val cal = Calendar.getInstance().apply {
                        set(y, mo, d, h, mi, 0); set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(cal.timeInMillis)
                },
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true,
            ).show()
        },
        now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH),
    ).show()
}
